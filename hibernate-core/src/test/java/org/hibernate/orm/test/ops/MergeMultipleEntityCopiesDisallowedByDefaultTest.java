/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ops;

import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests merging multiple detached representations of the same entity using
 * a the default (that does not allow this).
 *
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-9106")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/ops/Hoarder.hbm.xml"
)
@SessionFactory
public class MergeMultipleEntityCopiesDisallowedByDefaultTest {

	@Test
	public void testCascadeFromDetachedToNonDirtyRepresentations(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1" );

		Hoarder hoarder = new Hoarder();
		hoarder.setName( "joe" );

		scope.inTransaction(
				session -> {
					session.persist( item1 );
					session.persist( hoarder );
				}
		);

		// Get another representation of the same Item from a different session.
		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertNotSame( item1, item1_1 );
		assertEquals( item1, item1_1 );

		// Update hoarder (detached) to references both representations.
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );

		scope.inTransaction(
				session -> {
					try {
						session.merge( hoarder );
						fail( "should have failed due IllegalStateException" );
					}
					catch (IllegalStateException ex) {
						//expected
					}
				}
		);

		cleanup( scope );
	}

	@Test
	public void testTopLevelManyToOneManagedNestedIsDetached(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );
		item1.setCategory( category );
		category.setExampleItem( item1 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );

					item1Merged.setCategory( category );
					category.setExampleItem( item1_1 );

					// now item1Merged is managed and it has a nested detached item
					try {
						session.merge( item1Merged );
						fail( "should have failed due IllegalStateException" );
					}
					catch (IllegalStateException ex) {
						//expected
					}
				}
		);

		cleanup( scope );
	}

	@SuppressWarnings({ "unchecked" })
	private void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from SubItem" ).executeUpdate();
					for ( Hoarder hoarder : (List<Hoarder>) session.createQuery( "from Hoarder" ).list() ) {
						hoarder.getItems().clear();
						session.delete( hoarder );
					}

					for ( Category category : (List<Category>) session.createQuery( "from Category" ).list() ) {
						if ( category.getExampleItem() != null ) {
							category.setExampleItem( null );
							session.delete( category );
						}
					}

					for ( Item item : (List<Item>) session.createQuery( "from Item" ).list() ) {
						item.setCategory( null );
						session.delete( item );
					}

					session.createQuery( "delete from Item" ).executeUpdate();
				}
		);
	}
}
