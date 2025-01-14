/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;

/**
 * @author Steve Ebersole
 */
public interface EntityFetch extends EntityResultGraphNode, Fetch {
	@Override
	default boolean containsAnyNonScalarResults() {
		return true;
	}

	@Override
	default DomainResult<?> asResult(DomainResultCreationState creationState) {
		throw new UnsupportedOperationException( "EntityFetch -> DomainResult not supported" );
	}
}
