/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.hql.internal.ast;

import antlr.collections.AST;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.tree.DotNode;
import org.hibernate.hql.internal.ast.tree.MethodNode;
import org.hibernate.hql.internal.ast.tree.SqlNode;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import java.util.Map;

/**
 * @author Baogang Liu
 */
public class SqlGeneratorForPushdownPredictIntoFromElement extends SqlGenerator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SqlGeneratorForPushdownPredictIntoFromElement.class );

	private final String tableAliasToBeRemovedFromPropertyOrAliasExpression_withDotPostfix;
	private final String subclassTableName;
	private final Map<String, String> subclassTableColumnNullValue;

	public SqlGeneratorForPushdownPredictIntoFromElement(SessionFactoryImplementor sfi, String tableAliasToBeRemovedFromPropertyOrAliasExpression, String subclassTableName, Map<String, String> subclassTableColumnNullValue) {
		super(sfi);
		this.tableAliasToBeRemovedFromPropertyOrAliasExpression_withDotPostfix = tableAliasToBeRemovedFromPropertyOrAliasExpression + ".";
		this.subclassTableName = subclassTableName;
		this.subclassTableColumnNullValue = subclassTableColumnNullValue;
	}

	@Override
	protected void out(AST n) {
		if ( ! (n instanceof DotNode || n instanceof MethodNode) ) {
			if ( n.getText().startsWith(tableAliasToBeRemovedFromPropertyOrAliasExpression_withDotPostfix) ) {
				LOG.warnf( "AST n is not DotNode or MethodNode but n.getText().startsWith(tableAliasToBeRemovedFromPropertyOrAliasExpression_withDotPostfix). " +
						"n's type is: [%s]. n.getText() is: [%s]. tableAliasToBeRemovedFromPropertyOrAliasExpression_withDotPostfix is: [%s].",
						n.getClass().getName(), n.getText(), tableAliasToBeRemovedFromPropertyOrAliasExpression_withDotPostfix );
			}

			super.out(n);
			return;
		}

		if ( ! n.getText().startsWith(tableAliasToBeRemovedFromPropertyOrAliasExpression_withDotPostfix) ) {
			super.out(n);
			return;
		}

		String columnNameOrNullValue = n.getText().substring(tableAliasToBeRemovedFromPropertyOrAliasExpression_withDotPostfix.length());
		if (subclassTableColumnNullValue.containsKey(columnNameOrNullValue)) {
			columnNameOrNullValue = subclassTableColumnNullValue.get(columnNameOrNullValue);
		}

		SqlNode tmpSqlNode = makeEmptySqlNodeAccordingToAstType(n);
		tmpSqlNode.setText(columnNameOrNullValue);
		super.out(tmpSqlNode);
	}

	private SqlNode makeEmptySqlNodeAccordingToAstType(AST n) {
		if ( n instanceof DotNode ) {
			return new DotNode();
		}

		if ( n instanceof MethodNode ) {
			return new MethodNode();
		}

		throw new IllegalStateException("Should not reach here. Unsupported node type '" + n.getClass() + "'.");
	}
}
