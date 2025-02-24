/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * Commonality between `many-to-one`, `one-to-one` and `any`, as well as entity-valued collection elements and map-keys
 *
 * @author Steve Ebersole
 */
public interface EntityAssociationMapping extends ModelPart, Association, TableGroupJoinProducer {
	@Override
	default String getFetchableName() {
		return getPartName();
	}

	EntityMappingType getAssociatedEntityMappingType();

	/**
	 * The model sub-part relative to the associated entity type that is the target
	 * of this association's foreign-key
	 */
	ModelPart getKeyTargetMatchPart();

	@Override
	default boolean incrementFetchDepth(){
		return true;
	}

	/**
	 * Create a delayed DomainResult for a specific reference to this ModelPart.
	 */
	<T> DomainResult<T> createDelayedDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState);
}
