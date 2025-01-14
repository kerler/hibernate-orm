/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.Collection;

/**
 * The consumer part of a CTE statement - the select or insert or delete or update that uses
 * the CTE
 *
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public interface CteContainer {

	boolean isWithRecursive();

	void setWithRecursive(boolean recursive);

	Collection<CteStatement> getCteStatements();

	CteStatement getCteStatement(String cteLabel);

	void addCteStatement(CteStatement cteStatement);
}
