/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hqlfetchscroll;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.transform.DistinctRootEntityResultTransformer;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/hqlfetchscroll/ParentChild.hbm.xml"
)
@SessionFactory
public class HQLScrollFetchTest {
	private static final String QUERY = "select p from Parent p join fetch p.children c";

	@Test
	public void testNoScroll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List list = session.createQuery( QUERY )
							.setResultTransformer( DistinctRootEntityResultTransformer.INSTANCE )
							.list();
					assertResultFromAllUsers( list );
				}
		);
	}

	@Test
	public void testScroll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc, c.name asc" )
							.scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertResultFromAllUsers( list );
					}
				}
		);
	}

	@Test
	public void testIncompleteScrollFirstResult(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc" ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1283")
	public void testIncompleteScrollSecondResult(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc" ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
						results.next();
						p = (Parent) results.get();
						assertResultFromOneUser( p );
					}
				}
		);
	}

	@Test
	public void testIncompleteScrollFirstResultInTransaction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc" ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1283")
	public void testIncompleteScrollSecondResultInTransaction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc" ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
						results.next();
						p = (Parent) results.get();
						assertResultFromOneUser( p );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1283")
	public void testIncompleteScroll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc" ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
						// get the other parent entity from the persistence context along with its first child
						// retrieved from the resultset.
						Parent pOther = null;
						Child cOther = null;
						for ( Object entity : session.getPersistenceContext().getEntitiesByKey().values() ) {
							if ( Parent.class.isInstance( entity ) ) {
								if ( entity != p ) {
									if ( pOther != null ) {
										fail( "unexpected parent found." );
									}
									pOther = (Parent) entity;
								}
							}
							else if ( Child.class.isInstance( entity ) ) {
								if ( !p.getChildren().contains( entity ) ) {
									if ( cOther != null ) {
										fail( "unexpected child entity found" );
									}
									cOther = (Child) entity;
								}
							}
							else {
								fail( "unexpected type of entity." );
							}
						}
						// check that the same second parent is obtained by calling Session.get()
						assertNull( pOther );
						assertNull( cOther );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1283")
	public void testIncompleteScrollLast(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc" ).scroll()) {
						results.next();
						Parent p = (Parent) results.get();
						assertResultFromOneUser( p );
						results.last();
						// get the other parent entity from the persistence context.
						// since the result set was scrolled to the end, the other parent entity's collection has been
						// properly initialized.
						Parent pOther = null;
						Set childrenOther = new HashSet();
						for ( Object entity : session.getPersistenceContext().getEntitiesByKey().values() ) {
							if ( Parent.class.isInstance( entity ) ) {
								if ( entity != p ) {
									if ( pOther != null ) {
										fail( "unexpected parent found." );
									}
									pOther = (Parent) entity;
								}
							}
							else if ( Child.class.isInstance( entity ) ) {
								if ( !p.getChildren().contains( entity ) ) {
									childrenOther.add( entity );
								}
							}
							else {
								fail( "unexpected type of entity." );
							}
						}
						// check that the same second parent is obtained by calling Session.get()
						assertNotNull( pOther );
						assertSame( pOther, session.get( Parent.class, pOther.getId() ) );
						// access pOther's collection; should be completely loaded
						assertTrue( Hibernate.isInitialized( pOther.getChildren() ) );
						assertEquals( childrenOther, pOther.getChildren() );
						assertResultFromOneUser( pOther );
					}

				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1283")
	public void testScrollOrderParentAsc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc" ).scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertResultFromAllUsers( list );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1283")
	public void testScrollOrderParentDesc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name desc" ).scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertResultFromAllUsers( list );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1283")
	public void testScrollOrderParentAscChildrenAsc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc, c.name asc" )
							.scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertResultFromAllUsers( list );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1283")
	public void testScrollOrderParentAscChildrenDesc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by p.name asc, c.name desc" )
							.scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertResultFromAllUsers( list );
					}
				}
		);
	}

	final String errMsg = "should have failed because data is ordered incorrectly.";

	@Test
	public void testScrollOrderChildrenDesc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p0 = new Parent( "parent0" );
					session.save( p0 );
				}
		);

		scope.inSession(
				session -> {
					try (ScrollableResults results = session.createQuery( QUERY + " order by c.name desc" ).scroll()) {
						List list = new ArrayList();
						while ( results.next() ) {
							list.add( results.get() );
						}

						try {
							assertResultFromAllUsers( list );
							fail( errMsg );
						}
						catch (AssertionError ex) {
							if ( errMsg.equalsIgnoreCase( ex.getMessage() ) ) {
								throw ex;
							}
							// Other AssertionErrors expected
						}
					}
				}
		);
	}

	@Test
	public void testListOrderChildrenDesc(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p0 = new Parent( "parent0" );
					session.save( p0 );
				}
		);

		scope.inSession(
				session -> {
					List results = session.createQuery( QUERY + " order by c.name desc" ).list();
//					try {
						assertResultFromAllUsers( results );
//						fail( errMsg );
//					}
//					catch (AssertionError ex) {
//						if ( errMsg.equalsIgnoreCase( ex.getMessage() ) ) {
//							throw ex;
//						}
//						// Other AssertionErrors expected
//					}
				}
		);
	}

	private void assertResultFromOneUser(Parent parent) {
		assertEquals(
				3,
				parent.getChildren().size(),
				"parent " + parent + " has incorrect collection(" + parent.getChildren() + ")."
		);
	}

	private void assertResultFromAllUsers(List list) {
		assertEquals( 2, list.size(), "list is not correct size: " );
		for ( Object aList : list ) {
			assertResultFromOneUser( (Parent) aList );
		}
	}

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child_1_1 = new Child( "achild1-1" );
					Child child_1_2 = new Child( "ychild1-2" );
					Child child_1_3 = new Child( "dchild1-3" );
					Child child_2_1 = new Child( "bchild2-1" );
					Child child_2_2 = new Child( "cchild2-2" );
					Child child_2_3 = new Child( "zchild2-3" );

					session.save( child_1_1 );
					session.save( child_2_1 );
					session.save( child_1_2 );
					session.save( child_2_2 );
					session.save( child_1_3 );
					session.save( child_2_3 );

					session.flush();

					Parent p1 = new Parent( "parent1" );
					p1.addChild( child_1_1 );
					p1.addChild( child_1_2 );
					p1.addChild( child_1_3 );
					session.save( p1 );

					Parent p2 = new Parent( "parent2" );
					p2.addChild( child_2_1 );
					p2.addChild( child_2_2 );
					p2.addChild( child_2_3 );
					session.save( p2 );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List list = session.createQuery( "from Parent" ).list();
					for ( Iterator i = list.iterator(); i.hasNext(); ) {
						session.delete( i.next() );
					}
				}
		);
	}

}
