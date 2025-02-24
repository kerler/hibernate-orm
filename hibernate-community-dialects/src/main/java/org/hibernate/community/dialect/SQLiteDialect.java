/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.hibernate.ScrollMode;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.identity.SQLiteIdentityColumnSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.Replacer;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.query.IntervalType;
import org.hibernate.query.NullOrdering;
import org.hibernate.query.SemanticException;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.TrimSpec;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.TemporalUnit.DAY;
import static org.hibernate.query.TemporalUnit.EPOCH;
import static org.hibernate.query.TemporalUnit.MONTH;
import static org.hibernate.query.TemporalUnit.QUARTER;
import static org.hibernate.query.TemporalUnit.YEAR;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;

/**
 * An SQL dialect for SQLite.
 *
 * @author Christian Beikov
 * @author Vlad Mihalcea
 */
public class SQLiteDialect extends Dialect {

	private static final SQLiteIdentityColumnSupport IDENTITY_COLUMN_SUPPORT = new SQLiteIdentityColumnSupport();

	private final UniqueDelegate uniqueDelegate;
	private final DatabaseVersion version;

	public SQLiteDialect(DialectResolutionInfo info) {
		this( info.makeCopy() );
		registerKeywords( info );
	}

	public SQLiteDialect() {
		this( DatabaseVersion.make( 2, 0 ) );
	}

	public SQLiteDialect(DatabaseVersion version) {
		super();
		this.version = version;

		if ( version.isBefore( 3 ) ) {
			registerColumnType( Types.DECIMAL, "numeric($p,$s)" );
			registerColumnType( Types.CHAR, "char" );
			registerColumnType( Types.NCHAR, "nchar" );
		}
		// No precision support
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );
		registerColumnType( Types.TIME_WITH_TIMEZONE, "time" );

		registerColumnType( Types.BINARY, "blob" );
		registerColumnType( Types.VARBINARY, "blob" );
		uniqueDelegate = new SQLiteUniqueDelegate( this );
	}

	@Override
	public int getMaxVarbinaryLength() {
		//no varbinary type
		return -1;
	}

	private static class SQLiteUniqueDelegate extends DefaultUniqueDelegate {
		public SQLiteUniqueDelegate(Dialect dialect) {
			super( dialect );
		}
		@Override
		public String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context) {
			return " unique";
		}
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public DatabaseVersion getVersion() {
		return version;
	}

	/**
	 * The {@code extract()} function returns {@link TemporalUnit#DAY_OF_WEEK}
	 * numbered from 0 to 6. This isn't consistent with what most other
	 * databases do, so here we adjust the result by generating
	 * {@code (extract(dow,arg)+1))}.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		switch ( unit ) {
			case SECOND:
				return "cast(strftime('%S.%f',?2) as double)";
			case MINUTE:
				return "strftime('%M',?2)";
			case HOUR:
				return "strftime('%H',?2)";
			case DAY:
			case DAY_OF_MONTH:
				return "(strftime('%d',?2)+1)";
			case MONTH:
				return "strftime('%m',?2)";
			case YEAR:
				return "strftime('%Y',?2)";
			case DAY_OF_WEEK:
				return "(strftime('%w',?2)+1)";
			case DAY_OF_YEAR:
				return "strftime('%j',?2)";
			case EPOCH:
				return "strftime('%s',?2)";
			case WEEK:
				// Thanks https://stackoverflow.com/questions/15082584/sqlite-return-wrong-week-number-for-2013
				return "((strftime('%j',date(?2,'-3 days','weekday 4'))-1)/7+1)";
			default:
				return super.extractPattern(unit);
		}
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		final String function = temporalType == TemporalType.DATE ? "date" : "datetime";
		switch ( unit ) {
			case NANOSECOND:
			case NATIVE:
				return "datetime(?3,'+?2 seconds')";
			case QUARTER: //quarter is not supported in interval literals
				return function + "(?3,'+'||(?2*3)||' months')";
			case WEEK: //week is not supported in interval literals
				return function + "(?3,'+'||(?2*7)||' days')";
			default:
				return function + "(?3,'+?2 ?1s')";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		final StringBuilder pattern = new StringBuilder();
		switch ( unit ) {
			case YEAR:
				extractField( pattern, YEAR, unit );
				break;
			case QUARTER:
				pattern.append( "(" );
				extractField( pattern, YEAR, unit );
				pattern.append( "+" );
				extractField( pattern, QUARTER, unit );
				pattern.append( ")" );
				break;
			case MONTH:
				pattern.append( "(" );
				extractField( pattern, YEAR, unit );
				pattern.append( "+" );
				extractField( pattern, MONTH, unit );
				pattern.append( ")" );
				break;
			case WEEK: //week is not supported by extract() when the argument is a duration
			case DAY:
				extractField( pattern, DAY, unit );
				break;
			//in order to avoid multiple calls to extract(),
			//we use extract(epoch from x - y) * factor for
			//all the following units:
			case HOUR:
			case MINUTE:
			case SECOND:
			case NANOSECOND:
			case NATIVE:
				extractField( pattern, EPOCH, unit );
				break;
			default:
				throw new SemanticException( "unrecognized field: " + unit );
		}
		return pattern.toString();
	}

	private void extractField(
			StringBuilder pattern,
			TemporalUnit unit,
			TemporalUnit toUnit) {
		final String rhs = extractPattern( unit );
		final String lhs = rhs.replace( "?2", "?3" );
		pattern.append( '(');
		pattern.append( lhs );
		pattern.append( '-' );
		pattern.append( rhs );
		pattern.append(")").append( unit.conversionFactor( toUnit, this ) );
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		final BasicType<Integer> integerType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );

		CommonFunctionFactory.mod_operator( queryEngine );
		CommonFunctionFactory.leftRight_substr( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.characterLength_length( queryEngine, SqlAstNodeRenderingMode.DEFAULT );
		CommonFunctionFactory.leastGreatest_minMax( queryEngine );

		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		CommonFunctionFactory.chr_char( queryEngine );

		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				integerType,
				"instr(?2,?1)",
				"instr(?2,?1,?3)"
		).setArgumentListSignature("(pattern, string[, start])");
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"(substr(replace(hex(zeroblob(?2)),'00',' '),1,?2-length(?1))||?1)",
				"(substr(replace(hex(zeroblob(?2)),'00',?3),1,?2-length(?1))||?1)"
		).setArgumentListSignature("(string, length[, padding])");
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"(?1||substr(replace(hex(zeroblob(?2)),'00',' '),1,?2-length(?1)))",
				"(?1||substr(replace(hex(zeroblob(?2)),'00',?3),1,?2-length(?1)))"
		).setArgumentListSignature("(string, length[, padding])");

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder("format", "strftime")
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime as pattern)")
				.register();

		if (!supportsMathFunctions() ) {
			queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder(
					"floor",
					"(cast(?1 as int)-(?1<cast(?1 as int)))"
			).setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useArgType( 1 ) )
					.setExactArgumentCount( 1 )
					.register();
			queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder(
					"ceiling",
					"(cast(?1 as int)+(?1>cast(?1 as int)))"
			).setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useArgType( 1 ) )
					.setExactArgumentCount( 1 )
					.register();
		}
	}

	@Override
	public String trimPattern(TrimSpec specification, char character) {
		switch ( specification ) {
			case BOTH:
				return character == ' '
						? "trim(?1)"
						: "trim(?1,'" + character + "')";
			case LEADING:
				return character == ' '
						? "ltrim(?1)"
						: "ltrim(?1,'" + character + "')";
			case TRAILING:
				return character == ' '
						? "rtrim(?1)"
						: "rtrim(?1,'" + character + "')";
		}
		throw new UnsupportedOperationException( "Unsupported specification: " + specification );
	}

	protected boolean supportsMathFunctions() {
		// Math functions have to be enabled through a compile time option: https://www.sqlite.org/lang_mathfunc.html
		return true;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeDescriptorRegistry();
		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.PRIMITIVE_ARRAY_BINDING );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.STRING_BINDING );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitOffsetLimitHandler.INSTANCE;
	}

	@Override
	public boolean supportsLockTimeouts() {
		// may be http://sqlite.org/c3ref/db_mutex.html ?
		return false;
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return getVersion().isSameOrAfter( 3, 3 );
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SQLiteSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	private static final int SQLITE_BUSY = 5;
	private static final int SQLITE_LOCKED = 6;
	private static final int SQLITE_IOERR = 10;
	private static final int SQLITE_CORRUPT = 11;
	private static final int SQLITE_NOTFOUND = 12;
	private static final int SQLITE_FULL = 13;
	private static final int SQLITE_CANTOPEN = 14;
	private static final int SQLITE_PROTOCOL = 15;
	private static final int SQLITE_TOOBIG = 18;
	private static final int SQLITE_CONSTRAINT = 19;
	private static final int SQLITE_MISMATCH = 20;
	private static final int SQLITE_NOTADB = 26;

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );
				if (errorCode == SQLITE_CONSTRAINT) {
					return extractUsingTemplate( "constraint ", " failed", sqle.getMessage() );
				}
				return null;
			} );

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			switch ( errorCode ) {
				case SQLITE_TOOBIG:
				case SQLITE_MISMATCH:
					return new DataException( message, sqlException, sql );
				case SQLITE_BUSY:
				case SQLITE_LOCKED:
					return new LockAcquisitionException( message, sqlException, sql );
				case SQLITE_NOTADB:
					return new JDBCConnectionException( message, sqlException, sql );
				default:
					if ( errorCode >= SQLITE_IOERR && errorCode <= SQLITE_PROTOCOL ) {
						return new JDBCConnectionException( message, sqlException, sql );
					}
					return null;
			}
		};
	}

	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public boolean hasAlterTable() {
		// As specified in NHibernate dialect
		return false;
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getDropForeignKeyString() {
		throw new UnsupportedOperationException( "No drop foreign key syntax supported by SQLiteDialect" );
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		throw new UnsupportedOperationException( "No add foreign key syntax supported by SQLiteDialect" );
	}

	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		throw new UnsupportedOperationException( "No add primary key syntax supported by SQLiteDialect" );
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// TODO Validate (WAL mode...)
		return true;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	public int getInExpressionCountLimit() {
		// Compile/runtime time option: http://sqlite.org/limits.html#max_variable_number
		return 1000;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return IDENTITY_COLUMN_SUPPORT;
	}

	@Override
	public String getSelectGUIDString() {
		return "select hex(randomblob(16))";
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public String currentDate() {
		return "date('now')";
	}

	@Override
	public String currentTime() {
		return "time('now')";
	}

	@Override
	public String currentTimestamp() {
		return "datetime('now')";
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql( datetimeFormat( format ).result() );
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "" )
				.replace("%", "%%")

				//year
				.replace("yyyy", "%Y")
				.replace("yyy", "%Y")
				.replace("yy", "%y") //?????
				.replace("y", "%y") //?????

				//month of year
				.replace("MMMM", "%B") //?????
				.replace("MMM", "%b") //?????
				.replace("MM", "%m")
				.replace("M", "%m") //?????

				//day of week
				.replace("EEEE", "%A") //?????
				.replace("EEE", "%a") //?????
				.replace("ee", "%w")
				.replace("e", "%w") //?????

				//day of month
				.replace("dd", "%d")
				.replace("d", "%d") //?????

				//am pm
				.replace("aa", "%p") //?????
				.replace("a", "%p") //?????

				//hour
				.replace("hh", "%I") //?????
				.replace("HH", "%H")
				.replace("h", "%I") //?????
				.replace("H", "%H") //?????

				//minute
				.replace("mm", "%M")
				.replace("m", "%M") //?????

				//second
				.replace("ss", "%S")
				.replace("s", "%S") //?????

				//fractional seconds
				.replace("SSSSSS", "%f") //5 is the max
				.replace("SSSSS", "%f")
				.replace("SSSS", "%f")
				.replace("SSS", "%f")
				.replace("SS", "%f")
				.replace("S", "%f");
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		// All units should be handled in extractPattern so we should never hit this method
		throw new UnsupportedOperationException( "Unsupported unit: " + unit );
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date(" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( ')' );
				break;
			case TIME:
				appender.appendSql( "time(" );
				appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( ')' );
				break;
			case TIMESTAMP:
				appender.appendSql( "datetime(" );
				appendAsTimestampWithMicros( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( ')' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date(" );
				appendAsDate( appender, date );
				appender.appendSql( ')' );
				break;
			case TIME:
				appender.appendSql( "time(" );
				appendAsTime( appender, date );
				appender.appendSql( ')' );
				break;
			case TIMESTAMP:
				appender.appendSql( "datetime(" );
				appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
				appender.appendSql( ')' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "date(" );
				appendAsDate( appender, calendar );
				appender.appendSql( ')' );
				break;
			case TIME:
				appender.appendSql( "time(" );
				appendAsTime( appender, calendar );
				appender.appendSql( ')' );
				break;
			case TIMESTAMP:
				appender.appendSql( "datetime(" );
				appendAsTimestampWithMicros( appender, calendar, jdbcTimeZone );
				appender.appendSql( ')' );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

}
