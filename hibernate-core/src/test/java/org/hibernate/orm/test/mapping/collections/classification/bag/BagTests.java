/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections.classification.bag;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.CollectionClassification;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = {
		EntityWithBagAsCollection.class
} )
public class BagTests {
	@Test
	public void verifyCollectionAsBag(DomainModelScope scope) {
		scope.withHierarchy( EntityWithBagAsCollection.class, (entityDescriptor) -> {
			final Property names = entityDescriptor.getProperty( "names" );
			final Collection namesMapping = (Collection) names.getValue();

			assertThat( namesMapping.getCollectionSemantics().getCollectionClassification() ).isEqualTo( CollectionClassification.BAG );
		} );
	}
}
