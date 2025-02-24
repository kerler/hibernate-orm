/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.notfound;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-14537")
@DomainModel(
		annotatedClasses = {
				EagerProxyNotFoundTest.Task.class,
				EagerProxyNotFoundTest.Employee.class,
				EagerProxyNotFoundTest.Location.class
		}
)
@SessionFactory
public class EagerProxyNotFoundTest {

	@Test
	public void testNoProxyInSession(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Task task = new Task();
					task.id = 1;
					task.employeeEagerNotFoundIgnore = session.load( Employee.class, 2 );
					session.persist( task );
				} );

		scope.inTransaction(
				session -> {
					final Task task = session.createQuery( "from Task", Task.class ).getSingleResult();
					assertNotNull( task );
					assertNull( task.employeeEagerNotFoundIgnore );
				} );
	}

	@Test
	public void testNonExistingProxyInSession(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Task task = new Task();
					task.id = 1;
					task.employeeEagerNotFoundIgnore = session.load( Employee.class, 2 );
					session.persist( task );
				} );

		scope.inTransaction(
				session -> {
					session.load( Employee.class, 2 );
					final Task task = session.createQuery( "from Task", Task.class ).getSingleResult();
					assertNotNull( task );
					assertNull( task.employeeEagerNotFoundIgnore );
				} );
	}

	@Test
	public void testEagerIgnoreLazyProxy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Task task = new Task();
					task.id = 1;
					task.employeeLazy = session.load( Employee.class, 2 );
					task.employeeEagerNotFoundIgnore = task.employeeLazy;
					session.persist( task );
				} );

		scope.inTransaction(
				session -> {
					final Task task = session.createQuery( "from Task", Task.class ).getSingleResult();
					assertNotNull( task );
					assertNull( task.employeeEagerNotFoundIgnore );
					assertNotNull( task.employeeLazy );
					assertTrue( HibernateProxy.class.isInstance( task.employeeLazy ) );
					assertEquals( 2, task.employeeLazy.getId() );
				} );
	}

	@Test
	public void testProxyInSessionEagerIgnoreLazyProxy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Task task = new Task();
					task.id = 1;
					task.employeeLazy = session.load( Employee.class, 2 );
					task.employeeEagerNotFoundIgnore = task.employeeLazy;
					session.persist( task );
				} );

		scope.inTransaction(
				session -> {
					final Employee employeeProxy = session.load( Employee.class, 2 );
					final Task task = session.createQuery( "from Task", Task.class ).getSingleResult();
					assertNotNull( task );
					assertNull( task.employeeEagerNotFoundIgnore );
					assertNotNull( task.employeeLazy );
					assertTrue( HibernateProxy.class.isInstance( task.employeeLazy ) );
					assertEquals( 2, task.employeeLazy.getId() );
					assertSame( employeeProxy, task.employeeLazy );
				} );
	}

	@Test
	public void testExistingProxyWithNonExistingAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Employee employee = new Employee();
					employee.id = 1;
					session.persist( employee );

					final Task task = new Task();
					task.id = 2;
					task.employeeEagerNotFoundIgnore = employee;
					session.persist( task );

					session.flush();

					session.createNativeQuery( "update Employee set locationId = 3 where id = 1" )
							.executeUpdate();
				} );

		try {
			scope.inTransaction(
					session -> {
						session.load( Employee.class, 1 );
						session.createQuery( "from Task", Task.class ).getSingleResult();
					} );
			fail( "EntityNotFoundException should have been thrown because Task.employee.location is not found " +
						  "and is not mapped with @NotFound(IGNORE)" );
		}
		catch (EntityNotFoundException expected) {
		}
	}

	@Test
	public void testEnityWithNotExistingAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Employee employee = new Employee();
					employee.id = 1;
					session.persist( employee );

					final Task task = new Task();
					task.id = 2;
					task.employeeEagerNotFoundIgnore = employee;
					session.persist( task );

					session.flush();

					session.createNativeQuery( "update Employee set locationId = 3 where id = 1" )
							.executeUpdate();
				} );

		try {
			scope.inTransaction(
					session -> {
						session.createQuery( "from Employee", Employee.class ).getSingleResult();
					} );
			fail( "EntityNotFoundException should have been thrown because Task.employee.location is not found " +
						  "and is not mapped with @NotFound(IGNORE)" );
		}
		catch (EntityNotFoundException expected) {
		}
	}

	@AfterEach
	public void deleteData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Task" ).executeUpdate();
					session.createQuery( "delete from Employee" ).executeUpdate();
					session.createQuery( "delete from Location" ).executeUpdate();
				} );
	}

	@Entity(name = "Task")
	public static class Task {

		@Id
		private int id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(
				name = "employeeId",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@NotFound(action = NotFoundAction.IGNORE)
		private Employee employeeEagerNotFoundIgnore;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(
				name = "lazyEmployeeId",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private Employee employeeLazy;
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private int id;

		private String name;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "locationId", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		private Location location;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
	}

	@Entity(name = "Location")
	public static class Location {
		@Id
		private int id;

		private String description;
	}
}
