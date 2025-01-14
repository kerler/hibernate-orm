/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;


public class PredicateRegexes {

	protected final Map<String, RegexPair> regexes = new HashMap<>();

	// Note that we alias the function invocation so that
	// we can map the return value to the required type
	public PredicateRegexes(String geomFromTextFunction) {
		add(
				"overlaps",
				"select .* from .* where st_overlaps\\(.*geom\\s*,.*" + geomFromTextFunction + "\\(.*\\)\\s*=.*",
				"select .* from .* where st_overlaps\\(.*geom\\s*,.s*?\\)\\s*=\\s*?.*"
		);
		add(
				"crosses",
				"select .* from .* where st_crosses\\(.*geom\\s*,.*" + geomFromTextFunction + "\\(.*\\)\\s*=.*",
				"select .* from .* where st_crosses\\(.*geom\\s*,.s*?\\)\\s*=\\s*?.*"

		);
		add(
				"contains",
				"select .* from .* where st_contains\\(.*geom\\s*,.*" + geomFromTextFunction + "\\(.*\\)\\s*=.*",
				"select .* from .* where st_contains\\(.*geom\\s*,.s*?\\)\\s*=\\s*?.*"
		);
		add(
				"disjoint",
				"select .* from .* where st_disjoint\\(.*geom\\s*,.*" + geomFromTextFunction + "\\(.*\\)\\s*=.*",
				"select .* from .* where st_disjoint\\(.*geom\\s*,.s*?\\)\\s*=\\s*?.*"
		);
		add(
				"touches",
				"select .* from .* where st_touches\\(.*geom\\s*,.*" + geomFromTextFunction + "\\(.*\\)\\s*=.*",
				"select .* from .* where st_touches\\(.*geom\\s*,.s*?\\)\\s*=\\s*?.*"
		);
		add(
				"within",
				"select .* from .* where st_within\\(.*geom\\s*,.*" + geomFromTextFunction + "\\(.*\\)\\s*=.*",
				"select .* from .* where st_within\\(.*geom\\s*,.s*?\\)\\s*=\\s*?.*"

		);
		add(
				"intersects",
				"select .* from .* where st_intersects\\(.*geom\\s*,.*" + geomFromTextFunction + "\\(.*\\)\\s*=.*",
				"select .* from .* where st_intersects\\(.*geom\\s*,.s*?\\)\\s*=\\s*?.*"
		);
		add(
				"eq",
				"select .* from .* where st_equals\\(.*geom\\s*,.*" + geomFromTextFunction + "\\(.*\\)\\s*=.*",
				"select .* from .* where st_equals\\(.*geom\\s*,.s*?\\)\\s*=\\s*?.*"
		);

	}

	public Stream<PredicateRegex> inlineModeRegexes() {
		return extractForMode( entry -> new PredicateRegex( entry.getKey(), entry.getValue().inlineMode ) );
	}

	public Stream<PredicateRegex> bindingModeRegexes() {
		return extractForMode( entry -> new PredicateRegex( entry.getKey(), entry.getValue().bindingMode ) );
	}

	private Stream<PredicateRegex> extractForMode(Function<Map.Entry<String, RegexPair>, PredicateRegex> mkPredRegex) {
		return this.regexes
				.entrySet()
				.stream()
				.map( mkPredRegex );
	}

	private void add(String predicate, String inlineRegex, String bindingRegex) {
		regexes.put( predicate, new RegexPair( inlineRegex, bindingRegex ) );
	}

	static public class PredicateRegex {
		public final String predicate;
		public final String regex;

		PredicateRegex(String predicate, String regex) {
			this.predicate = predicate;
			this.regex = regex;
		}
	}

	static class RegexPair {
		final String inlineMode;
		final String bindingMode;

		private RegexPair(String inline, String binding) {
			inlineMode = inline;
			bindingMode = binding;
		}
	}
}
