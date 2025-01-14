/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.internal.util.StringHelper;

/**
 * Performs formatting of basic SQL statements (DML + query).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BasicFormatterImpl implements Formatter {

	private static final Set<String> BEGIN_CLAUSES = Set.of(
			"left",	"right", "inner", "outer", "group", "order"
	);
	private static final Set<String> END_CLAUSES = Set.of(
			"where", "set", "having", "from", "by", "join", "into", "union"
	);
	private static final Set<String> LOGICAL = Set.of(
			"and", "or", "when", "else", "end"
	);
	private static final Set<String> QUANTIFIERS = Set.of(
			"in", "all", "exists", "some", "any"
	);
	private static final Set<String> DML = Set.of(
			"insert", "update", "delete"
	);
	private static final Set<String> MISC = Set.of(
			"select", "on"
	);

	private static final String INDENT_STRING = "    ";
	private static final String INITIAL = System.lineSeparator() + INDENT_STRING;

	@Override
	public String format(String source) {
		return new FormatProcess( source ).perform();
	}

	private static class FormatProcess {
		boolean beginLine = true;
		boolean afterBeginBeforeEnd;
		boolean afterByOrSetOrFromOrSelect;
		boolean afterOn;
		boolean afterBetween;
		boolean afterInsert;
		int inFunction;
		int parensSinceSelect;
		private LinkedList<Integer> parenCounts = new LinkedList<>();
		private LinkedList<Boolean> afterByOrFromOrSelects = new LinkedList<>();

		int indent = 1;

		StringBuilder result = new StringBuilder();
		StringTokenizer tokens;
		String lastToken;
		String token;
		String lcToken;

		public FormatProcess(String sql) {
			assert sql != null : "SQL to format should not be null";

			tokens = new StringTokenizer(
					sql,
					"()+*/-=<>'`\"[]," + StringHelper.WHITESPACE,
					true
			);
		}

		public String perform() {

			result.append( INITIAL );

			while ( tokens.hasMoreTokens() ) {
				token = tokens.nextToken();
				lcToken = token.toLowerCase(Locale.ROOT);

				if ( "'".equals( token ) ) {
					String t;
					do {
						t = tokens.nextToken();
						token += t;
					}
					// cannot handle single quotes
					while ( !"'".equals( t ) && tokens.hasMoreTokens() );
				}
				else if ( "\"".equals( token ) ) {
					String t;
					do {
						t = tokens.nextToken();
						token += t;
					}
					while ( !"\"".equals( t )  && tokens.hasMoreTokens() );
				}
				// SQL Server uses "[" and "]" to escape reserved words
				// see SQLServerDialect.openQuote and SQLServerDialect.closeQuote
				else if ( "[".equals( token ) ) {
					String t;
					do {
						t = tokens.nextToken();
						token += t;
					}
					while ( !"]".equals( t ) && tokens.hasMoreTokens());
				}

				if ( afterByOrSetOrFromOrSelect && ",".equals( token ) ) {
					commaAfterByOrFromOrSelect();
				}
				else if ( afterOn && ",".equals( token ) ) {
					commaAfterOn();
				}

				else if ( "(".equals( token ) ) {
					openParen();
				}
				else if ( ")".equals( token ) ) {
					closeParen();
				}

				else if ( BEGIN_CLAUSES.contains( lcToken ) ) {
					beginNewClause();
				}

				else if ( END_CLAUSES.contains( lcToken ) ) {
					endNewClause();
				}

				else if ( "select".equals( lcToken ) ) {
					select();
				}

				else if ( DML.contains( lcToken ) ) {
					updateOrInsertOrDelete();
				}

				else if ( "values".equals( lcToken ) ) {
					values();
				}

				else if ( "on".equals( lcToken ) ) {
					on();
				}

				else if ( afterBetween && lcToken.equals( "and" ) ) {
					misc();
					afterBetween = false;
				}

				else if ( LOGICAL.contains( lcToken ) ) {
					logical();
				}

				else if ( isWhitespace( token ) ) {
					white();
				}

				else {
					misc();
				}

				if ( !isWhitespace( token ) ) {
					lastToken = lcToken;
				}

			}
			return result.toString();
		}

		private void commaAfterOn() {
			out();
			indent--;
			newline();
			afterOn = false;
			afterByOrSetOrFromOrSelect = true;
		}

		private void commaAfterByOrFromOrSelect() {
			out();
			newline();
		}

		private void logical() {
			if ( "end".equals( lcToken ) ) {
				indent--;
			}
			newline();
			out();
			beginLine = false;
		}

		private void on() {
			indent++;
			afterOn = true;
			newline();
			out();
			beginLine = false;
		}

		private void misc() {
			out();
			if ( "between".equals( lcToken ) ) {
				afterBetween = true;
			}
			if ( afterInsert ) {
				newline();
				afterInsert = false;
			}
			else {
				beginLine = false;
				if ( "case".equals( lcToken ) ) {
					indent++;
				}
			}
		}

		private void white() {
			if ( !beginLine ) {
				result.append( " " );
			}
		}

		private void updateOrInsertOrDelete() {
			out();
			indent++;
			beginLine = false;
			if ( "update".equals( lcToken ) ) {
				newline();
			}
			if ( "insert".equals( lcToken ) ) {
				afterInsert = true;
			}
		}

		private void select() {
			out();
			indent++;
			newline();
			parenCounts.addLast( parensSinceSelect );
			afterByOrFromOrSelects.addLast( afterByOrSetOrFromOrSelect );
			parensSinceSelect = 0;
			afterByOrSetOrFromOrSelect = true;
		}

		private void out() {
			result.append( token );
		}

		private void endNewClause() {
			if ( !afterBeginBeforeEnd ) {
				indent--;
				if ( afterOn ) {
					indent--;
					afterOn = false;
				}
				newline();
			}
			out();
			if ( !"union".equals( lcToken ) ) {
				indent++;
			}
			newline();
			afterBeginBeforeEnd = false;
			afterByOrSetOrFromOrSelect = "by".equals( lcToken )
					|| "set".equals( lcToken )
					|| "from".equals( lcToken );
		}

		private void beginNewClause() {
			if ( !afterBeginBeforeEnd ) {
				if ( afterOn ) {
					indent--;
					afterOn = false;
				}
				indent--;
				newline();
			}
			out();
			beginLine = false;
			afterBeginBeforeEnd = true;
		}

		private void values() {
			indent--;
			newline();
			out();
			indent++;
			newline();
		}

		private void closeParen() {
			parensSinceSelect--;
			if ( parensSinceSelect < 0 ) {
				indent--;
				parensSinceSelect = parenCounts.removeLast();
				afterByOrSetOrFromOrSelect = afterByOrFromOrSelects.removeLast();
			}
			if ( inFunction > 0 ) {
				inFunction--;
				out();
			}
			else {
				if ( !afterByOrSetOrFromOrSelect ) {
					indent--;
					newline();
				}
				out();
			}
			beginLine = false;
		}

		private void openParen() {
			if ( isFunctionName( lastToken ) || inFunction > 0 ) {
				inFunction++;
			}
			beginLine = false;
			if ( inFunction > 0 ) {
				out();
			}
			else {
				out();
				if ( !afterByOrSetOrFromOrSelect ) {
					indent++;
					newline();
					beginLine = true;
				}
			}
			parensSinceSelect++;
		}

		private static boolean isFunctionName(String tok) {
			if ( tok == null || tok.length() == 0 ) {
				return false;
			}

			final char begin = tok.charAt( 0 );
			final boolean isIdentifier = Character.isJavaIdentifierStart( begin ) || '"' == begin;
			return isIdentifier &&
					!LOGICAL.contains( tok ) &&
					!END_CLAUSES.contains( tok ) &&
					!QUANTIFIERS.contains( tok ) &&
					!DML.contains( tok ) &&
					!MISC.contains( tok );
		}

		private static boolean isWhitespace(String token) {
			return StringHelper.WHITESPACE.contains( token );
		}

		private void newline() {
			result.append( System.lineSeparator() );
			for ( int i = 0; i < indent; i++ ) {
				result.append( INDENT_STRING );
			}
			beginLine = true;
		}
	}

}
