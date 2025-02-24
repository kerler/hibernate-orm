/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlExpressionAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SqlSelection used in {@link ResultSetMapping} resolution.  Doubles as its own
 * {@link Expression} as well.
 *
 * @author Steve Ebersole
 */
public class ResultSetMappingSqlSelection implements SqlSelection, Expression, SqlExpressionAccess {
	private final int valuesArrayPosition;
	private final BasicValuedMapping valueMapping;
	private final JdbcMapping jdbcMapping;

	public ResultSetMappingSqlSelection(int valuesArrayPosition, BasicValuedMapping valueMapping) {
		this.valuesArrayPosition = valuesArrayPosition;
		this.valueMapping = valueMapping;
		this.jdbcMapping = valueMapping.getJdbcMapping();
	}

	public ResultSetMappingSqlSelection(int valuesArrayPosition, JdbcMapping jdbcMapping) {
		this.valuesArrayPosition = valuesArrayPosition;
		this.jdbcMapping = jdbcMapping;
		this.valueMapping = null;
	}

	@Override
	public ValueExtractor getJdbcValueExtractor() {
		return jdbcMapping.getJdbcValueExtractor();
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaType javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return this;
	}

	@Override
	public SqlSelection resolve(JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory) {
		return this;
	}

	@Override
	public int getValuesArrayPosition() {
		return valuesArrayPosition;
	}

	@Override
	public int getJdbcResultSetIndex() {
		return valuesArrayPosition + 1;
	}

	@Override
	public Expression getExpression() {
		return this;
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return valueMapping;
	}

	@Override
	public void accept(SqlAstWalker sqlAstWalker) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Expression getSqlExpression() {
		return this;
	}
}
