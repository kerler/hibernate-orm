/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.collection.spi.PersistentIdentifierBag;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Initializer for both {@link PersistentBag} and {@link PersistentIdentifierBag}
 * collections
 *
 * @author Steve Ebersole
 */
public class BagInitializer extends AbstractImmediateCollectionInitializer {
	private static final String CONCRETE_NAME = BagInitializer.class.getSimpleName();

	private final DomainResultAssembler<?> elementAssembler;
	private final DomainResultAssembler<?> collectionIdAssembler;

	public BagInitializer(
			PluralAttributeMapping bagDescriptor,
			FetchParentAccess parentAccess,
			NavigablePath navigablePath,
			LockMode lockMode,
			DomainResultAssembler<?> collectionKeyAssembler,
			DomainResultAssembler<?> collectionValueKeyAssembler,
			DomainResultAssembler<?> elementAssembler,
			DomainResultAssembler<?> collectionIdAssembler) {
		super(
				navigablePath,
				bagDescriptor,
				parentAccess,
				lockMode,
				collectionKeyAssembler,
				collectionValueKeyAssembler
		);
		this.elementAssembler = elementAssembler;
		this.collectionIdAssembler = collectionIdAssembler;
	}

	@Override
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List<Object> loadingState,
			RowProcessingState rowProcessingState) {
		if ( collectionIdAssembler != null ) {
			final Object[] row = new Object[2];
			row[0] = collectionIdAssembler.assemble( rowProcessingState );
			row[1] = elementAssembler.assemble( rowProcessingState );

			loadingState.add( row );
		}
		else {
			loadingState.add( elementAssembler.assemble( rowProcessingState ) );
		}
	}

	@Override
	public String toString() {
		return "BagInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
