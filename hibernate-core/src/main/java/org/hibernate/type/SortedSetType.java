/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Comparator;
import java.util.TreeSet;

import org.hibernate.collection.spi.PersistentSortedSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.spi.TypeConfiguration;

public class SortedSetType extends SetType {

	private final Comparator comparator;

	public SortedSetType(TypeConfiguration typeConfiguration, String role, String propertyRef, Comparator comparator) {
		super( typeConfiguration, role, propertyRef );
		this.comparator = comparator;
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Object key) {
		return new PersistentSortedSet( session, comparator );
	}

	public Class getReturnedClass() {
		return java.util.SortedSet.class;
	}

	@SuppressWarnings( {"unchecked"})
	public Object instantiate(int anticipatedSize) {
		return new TreeSet(comparator);
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentSortedSet( session, (java.util.SortedSet) collection );
	}
}
