/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cfg.Settings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.collections.SingletonIterator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.pushdown_predict.util.FromClause_PushdownPredict_Util_ForPositionalParameters;
import org.hibernate.sql.SelectFragment;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Implementation of the "table-per-concrete-class" or "roll-down" mapping
 * strategy for an entity and its inheritance hierarchy.
 *
 * @author Gavin King
 */
public class UnionSubclassEntityPersister extends AbstractEntityPersister {

	// When using String.format(..., someArg) to replace the format specifier, the someArg should be either "" or a string with a leading white space such as " where x=?".
	public static final String formatSpecifierForPushdownPredict = "%1$s";

	// the class hierarchy structure
	private final String subquery;
	private final String subqueryWithFormatTemplate;
	private final TableColumnNullValues subClassTableColumnNullValues; //Order of subclasses in this variable is the same as order of subclasses in 'subquery'.
	private final String tableName;
	//private final String rootTableName;
	private final String[] subclassClosure;
	private final String[] spaces;
	private final String[] subclassSpaces;
	private final Object discriminatorValue;
	private final String discriminatorSQLValue;
	private final Map subclassByDiscriminatorValue = new HashMap();

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	//INITIALIZATION:

	public UnionSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {

		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );

		if ( getIdentifierGenerator() instanceof IdentityGenerator ) {
			throw new MappingException(
					"Cannot use identity column key generation with <union-subclass> mapping for: " +
							getEntityName()
			);
		}

		final SessionFactoryImplementor factory = creationContext.getSessionFactory();
		final Database database = creationContext.getMetadata().getDatabase();

		// TABLE

		tableName = determineTableName( persistentClass.getTable() );

		//Custom SQL

		String sql;
		boolean callable = false;
		ExecuteUpdateResultCheckStyle checkStyle = null;
		sql = persistentClass.getCustomSQLInsert();
		callable = sql != null && persistentClass.isCustomInsertCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLInsertCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLInsertCheckStyle();
		customSQLInsert = new String[] {sql};
		insertCallable = new boolean[] {callable};
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[] {checkStyle};

		sql = persistentClass.getCustomSQLUpdate();
		callable = sql != null && persistentClass.isCustomUpdateCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLUpdateCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLUpdateCheckStyle();
		customSQLUpdate = new String[] {sql};
		updateCallable = new boolean[] {callable};
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[] {checkStyle};

		sql = persistentClass.getCustomSQLDelete();
		callable = sql != null && persistentClass.isCustomDeleteCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLDeleteCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLDeleteCheckStyle();
		customSQLDelete = new String[] {sql};
		deleteCallable = new boolean[] {callable};
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[] {checkStyle};

		discriminatorValue = persistentClass.getSubclassId();
		discriminatorSQLValue = String.valueOf( persistentClass.getSubclassId() );

		// PROPERTIES

		int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();

		// SUBCLASSES
		subclassByDiscriminatorValue.put(
				persistentClass.getSubclassId(),
				persistentClass.getEntityName()
		);
		if ( persistentClass.isPolymorphic() ) {
			Iterator<Subclass> subclassIter = persistentClass.getSubclassIterator();
			int k = 1;
			while ( subclassIter.hasNext() ) {
				Subclass subclass = subclassIter.next();
				subclassClosure[k++] = subclass.getEntityName();
				subclassByDiscriminatorValue.put( subclass.getSubclassId(), subclass.getEntityName() );
			}
		}

		//SPACES
		//TODO: I'm not sure, but perhaps we should exclude
		//      abstract denormalized tables?

		int spacesSize = 1 + persistentClass.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = tableName;
		Iterator<String> iter = persistentClass.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = iter.next();
		}

		HashSet<String> subclassTables = new HashSet();
		Iterator<Table> subclassTableIter = persistentClass.getSubclassTableClosureIterator();
		while ( subclassTableIter.hasNext() ) {
			subclassTables.add( determineTableName( subclassTableIter.next() ) );
		}
		subclassSpaces = ArrayHelper.toStringArray( subclassTables );

		subquery = generateSubquery( persistentClass, creationContext.getMetadata() ).subquery;
		final ResultParamObjectOfGenerateSubquery subqueryWithFormatTemplate = generateSubquery( persistentClass, creationContext.getMetadata(), true );
		this.subqueryWithFormatTemplate = subqueryWithFormatTemplate.subquery;
		this.subClassTableColumnNullValues = subqueryWithFormatTemplate.tableColumnNullValues;

		if ( isMultiTable() ) {
			int idColumnSpan = getIdentifierColumnSpan();
			ArrayList<String> tableNames = new ArrayList<>();
			ArrayList<String[]> keyColumns = new ArrayList<>();
			Iterator<Table> tableIter = persistentClass.getSubclassTableClosureIterator();
			while ( tableIter.hasNext() ) {
				Table tab = tableIter.next();
				if ( !tab.isAbstractUnionTable() ) {
					final String tableName = determineTableName( tab );
					tableNames.add( tableName );
					String[] key = new String[idColumnSpan];
					Iterator<Column> citer = tab.getPrimaryKey().getColumnIterator();
					for ( int k = 0; k < idColumnSpan; k++ ) {
						key[k] = citer.next().getQuotedName( factory.getDialect() );
					}
					keyColumns.add( key );
				}
			}

			constraintOrderedTableNames = ArrayHelper.toStringArray( tableNames );
			constraintOrderedKeyColumnNames = ArrayHelper.to2DStringArray( keyColumns );
		}
		else {
			constraintOrderedTableNames = new String[] {tableName};
			constraintOrderedKeyColumnNames = new String[][] {getIdentifierColumnNames()};
		}

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( creationContext.getMetadata() );

	}

	public Serializable[] getQuerySpaces() {
		return subclassSpaces;
	}

	public String getTableName() {
		return subquery;
	}

	@Override
	public String getTableName_asSubqueryWithFormatTemplate() {
		return subqueryWithFormatTemplate;
	}

	public TableColumnNullValues getSubClassTableColumnNullValues() {
		return subClassTableColumnNullValues;
	}

	public Type getDiscriminatorType() {
		return StandardBasicTypes.INTEGER;
	}

	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	public String[] getSubclassClosure() {
		return subclassClosure;
	}

	public String getSubclassForDiscriminatorValue(Object value) {
		return (String) subclassByDiscriminatorValue.get( value );
	}

	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	protected boolean isDiscriminatorFormula() {
		return false;
	}

	/**
	 * Generate the SQL that selects a row by id
	 */
	protected String generateSelectString(LockMode lockMode) {
		SimpleSelect select = new SimpleSelect( getFactory().getDialect() )
				.setLockMode( lockMode )
				.setTableName( getTableName() )
				.addColumns( getIdentifierColumnNames() )
				.addColumns(
						getSubclassColumnClosure(),
						getSubclassColumnAliasClosure(),
						getSubclassColumnLazyiness()
				)
				.addColumns(
						getSubclassFormulaClosure(),
						getSubclassFormulaAliasClosure(),
						getSubclassFormulaLazyiness()
				);
		//TODO: include the row ids!!!!
		if ( hasSubclasses() ) {
			if ( isDiscriminatorFormula() ) {
				select.addColumn( getDiscriminatorFormula(), getDiscriminatorAlias() );
			}
			else {
				select.addColumn( getDiscriminatorColumnName(), getDiscriminatorAlias() );
			}
		}
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( "load " + getEntityName() );
		}
		return select.addCondition( getIdentifierColumnNames(), "=?" ).toStatementString();
	}

	protected String getDiscriminatorFormula() {
		return null;
	}

	public String getTableName(int j) {
		return tableName;
	}

	public String[] getKeyColumns(int j) {
		return getIdentifierColumnNames();
	}

	public boolean isTableCascadeDeleteEnabled(int j) {
		return false;
	}

	public boolean isPropertyOfTable(int property, int j) {
		return true;
	}

	// Execute the SQL:

	public String fromTableFragment(String name) {
		return getTableName() + ' ' + name;
	}

	@Override
	public String fromTableFragment_asSubqueryWithFormatTemplate(String name) {
		return getTableName_asSubqueryWithFormatTemplate() + ' ' + name;
	}

	@Override
	protected String filterFragment(String name) {
		return hasWhere()
				? " and " + getSQLWhereString( name )
				: "";
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		return filterFragment( alias );
	}

	public String getSubclassPropertyTableName(int i) {
		return getTableName();//ie. the subquery! yuck!
	}

	protected void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
		select.addColumn( name, getDiscriminatorColumnName(), getDiscriminatorAlias() );
	}

	protected int[] getPropertyTableNumbersInSelect() {
		return new int[getPropertySpan()];
	}

	protected int getSubclassPropertyTableNumber(int i) {
		return 0;
	}

	public int getSubclassPropertyTableNumber(String propertyName) {
		return 0;
	}

	public boolean isMultiTable() {
		// This could also just be true all the time...
		return isAbstract() || hasSubclasses();
	}

	public int getTableSpan() {
		return 1;
	}

	protected int[] getSubclassColumnTableNumberClosure() {
		return new int[getSubclassColumnClosure().length];
	}

	protected int[] getSubclassFormulaTableNumberClosure() {
		return new int[getSubclassFormulaClosure().length];
	}

	protected boolean[] getTableHasColumns() {
		return new boolean[] {true};
	}

	protected int[] getPropertyTableNumbers() {
		return new int[getPropertySpan()];
	}

	@Override
	protected void getIdentifierType_nullSafeSet__ForPushdownPredict_IfNeed(PreparedStatement ps, Serializable id, SharedSessionContractImplementor session, String sqlSnapshotSelectString) throws SQLException {
		final int iDuplicateNumberOfPositionalParameterTypesAndValues = 1 + FromClause_PushdownPredict_Util_ForPositionalParameters.getNumberOfPositionalParameterTypesAndValues_toDuplicateForPushdownPredictIntoFromClause(this, sqlSnapshotSelectString);

		for (int index = 1 ; index <= iDuplicateNumberOfPositionalParameterTypesAndValues ; index ++) {
			getIdentifierType().nullSafeSet(ps, id, index, session);
		}
	}

	protected ResultParamObjectOfGenerateSubquery generateSubquery(PersistentClass model, Mapping mapping) {
		return generateSubquery(model, mapping, false);
	}

	protected ResultParamObjectOfGenerateSubquery generateSubquery(PersistentClass model, Mapping mapping, boolean generateFormatTemplate) {

		TableColumnNullValues tableColumnNullValues = new TableColumnNullValues();

		Dialect dialect = getFactory().getDialect();
		Settings settings = getFactory().getSettings();
		SqlStringGenerationContext sqlStringGenerationContext = getFactory().getSqlStringGenerationContext();

		if ( !model.hasSubclasses() ) {
			return new ResultParamObjectOfGenerateSubquery(
				model.getTable().getQualifiedName(
					sqlStringGenerationContext
				),
				tableColumnNullValues
			);
		}

		HashSet columns = new LinkedHashSet();
		Iterator titer = model.getSubclassTableClosureIterator();
		while ( titer.hasNext() ) {
			Table table = (Table) titer.next();
			if ( !table.isAbstractUnionTable() ) {
				Iterator citer = table.getColumnIterator();
				while ( citer.hasNext() ) {
					columns.add( citer.next() );
				}
			}
		}

		StringBuilder buf = new StringBuilder()
				.append( "( " );

		Iterator siter = new JoinedIterator(
				new SingletonIterator( model ),
				model.getSubclassIterator()
		);


		while ( siter.hasNext() ) {
			PersistentClass clazz = (PersistentClass) siter.next();
			Table table = clazz.getTable();
			if ( !table.isAbstractUnionTable() ) {
				//TODO: move to .sql package!!

				final String tableQualifiedName = table.getQualifiedName(
						sqlStringGenerationContext
				);

				tableColumnNullValues.putTableName(tableQualifiedName);
				buf.append( "select " );

				Iterator citer = columns.iterator();
				while ( citer.hasNext() ) {
					Column col = (Column) citer.next();
					final String colQuotedName = col.getQuotedName(dialect);

					if ( !table.containsColumn( col ) ) {
						int sqlType = col.getSqlTypeCode( mapping );
						final String colNullString = dialect.getSelectClauseNullString(sqlType);

						tableColumnNullValues.putTableColumnNullValue(tableQualifiedName, colQuotedName, colNullString);
						buf.append(colNullString)
								.append( " as " );
					}
					buf.append(colQuotedName);
					buf.append( ", " );
				}

				final int subclassId = clazz.getSubclassId();

				tableColumnNullValues.putTableColumnNullValue(tableQualifiedName, "clazz_", String.valueOf(subclassId));
				buf.append(subclassId)
						.append( " as clazz_" );

				buf.append( " from " )
						.append(tableQualifiedName);

				if (generateFormatTemplate) {
					buf.append(formatSpecifierForPushdownPredict);
				}

				buf.append( " union " );
				if ( dialect.supportsUnionAll() ) {
					buf.append( "all " );
				}
			}
		}

		if ( buf.length() > 2 ) {
			//chop the last union (all)
			buf.setLength( buf.length() - ( dialect.supportsUnionAll() ? 11 : 7 ) );
		}

		return new ResultParamObjectOfGenerateSubquery( buf.append( " )" ).toString(), tableColumnNullValues);
	}

	private static class ResultParamObjectOfGenerateSubquery {
		public final String subquery;
		public final TableColumnNullValues tableColumnNullValues;

		private ResultParamObjectOfGenerateSubquery(String subquery, TableColumnNullValues tableColumnNullValues) {
			this.subquery = subquery;
			this.tableColumnNullValues = tableColumnNullValues;
		}
	}

	protected String[] getSubclassTableKeyColumns(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return getIdentifierColumnNames();
	}

	public String getSubclassTableName(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return tableName;
	}

	public int getSubclassTableSpan() {
		return 1;
	}

	protected boolean isClassOrSuperclassTable(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return true;
	}

	@Override
	public String getPropertyTableName(String propertyName) {
		//TODO: check this....
		return getTableName();
	}

	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	public String[][] getContraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}

	public static class TableColumnNullValues {
		public final Map<String, Map<String, String>> tableColumnNullValues = makeMap(); //String tableName, String columnName, String columnNullValue

		public String getTableColumnNullValue(String tableName, String columnName) {
			return getFromMapOfMap(tableName, columnName, tableColumnNullValues);
		}

		public Map<String, String> putTableName(String tableName) {
			return putIntoMap(tableName, tableColumnNullValues);
		}

		public String putTableColumnNullValue(String tableName, String columnName, String nullValueOfColumn) {
			return putIntoMapOfMap(tableName, columnName, nullValueOfColumn, tableColumnNullValues);
		}

		private static String getFromMapOfMap(String key1, String key2, Map<String, Map<String, String>> mapOfMap) {
			if (! mapOfMap.containsKey(key1)) {
				return null;
			}

			final Map<String, String> stringIntegerMap = mapOfMap.get(key1);
			if (! stringIntegerMap.containsKey(key2)) {
				return null;
			}

			return stringIntegerMap.get(key2);
		}

		private static  Map<String, String>  putIntoMap(String key1, Map<String, Map<String, String>> mapOfMap) {
			if ( mapOfMap.containsKey(key1) ) {
				throw new IllegalStateException("key '" + key1 + "' in mapOfMap has already existed.");
			}

			return mapOfMap.put(key1, makeMap());
		}

		private static String putIntoMapOfMap(String key1, String key2, String value, Map<String, Map<String, String>> mapOfMap) {
			if (! mapOfMap.containsKey(key1)) {
				throw new IllegalStateException("key '" + key1 + "' in mapOfMap does NOT exist.");
			}

			return mapOfMap.get(key1).put(key2, value);
		}

		private static Map makeMap() {
			return new LinkedHashMap<>();
		}
	}
}
