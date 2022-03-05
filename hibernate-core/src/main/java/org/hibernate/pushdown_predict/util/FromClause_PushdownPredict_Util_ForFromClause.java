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
import org.hibernate.persister.entity.UnionSubclassEntityPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FromClause_PushdownPredict_Util_ForFromClause {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( FromClause_PushdownPredict_Util_ForFromClause.class );

	public static final String regexBegin = "^";
	public static final String regexEnd = "$";
	public static final String regexAny = ".*";
	public static final String regexRightParentheses = "\\)";
	public static final String regexSpaces = "[\\s]*";
	public static final String regexVariableName = "[a-zA-Z_][a-zA-Z0-9_]*";
	public static final String regexDot = "\\.";
	public static final String regexEqualSign = "\\=";
	public static final String regexQuestionMark = "\\?";

	public static final String regexSingleWhereCondition_ForWholeString =
			regexBegin
					+ withLeadingSpaces(regexVariableName)
					+ withLeadingSpaces(regexDot)
					+ withLeadingSpaces(regexVariableName)
					+ withLeadingSpaces(regexEqualSign)
					+ withLeadingSpaces(regexQuestionMark)
					+ regexSpaces
					+ regexEnd;

	public static String makeFromClause_withPushdownPredictIntoFromClause_IfNeed(
			String fromClause,
			String fromClause_asSubqueryWithFormatTemplate,
			String outerJoinsAfterFrom,
			String outerJoinsAfterWhere,
			String whereClause) {

		final ParamaterValidationResult_ForPushdownPredictIntoFromClause paramaterValidationResult_forPushdownPredictIntoFromClause = validateParameterForPushdownPredictIntoFromClause(
				fromClause,
				fromClause_asSubqueryWithFormatTemplate,
				outerJoinsAfterFrom,
				outerJoinsAfterWhere,
				whereClause);

		if (! paramaterValidationResult_forPushdownPredictIntoFromClause.validParameter) {
			return fromClause;
		}

		return doMakeFromClause_withPushdownPredictIntoFromClause(fromClause_asSubqueryWithFormatTemplate, paramaterValidationResult_forPushdownPredictIntoFromClause.columnNameInWhereClause);
	}

	private static class ParamaterValidationResult_ForPushdownPredictIntoFromClause {
		public static final ParamaterValidationResult_ForPushdownPredictIntoFromClause FALSE
				= new ParamaterValidationResult_ForPushdownPredictIntoFromClause(false, null);

		public final boolean validParameter;
		public final String columnNameInWhereClause;

		private ParamaterValidationResult_ForPushdownPredictIntoFromClause(boolean validParameter, String columnNameInWhereClause) {
			this.validParameter = validParameter;
			this.columnNameInWhereClause = columnNameInWhereClause;
		}
	}

	private static ParamaterValidationResult_ForPushdownPredictIntoFromClause validateParameterForPushdownPredictIntoFromClause(
			String fromClause,
			String fromClause_asSubqueryWithFormatTemplate,
			String outerJoinsAfterFrom,
			String outerJoinsAfterWhere,
			String whereClause) {

		if (fromClause.equals(fromClause_asSubqueryWithFormatTemplate)) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("fromClause.equals(fromClause_asSubqueryWithFormatTemplate), so does NOT do pushdown-predict. fromClause is: '" + fromClause + "'.");
			}
			return ParamaterValidationResult_ForPushdownPredictIntoFromClause.FALSE;
		}

		if (StringHelper.isEmpty(fromClause_asSubqueryWithFormatTemplate)) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("fromClause_asSubqueryWithFormatTemplate is empty, so does NOT do pushdown-predict.");
			}
			return ParamaterValidationResult_ForPushdownPredictIntoFromClause.FALSE;
		}

//		if (StringHelper.isNotEmpty(outerJoinsAfterFrom)) {
//			return ParamaterValidationResult_ForPushdownPredictIntoFromClause.FALSE;
//		}
//
//		if (StringHelper.isNotEmpty(outerJoinsAfterWhere)) {
//			return ParamaterValidationResult_ForPushdownPredictIntoFromClause.FALSE;
//		}

		if (StringHelper.isEmpty(whereClause)) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("whereClause is empty, so does NOT do pushdown-predict.");
			}
			return ParamaterValidationResult_ForPushdownPredictIntoFromClause.FALSE;
		}

		if (! whereClause.matches(regexSingleWhereCondition_ForWholeString)) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("whereClause '" + whereClause + "' does NOT match regex '" + regexSingleWhereCondition_ForWholeString + "', so does NOT do pushdown-predict.");
			}
			return ParamaterValidationResult_ForPushdownPredictIntoFromClause.FALSE;
		}

		final List<String> tableNameAndColumnNameInWhereClause = findTwoMatchedVariableString(whereClause, regexVariableName);

		final String regex_rightParenthesesAndNormalStringAtEnd = getRegex_RightParenthesesAndNormalStringAtEnd(tableNameAndColumnNameInWhereClause.get(0));
		if (! fromClause_asSubqueryWithFormatTemplate.matches(regex_rightParenthesesAndNormalStringAtEnd)) {
			final String errorMsg = "fromClause_asSubqueryWithFormatTemplate '" + fromClause_asSubqueryWithFormatTemplate + "' does not match regex '" + regex_rightParenthesesAndNormalStringAtEnd + "'.";
			LOG.error(errorMsg);
			throw new IllegalStateException(errorMsg);
		}

		return new ParamaterValidationResult_ForPushdownPredictIntoFromClause(true, tableNameAndColumnNameInWhereClause.get(1));
	}

	private static String doMakeFromClause_withPushdownPredictIntoFromClause(String fromClause_asSubqueryWithFormatTemplate, String columnNameInWhereClause) {
		if ( LOG.isTraceEnabled() ) {
			LOG.trace("Enter doMakeFromClause_withPushdownPredictIntoFromClause(...). fromClause_asSubqueryWithFormatTemplate is '" + fromClause_asSubqueryWithFormatTemplate + "'. columnNameInWhereClause is '" + columnNameInWhereClause + "'.");
		}

		String newTextForFromElement = fromClause_asSubqueryWithFormatTemplate;

		while (newTextForFromElement.contains(UnionSubclassEntityPersister.formatSpecifierForPushdownPredict)) {

			newTextForFromElement = newTextForFromElement.replaceFirst(
					Pattern.quote(UnionSubclassEntityPersister.formatSpecifierForPushdownPredict),
					" where " + columnNameInWhereClause + "=?");
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debug("In doMakeFromClause_withPushdownPredictIntoFromClause(...), fromClause_asSubqueryWithFormatTemplate's text '" + fromClause_asSubqueryWithFormatTemplate + "' is replaced with '" + newTextForFromElement + "'.");
		}

		return newTextForFromElement;
	}

	private static String withLeadingSpaces(String regex) {
		return regexSpaces + regex;
	}

	private static String getRegex_RightParenthesesAndNormalStringAtEnd(String person0_) {
		return regexAny + regexRightParentheses + regexSpaces + person0_ + regexSpaces + regexEnd;
	}

	private static List<String> findTwoMatchedVariableString(String string, String regex) {
		final Pattern compile = Pattern.compile(regex);
		final Matcher matcher = compile.matcher(string);

		List<String> listStrFound = new ArrayList<>();
		while (matcher.find()) {
			listStrFound.add(matcher.group());
		}

		if (listStrFound.size() != 2) {
			throw new IllegalStateException("The result of finding regex '" + regex + "' in '" + string + "' is not 2 but " + listStrFound.size() + ".");
		}

		return listStrFound;
	}
}
