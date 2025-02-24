/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.Member;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.internal.AttributeFactory;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.internal.PluralAttributeMetadata;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.metamodel.internal.AttributeFactory.determineSimpleType;

/**
 * A "parameter object" for creating a plural attribute
 */
public class PluralAttributeBuilder<D, C, E, K> {
	private final JavaType<C> collectionJtd;

	private final AttributeClassification attributeClassification;
	private final CollectionClassification collectionClassification;

	private final SimpleDomainType<E> elementType;
	private final SimpleDomainType<K> listIndexOrMapKeyType;

	private final ManagedDomainType<D> declaringType;

	private final Property property;
	private final Member member;

	public PluralAttributeBuilder(
			JavaType<C> collectionJtd,
			AttributeClassification attributeClassification,
			CollectionClassification collectionClassification,
			SimpleDomainType<E> elementType,
			SimpleDomainType<K> listIndexOrMapKeyType,
			ManagedDomainType<D> declaringType,
			Property property,
			Member member) {
		this.collectionJtd = collectionJtd;
		this.attributeClassification = attributeClassification;
		this.collectionClassification = collectionClassification;
		this.elementType = elementType;
		this.listIndexOrMapKeyType = listIndexOrMapKeyType;
		this.declaringType = declaringType;
		this.property = property;
		this.member = member;
	}

	public static <Y, X> PersistentAttribute<X, Y> build(
			PluralAttributeMetadata<?,Y,?> attributeMetadata,
			MetadataContext metadataContext) {

		final JavaType<Y> attributeJtd = metadataContext.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( attributeMetadata.getJavaType() );

		//noinspection unchecked
		final PluralAttributeBuilder builder = new PluralAttributeBuilder(
				attributeJtd,
				attributeMetadata.getAttributeClassification(),
				attributeMetadata.getCollectionClassification(),
				AttributeFactory.determineSimpleType(
						attributeMetadata.getElementValueContext(),
						metadataContext
				),
				determineListIndexOrMapKeyType( attributeMetadata, metadataContext ),
				attributeMetadata.getOwnerType(),
				attributeMetadata.getPropertyMapping(),
				attributeMetadata.getMember()
		);

		if ( Map.class.equals( attributeJtd.getJavaTypeClass() ) ) {
			//noinspection unchecked
			return new MapAttributeImpl<>( builder, metadataContext );
		}
		else if ( Set.class.equals( attributeJtd.getJavaTypeClass() ) ) {
			//noinspection unchecked
			return new SetAttributeImpl<>( builder, metadataContext );
		}
		else if ( List.class.equals( attributeJtd.getJavaTypeClass() ) ) {
			//noinspection unchecked
			return new ListAttributeImpl<>( builder, metadataContext );
		}
		else if ( Collection.class.equals( attributeJtd.getJavaTypeClass() ) ) {
			//noinspection unchecked
			return new BagAttributeImpl<>( builder, metadataContext );
		}

		//apply loose rules
		if ( attributeJtd.getJavaTypeClass().isArray() ) {
			//noinspection unchecked
			return new ListAttributeImpl<>( builder, metadataContext );
		}

		if ( Map.class.isAssignableFrom( attributeJtd.getJavaTypeClass() ) ) {
			//noinspection unchecked
			return new MapAttributeImpl<>( builder, metadataContext );
		}
		else if ( Set.class.isAssignableFrom( attributeJtd.getJavaTypeClass() ) ) {
			//noinspection unchecked
			return new SetAttributeImpl<>( builder, metadataContext );
		}
		else if ( List.class.isAssignableFrom( attributeJtd.getJavaTypeClass() ) ) {
			//noinspection unchecked
			return new ListAttributeImpl<>( builder, metadataContext );
		}
		else if ( Collection.class.isAssignableFrom( attributeJtd.getJavaTypeClass() ) ) {
			//noinspection unchecked
			return new BagAttributeImpl<>( builder, metadataContext );
		}

		throw new UnsupportedMappingException( "Unknown collection: " + attributeJtd.getJavaType() );
	}

	private static SimpleDomainType<?> determineListIndexOrMapKeyType(
			PluralAttributeMetadata<?,?,?> attributeMetadata,
			MetadataContext metadataContext) {
		if ( Map.class.isAssignableFrom( attributeMetadata.getJavaType() ) ) {
			return determineSimpleType( attributeMetadata.getMapKeyValueContext(), metadataContext );
		}

		if ( List.class.isAssignableFrom( attributeMetadata.getJavaType() )
				|| attributeMetadata.getJavaType().isArray() ) {
			return metadataContext.getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( Integer.class );
		}

		return null;
	}

	public ManagedDomainType<D> getDeclaringType() {
		return declaringType;
	}

	public AttributeClassification getAttributeClassification() {
		return attributeClassification;
	}

	public CollectionClassification getCollectionClassification() {
		return collectionClassification;
	}

	public SimpleDomainType<K> getListIndexOrMapKeyType() {
		return listIndexOrMapKeyType;
	}

	public JavaType<C> getCollectionJavaTypeDescriptor() {
		return collectionJtd;
	}

	public SimpleDomainType<E> getValueType() {
		return elementType;
	}

	public Property getProperty() {
		return property;
	}

	public Member getMember() {
		return member;
	}
}
