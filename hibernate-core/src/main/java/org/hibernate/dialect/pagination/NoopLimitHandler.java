/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.query.Limit;

import static java.util.regex.Pattern.compile;

/**
 * Handler not supporting query LIMIT clause. JDBC API is used to set maximum number of returned rows.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class NoopLimitHandler extends AbstractLimitHandler {

	public static final NoopLimitHandler INSTANCE = new NoopLimitHandler();

	@Override
	public String processSql(String sql, Limit limit) {
		return sql;
	}

	@Override
	public int bindLimitParametersAtStartOfQuery(Limit limit, PreparedStatement statement, int index) {
		return 0;
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(Limit limit, PreparedStatement statement, int index) {
		return 0;
	}

	@Override
	public void setMaxRows(Limit limit, PreparedStatement statement) throws SQLException {
		if ( limit != null && limit.getMaxRows() != null && limit.getMaxRows() > 0 ) {
			final int maxRows = limit.getMaxRows() + convertToFirstRowValue(
					limit.getFirstRow() == null ? 0 : limit.getFirstRow()
			);
			// Use Integer.MAX_VALUE on overflow
			if ( maxRows < 0 ) {
				statement.setMaxRows( Integer.MAX_VALUE );
			}
			else {
				statement.setMaxRows( maxRows );
			}
		}
	}
}
