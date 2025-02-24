/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.metamodel.model.domain.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class MappedSuperclassTypeImpl<X> extends AbstractIdentifiableType<X> implements MappedSuperclassDomainType<X> {
	public MappedSuperclassTypeImpl(
			JavaType<X> javaTypeDescriptor,
			MappedSuperclass mappedSuperclass,
			IdentifiableDomainType<? super X> superType,
			JpaMetamodel jpaMetamodel) {
		super(
				javaTypeDescriptor.getJavaType().getTypeName(),
				javaTypeDescriptor,
				superType,
				mappedSuperclass.getDeclaredIdentifierMapper() != null || ( superType != null && superType.hasIdClass() ),
				mappedSuperclass.hasIdentifierProperty(),
				mappedSuperclass.isVersioned(),
				jpaMetamodel
		);
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.MAPPED_SUPERCLASS;
	}

	@Override
	public <S extends X> SubGraphImplementor<S> makeSubGraph(Class<S> subType) {
		throw new NotYetImplementedException(  );
	}

	@Override
	protected boolean isIdMappingRequired() {
		return false;
	}
}
