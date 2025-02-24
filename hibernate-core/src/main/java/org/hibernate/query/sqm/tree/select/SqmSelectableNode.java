/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.function.Consumer;
import jakarta.persistence.criteria.Selection;

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;

/**
 * Defines a SQM AST node that can be used as a selection in the query,
 * or as an argument to a dynamic-instantiation.
 *
 * @author Steve Ebersole
 */
public interface SqmSelectableNode<T> extends JpaSelection<T>, SqmTypedNode<T>, SqmVisitableNode {
	/**
	 * Visit each of this selectable's direct sub-selectables - used to
	 * support JPA's {@link Selection} model (which is really a "selectable",
	 * just poorly named and poorly defined
	 *
	 * @see JpaSelection#getSelectionItems()
	 * @see Selection#getCompoundSelectionItems()
	 */
	void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> jpaSelectionConsumer);
}
