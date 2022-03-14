/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.pushdown_predict.util;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;

import java.util.regex.Pattern;

public final class FromClause_PushdownPredict_Util_ForPositionalParameters {

	private FromClause_PushdownPredict_Util_ForPositionalParameters() {
		throw new AssertionError("Instantiating utility class.");
	}

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( FromClause_PushdownPredict_Util_ForPositionalParameters.class );

	public static int getNumberOfPositionalParameterTypesAndValues_toDuplicateForPushdownPredictIntoFromClause(EntityPersister entityPersister, String sqlStatement) {
		if (! (entityPersister instanceof UnionSubclassEntityPersister)) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("entityPersister is NOT instanceof UnionSubclassEntityPersister, its class is '" + entityPersister.getClass().getName() + "', so return 0 from getNumberOfPositionalParameterTypesAndValues_toDuplicateForPushdownPredictIntoFromClause().");
			}
			return 0;
		}

		final int numberOf_formatSpecifierForPushdownPredict_inSubqueryWithFormatTemplate = ((UnionSubclassEntityPersister) entityPersister).getSubClassTableColumnNullValues().tableColumnNullValues.size();
		final int numberOfMatchedQuestionMarkInSqlStatement = countStringMatches(sqlStatement, "?");

		if (numberOfMatchedQuestionMarkInSqlStatement-1 != numberOf_formatSpecifierForPushdownPredict_inSubqueryWithFormatTemplate) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debug("numberOfMatchedQuestionMarkInSqlStatement-1 != numberOf_formatSpecifierForPushdownPredict_inSubqueryWithFormatTemplate. "
						+ "numberOfMatchedQuestionMarkInSqlStatement: " + numberOfMatchedQuestionMarkInSqlStatement
						+ ", numberOf_formatSpecifierForPushdownPredict_inSubqueryWithFormatTemplate: " + numberOf_formatSpecifierForPushdownPredict_inSubqueryWithFormatTemplate
						+ ", sqlStatement: " + sqlStatement + ".");
			}

			return 0;
		}

		if ( LOG.isTraceEnabled() ) {
			LOG.trace("getNumberOfPositionalParameterTypesAndValues_toDuplicateForPushdownPredictIntoFromClause(): " + numberOf_formatSpecifierForPushdownPredict_inSubqueryWithFormatTemplate
					+ ", for sqlStatement: " + sqlStatement + ".");
		}

		return numberOf_formatSpecifierForPushdownPredict_inSubqueryWithFormatTemplate;
	}

	public static int countStringMatches(String text, String strToBeFound) {
		if (StringHelper.isEmpty(text) || StringHelper.isEmpty(strToBeFound)) {
			return 0;
		}

		return text.split( Pattern.quote(strToBeFound), -1 ).length - 1;
	}
}
