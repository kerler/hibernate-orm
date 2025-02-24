/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation.ColumnReferenceMapping;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSchemaMigrator implements SchemaMigrator {
	private static final Logger log = Logger.getLogger( IndividuallySchemaMigratorImpl.class );

	protected HibernateSchemaManagementTool tool;
	protected SchemaFilter schemaFilter;

	public AbstractSchemaMigrator(
			HibernateSchemaManagementTool tool,
			SchemaFilter schemaFilter) {
		this.tool = tool;
		if ( schemaFilter == null ) {
			this.schemaFilter = DefaultSchemaFilter.INSTANCE;
		}
		else {
			this.schemaFilter = schemaFilter;
		}
	}

	private UniqueConstraintSchemaUpdateStrategy uniqueConstraintStrategy;

	/**
	 * For testing...
	 */
	public void setUniqueConstraintStrategy(UniqueConstraintSchemaUpdateStrategy uniqueConstraintStrategy) {
		this.uniqueConstraintStrategy = uniqueConstraintStrategy;
	}

	@Override
	public void doMigration(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			TargetDescriptor targetDescriptor) {
		SqlStringGenerationContext sqlStringGenerationContext = SqlStringGenerationContextImpl.fromConfigurationMap(
				tool.getServiceRegistry().getService( JdbcEnvironment.class ),
				metadata.getDatabase(),
				options.getConfigurationValues()
		);
		if ( !targetDescriptor.getTargetTypes().isEmpty() ) {
			final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );
			final DdlTransactionIsolator ddlTransactionIsolator = tool.getDdlTransactionIsolator( jdbcContext );
			try {
				final DatabaseInformation databaseInformation = Helper.buildDatabaseInformation(
						tool.getServiceRegistry(),
						ddlTransactionIsolator,
						sqlStringGenerationContext,
						tool
				);

				final GenerationTarget[] targets = tool.buildGenerationTargets(
						targetDescriptor,
						ddlTransactionIsolator,
						options.getConfigurationValues()
				);

				try {
					for ( GenerationTarget target : targets ) {
						target.prepare();
					}

					try {
						performMigration( metadata, databaseInformation, options, contributableInclusionFilter, jdbcContext.getDialect(),
								sqlStringGenerationContext, targets );
					}
					finally {
						for ( GenerationTarget target : targets ) {
							try {
								target.release();
							}
							catch (Exception e) {
								log.debugf( "Problem releasing GenerationTarget [%s] : %s", target, e.getMessage() );
							}
						}
					}
				}
				finally {
					try {
						databaseInformation.cleanup();
					}
					catch (Exception e) {
						log.debug( "Problem releasing DatabaseInformation : " + e.getMessage() );
					}
				}
			}
			finally {
				ddlTransactionIsolator.release();
			}
		}
	}

	protected abstract NameSpaceTablesInformation performTablesMigration(
			Metadata metadata,
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect,
			Formatter formatter,
			Set<String> exportIdentifiers,
			boolean tryToCreateCatalogs,
			boolean tryToCreateSchemas,
			Set<Identifier> exportedCatalogs,
			Namespace namespace,
			SqlStringGenerationContext sqlStringGenerationContext,
			GenerationTarget[] targets);

	private void performMigration(
			Metadata metadata,
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect,
			SqlStringGenerationContext sqlStringGenerationContext,
			GenerationTarget... targets) {
		final boolean format = Helper.interpretFormattingEnabled( options.getConfigurationValues() );
		final Formatter formatter = format ? FormatStyle.DDL.getFormatter() : FormatStyle.NONE.getFormatter();

		final Set<String> exportIdentifiers = CollectionHelper.setOfSize( 50 );

		final Database database = metadata.getDatabase();

		// Drop all AuxiliaryDatabaseObjects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				applySqlStrings(
						true,
						dialect.getAuxiliaryDatabaseObjectExporter()
								.getSqlDropStrings( auxiliaryDatabaseObject, metadata, sqlStringGenerationContext ),
						formatter,
						options,
						targets
				);
			}
		}

		// Create before-table AuxiliaryDatabaseObjects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( !auxiliaryDatabaseObject.beforeTablesOnCreation() && auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				applySqlStrings(
						true,
						auxiliaryDatabaseObject.sqlCreateStrings( sqlStringGenerationContext ),
						formatter,
						options,
						targets
				);
			}
		}

		boolean tryToCreateCatalogs = false;
		boolean tryToCreateSchemas = false;
		if ( options.shouldManageNamespaces() ) {
			if ( dialect.canCreateSchema() ) {
				tryToCreateSchemas = true;
			}
			if ( dialect.canCreateCatalog() ) {
				tryToCreateCatalogs = true;
			}
		}
		final Map<Namespace, NameSpaceTablesInformation> tablesInformation = new HashMap<>();
		Set<Identifier> exportedCatalogs = new HashSet<>();
		for ( Namespace namespace : database.getNamespaces() ) {
			final NameSpaceTablesInformation nameSpaceTablesInformation = performTablesMigration(
					metadata,
					existingDatabase,
					options,
					contributableInclusionFilter,
					dialect,
					formatter,
					exportIdentifiers,
					tryToCreateCatalogs,
					tryToCreateSchemas,
					exportedCatalogs,
					namespace,
					sqlStringGenerationContext, targets
			);
			tablesInformation.put( namespace, nameSpaceTablesInformation );
			if ( options.getSchemaFilter().includeNamespace( namespace ) ) {
				for ( Sequence sequence : namespace.getSequences() ) {
					if ( ! contributableInclusionFilter.matches( sequence ) ) {
						continue;
					}
					checkExportIdentifier( sequence, exportIdentifiers );
					final SequenceInformation sequenceInformation = existingDatabase.getSequenceInformation( sequence.getName() );
					if ( sequenceInformation == null ) {
						applySqlStrings(
								false,
								dialect.getSequenceExporter().getSqlCreateStrings(
										sequence,
										metadata,
										sqlStringGenerationContext
								),
								formatter,
								options,
								targets
						);
					}
				}
			}
		}

		//NOTE : Foreign keys must be created *after* all tables of all namespaces for cross namespace fks. see HHH-10420
		for ( Namespace namespace : database.getNamespaces() ) {
			if ( options.getSchemaFilter().includeNamespace( namespace ) ) {
				final NameSpaceTablesInformation nameSpaceTablesInformation = tablesInformation.get( namespace );
				for ( Table table : namespace.getTables() ) {
					if ( ! options.getSchemaFilter().includeTable( table ) ) {
						continue;
					}
					if ( ! contributableInclusionFilter.matches( table ) ) {
						continue;
					}

					final TableInformation tableInformation = nameSpaceTablesInformation.getTableInformation( table );
					if ( tableInformation == null || tableInformation.isPhysicalTable() ) {
						applyForeignKeys( table, tableInformation, dialect, metadata, formatter, options,
									sqlStringGenerationContext, targets );
					}
				}
			}
		}

		// Create after-table AuxiliaryDatabaseObjects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.beforeTablesOnCreation() && auxiliaryDatabaseObject.appliesToDialect( dialect )) {
				applySqlStrings(
						true,
						auxiliaryDatabaseObject.sqlCreateStrings( sqlStringGenerationContext ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	protected void createTable(
			Table table,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlStringGenerationContext,
			GenerationTarget... targets) {
		applySqlStrings(
				false,
				dialect.getTableExporter().getSqlCreateStrings( table, metadata, sqlStringGenerationContext ),
				formatter,
				options,
				targets
		);
	}

	protected void migrateTable(
			Table table,
			TableInformation tableInformation,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlStringGenerationContext,
			GenerationTarget... targets) {
		final Database database = metadata.getDatabase();

		//noinspection unchecked
		applySqlStrings(
				false,
				table.sqlAlterStrings(
						dialect,
						metadata,
						tableInformation,
						sqlStringGenerationContext
				),
				formatter,
				options,
				targets
		);
	}

	protected void applyIndexes(
			Table table,
			TableInformation tableInformation,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlStringGenerationContext,
			GenerationTarget... targets) {
		final Exporter<Index> exporter = dialect.getIndexExporter();

		final Iterator<Index> indexItr = table.getIndexIterator();
		while ( indexItr.hasNext() ) {
			final Index index = indexItr.next();
			if ( !StringHelper.isEmpty( index.getName() ) ) {
				IndexInformation existingIndex = null;
				if ( tableInformation != null ) {
					existingIndex = findMatchingIndex( index, tableInformation );
				}
				if ( existingIndex == null ) {
					applySqlStrings(
							false,
							exporter.getSqlCreateStrings( index, metadata, sqlStringGenerationContext ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private IndexInformation findMatchingIndex(Index index, TableInformation tableInformation) {
		return tableInformation.getIndex( Identifier.toIdentifier( index.getName() ) );
	}

	protected void applyUniqueKeys(
			Table table,
			TableInformation tableInfo,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlStringGenerationContext,
			GenerationTarget... targets) {
		if ( uniqueConstraintStrategy == null ) {
			uniqueConstraintStrategy = determineUniqueConstraintSchemaUpdateStrategy( metadata );
		}

		if ( uniqueConstraintStrategy != UniqueConstraintSchemaUpdateStrategy.SKIP ) {
			final Exporter<Constraint> exporter = dialect.getUniqueKeyExporter();

			final Iterator ukItr = table.getUniqueKeyIterator();
			while ( ukItr.hasNext() ) {
				final UniqueKey uniqueKey = (UniqueKey) ukItr.next();
				// Skip if index already exists. Most of the time, this
				// won't work since most Dialects use Constraints. However,
				// keep it for the few that do use Indexes.
				IndexInformation indexInfo = null;
				if ( tableInfo != null && StringHelper.isNotEmpty( uniqueKey.getName() ) ) {
					indexInfo = tableInfo.getIndex( Identifier.toIdentifier( uniqueKey.getName() ) );
				}
				if ( indexInfo == null ) {
					if ( uniqueConstraintStrategy == UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY ) {
						applySqlStrings(
								true,
								exporter.getSqlDropStrings( uniqueKey, metadata, sqlStringGenerationContext ),
								formatter,
								options,
								targets
						);
					}

					applySqlStrings(
							true,
							exporter.getSqlCreateStrings( uniqueKey, metadata, sqlStringGenerationContext ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private UniqueConstraintSchemaUpdateStrategy determineUniqueConstraintSchemaUpdateStrategy(Metadata metadata) {
		final ConfigurationService cfgService = ((MetadataImplementor) metadata).getMetadataBuildingOptions()
				.getServiceRegistry()
				.getService( ConfigurationService.class );

		return UniqueConstraintSchemaUpdateStrategy.interpret(
				cfgService.getSetting( UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, StandardConverters.STRING )
		);
	}

	protected void applyForeignKeys(
			Table table,
			TableInformation tableInformation,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlStringGenerationContext,
			GenerationTarget... targets) {
		if ( dialect.hasAlterTable() ) {
			final Exporter<ForeignKey> exporter = dialect.getForeignKeyExporter();

			@SuppressWarnings("unchecked")
			final Iterator<ForeignKey> fkItr = table.getForeignKeyIterator();
			while ( fkItr.hasNext() ) {
				final ForeignKey foreignKey = fkItr.next();
				if ( foreignKey.isPhysicalConstraint() && foreignKey.isCreationEnabled() ) {
					boolean existingForeignKeyFound = false;
					if ( tableInformation != null ) {
						existingForeignKeyFound = checkForExistingForeignKey(
								foreignKey,
								tableInformation
						);
					}
					if ( !existingForeignKeyFound ) {
						// todo : shouldn't we just drop+recreate if FK exists?
						//		this follows the existing code from legacy SchemaUpdate which just skipped

						// in old SchemaUpdate code, this was the trigger to "create"
						applySqlStrings(
								false,
								exporter.getSqlCreateStrings( foreignKey, metadata, sqlStringGenerationContext ),
								formatter,
								options,
								targets
						);
					}
				}
			}
		}
	}

	/**
	 * Check if the ForeignKey already exists. First check based on definition and if that is not matched check if a key
	 * with the exact same name exists. Keys with the same name are presumed to be functional equal.
	 *
	 * @param foreignKey - ForeignKey, new key to be created
	 * @param tableInformation - TableInformation, information of existing keys
	 * @return boolean, true if key already exists
	 */
	private boolean checkForExistingForeignKey(ForeignKey foreignKey, TableInformation tableInformation) {
		if ( foreignKey.getName() == null || tableInformation == null ) {
			return false;
		}

		final String referencingColumn = foreignKey.getColumn( 0 ).getName();
		final String referencedTable = foreignKey.getReferencedTable().getName();

		/*
		 * Find existing keys based on referencing column and referencedTable. "referencedColumnName" is not checked
		 * because that always is the primary key of the "referencedTable".
		 */
		if (equivalentForeignKeyExistsInDatabase(tableInformation, referencingColumn, referencedTable)) {
			return true;
		}

		// And at the end just compare the name of the key. If a key with the same name exists we assume the function is
		// also the same...
		return tableInformation.getForeignKey( Identifier.toIdentifier( foreignKey.getName() ) ) != null;
	}

	boolean equivalentForeignKeyExistsInDatabase(TableInformation tableInformation, String referencingColumn, String referencedTable) {
		Predicate<ColumnReferenceMapping> mappingPredicate = m -> {
			String existingReferencingColumn = m.getReferencingColumnMetadata().getColumnIdentifier().getText();
			String existingReferencedTable = m.getReferencedColumnMetadata().getContainingTableInformation().getName().getTableName().getCanonicalName();
			return referencingColumn.equalsIgnoreCase( existingReferencingColumn ) && referencedTable.equalsIgnoreCase( existingReferencedTable );
		};
		Stream<ForeignKeyInformation> keyStream = StreamSupport.stream( tableInformation.getForeignKeys().spliterator(), false );
		Stream<ColumnReferenceMapping> mappingStream = keyStream.flatMap( k -> StreamSupport.stream( k.getColumnReferenceMappings().spliterator(), false ) );
		return mappingStream.anyMatch( mappingPredicate );
	}

	protected void checkExportIdentifier(Exportable exportable, Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException(
					String.format(
							"Export identifier [%s] encountered more than once",
							exportIdentifier
					)
			);
		}
		exportIdentifiers.add( exportIdentifier );
	}

	protected static void applySqlStrings(
			boolean quiet,
			String[] sqlStrings,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( sqlStrings != null ) {
			for ( String sqlString : sqlStrings ) {
				applySqlString( quiet, sqlString, formatter, options, targets );
			}
		}
	}

	protected void createSchemaAndCatalog(
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			Dialect dialect,
			Formatter formatter,
			boolean tryToCreateCatalogs,
			boolean tryToCreateSchemas,
			Set<Identifier> exportedCatalogs, Namespace namespace, GenerationTarget[] targets) {
		if ( tryToCreateCatalogs || tryToCreateSchemas ) {
			if ( tryToCreateCatalogs ) {
				final Identifier catalogLogicalName = namespace.getName().getCatalog();
				final Identifier catalogPhysicalName = namespace.getPhysicalName().getCatalog();

				if ( catalogPhysicalName != null && !exportedCatalogs.contains( catalogLogicalName )
						&& !existingDatabase.catalogExists( catalogLogicalName ) ) {
					applySqlStrings(
							false,
							dialect.getCreateCatalogCommand( catalogPhysicalName.render( dialect ) ),
							formatter,
							options,
							targets
					);
					exportedCatalogs.add( catalogLogicalName );
				}
			}

			if ( tryToCreateSchemas
					&& namespace.getPhysicalName().getSchema() != null
					&& !existingDatabase.schemaExists( namespace.getName() ) ) {
				applySqlStrings(
						false,
						dialect.getCreateSchemaCommand( namespace.getPhysicalName().getSchema().render( dialect ) ),
						formatter,
						options,
						targets
				);
			}
		}
	}

	private static void applySqlString(
			boolean quiet,
			String sqlString,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( !StringHelper.isEmpty( sqlString ) ) {
			String sqlStringFormatted = formatter.format( sqlString );
			for ( GenerationTarget target : targets ) {
				try {
					target.accept( sqlStringFormatted );
				}
				catch (CommandAcceptanceException e) {
					if ( !quiet ) {
						options.getExceptionHandler().handleException( e );
					}
					// otherwise ignore the exception
				}
			}
		}
	}

	private static void applySqlStrings(
			boolean quiet,
			Iterator<String> sqlStrings,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( sqlStrings != null ) {
			while ( sqlStrings.hasNext() ) {
				final String sqlString = sqlStrings.next();
				applySqlString( quiet, sqlString, formatter, options, targets );
			}
		}
	}
}
