/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.procedure;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.procedure.internal.NamedCallableQueryMementoImpl;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.IntegerJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.LongJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;

import org.junit.Test;

import jakarta.persistence.EntityManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public abstract class AbstractStoredProcedureTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testNamedStoredProcedureBinding() {
		EntityManager em = getOrCreateEntityManager();
		SessionFactoryImplementor sf = em.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		final NamedCallableQueryMementoImpl m1 = (NamedCallableQueryMementoImpl) sf.getQueryEngine()
				.getNamedObjectRepository()
				.getCallableQueryMemento( "s1" );
		assertNotNull( m1 );
		assertEquals( "p1", m1.getCallableName() );
		assertEquals( ParameterStrategy.NAMED, m1.getParameterStrategy() );
		List<NamedCallableQueryMemento.ParameterMemento> list = m1.getParameterMementos();
		assertEquals( 2, list.size() );
		NamedCallableQueryMemento.ParameterMemento memento = list.get( 0 );
		ProcedureParameterImplementor parameterImplementor = memento.resolve( (SharedSessionContractImplementor) em );
		assertEquals( "p11", parameterImplementor.getName() );
		assertEquals( jakarta.persistence.ParameterMode.IN, parameterImplementor.getMode() );
		BasicType hibernateType = (BasicType) parameterImplementor.getHibernateType();
		assertEquals( IntegerJavaTypeDescriptor.INSTANCE, hibernateType.getJavaTypeDescriptor() );
		assertEquals( Integer.class, parameterImplementor.getParameterType() );

		memento = list.get( 1 );
		parameterImplementor = memento.resolve( (SharedSessionContractImplementor) em );
		assertEquals( "p12", parameterImplementor.getName() );
		assertEquals( jakarta.persistence.ParameterMode.IN, parameterImplementor.getMode() );
		hibernateType = (BasicType) parameterImplementor.getHibernateType();
		assertEquals( IntegerJavaTypeDescriptor.INSTANCE, hibernateType.getJavaTypeDescriptor() );
		assertEquals( Integer.class, parameterImplementor.getParameterType() );


		final NamedCallableQueryMementoImpl m2 = (NamedCallableQueryMementoImpl) sf.getQueryEngine()
				.getNamedObjectRepository()
				.getCallableQueryMemento( "s2" );
		assertNotNull( m2 );
		assertEquals( "p2", m2.getCallableName() );
		assertEquals( ParameterStrategy.POSITIONAL, m2.getParameterStrategy() );
		list = m2.getParameterMementos();

		memento = list.get( 0 );
		parameterImplementor = memento.resolve( (SharedSessionContractImplementor) em );
		assertEquals( Integer.valueOf( 1 ), parameterImplementor.getPosition() );
		assertEquals( jakarta.persistence.ParameterMode.INOUT, parameterImplementor.getMode() );
		hibernateType = (BasicType) parameterImplementor.getHibernateType();

		assertEquals( StringJavaTypeDescriptor.INSTANCE, hibernateType.getJavaTypeDescriptor() );
		assertEquals( String.class, parameterImplementor.getParameterType() );

		memento = list.get( 1 );
		parameterImplementor = memento.resolve( (SharedSessionContractImplementor) em );

		assertEquals( Integer.valueOf( 2 ), parameterImplementor.getPosition() );
		assertEquals( jakarta.persistence.ParameterMode.INOUT, parameterImplementor.getMode() );
		hibernateType = (BasicType) parameterImplementor.getHibernateType();

		assertEquals( LongJavaTypeDescriptor.INSTANCE, hibernateType.getJavaTypeDescriptor() );
		assertEquals( Long.class, parameterImplementor.getParameterType() );

	}
}
