/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ops;


import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
public class DeleteTest extends AbstractOperationTestCase {

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testDeleteVersionedWithCollectionNoUpdate(SessionFactoryScope scope) {
		// test adapted from HHH-1564...
		scope.inTransaction(
				session -> {
					VersionedEntity c = new VersionedEntity( "c1", "child-1" );
					VersionedEntity p = new VersionedEntity( "root", "root" );
					p.getChildren().add( c );
					c.setParent( p );
					session.save( p );
				}
		);

		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedEntity loadedParent = session.get( VersionedEntity.class, "root" );
					session.delete( loadedParent );
				}
		);

		assertInsertCount( 0, scope );
		assertUpdateCount( 0, scope );
		assertDeleteCount( 2, scope );
	}

	@Test
	public void testNoUpdateOnDelete(SessionFactoryScope scope) {
		Node node = new Node( "test" );
		scope.inTransaction(
				session ->
						session.persist( node )
		);

		clearCounts( scope );

		scope.inTransaction(
				session ->
						session.delete( node )
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testNoUpdateOnDeleteWithCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Node parent = new Node( "parent" );
					Node child = new Node( "child" );
					parent.getCascadingChildren().add( child );
					session.persist( parent );
				}
		);


		clearCounts( scope );

		scope.inTransaction(
				session -> {
					Node parent = session.get( Node.class, "parent" );
					session.delete( parent );
				}
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );
		assertDeleteCount( 2, scope );
	}
}
