/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections.semantics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

/**
 * @author Steve Ebersole
 */
//tag::collections-custom-type-ex[]
public class UniqueListType implements UserCollectionType {
	@Override
	public PersistentCollection instantiate(
			SharedSessionContractImplementor session,
			CollectionPersister persister) {
		return new UniqueListWrapper( session );
	}

	@Override
	public PersistentCollection wrap(
			SharedSessionContractImplementor session,
			Object collection) {
		return new UniqueListWrapper( session, (List) collection );
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return ( (List) collection ).iterator();
	}

	@Override
	public boolean contains(Object collection, Object entity) {
		return ( (List) collection ).contains( entity );
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		return ( (List) collection ).indexOf( entity );
	}

	@Override
	public Object replaceElements(
			Object original,
			Object target,
			CollectionPersister persister,
			Object owner,
			Map copyCache,
			SharedSessionContractImplementor session) {
		List result = (List) target;
		result.clear();
		result.addAll( (List) original );
		return result;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return new ArrayList<>();
	}
}
//end::collections-custom-type-ex[]
