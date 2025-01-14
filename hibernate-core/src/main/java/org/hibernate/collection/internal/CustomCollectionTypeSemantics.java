/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.InitializerProducerBuilder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.type.CollectionType;

/**
 * A collection semantics wrapper for <code>CollectionType</code>.
 *
 * @author Christian Beikov
 */
public class CustomCollectionTypeSemantics<CE, E> implements CollectionSemantics<CE, E> {
	private final CollectionType collectionType;
	private final CollectionClassification classification;

	public CustomCollectionTypeSemantics(CollectionType collectionType, CollectionClassification classification) {
		this.collectionType = collectionType;
		this.classification = classification;
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return classification;
	}

	@Override
	public Class<?> getCollectionJavaType() {
		return collectionType.getReturnedClass();
	}

	@Override
	public CE instantiateRaw(int anticipatedSize, CollectionPersister collectionDescriptor) {
		//noinspection unchecked
		return (CE) collectionType.instantiate( anticipatedSize );
	}

	@Override
	public Iterator<E> getElementIterator(CE rawCollection) {
		//noinspection unchecked
		return collectionType.getElementsIterator( rawCollection, null );
	}

	@Override
	public void visitElements(CE rawCollection, Consumer<? super E> action) {
		getElementIterator( rawCollection ).forEachRemaining( action );
	}

	@Override
	public CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		return InitializerProducerBuilder.createCollectionTypeWrapperInitializerProducer(
				navigablePath,
				attributeMapping,
				classification,
				fetchParent,
				selected,
				indexFetch,
				elementFetch,
				creationState
		);
	}

	@Override
	public PersistentCollection<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		//noinspection unchecked
		return collectionType.instantiate( session, collectionDescriptor, key );
	}

	@Override
	public PersistentCollection<E> wrap(
			CE rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		//noinspection unchecked
		return collectionType.wrap( session, rawCollection );
	}
}
