/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter.custom;

import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * @author Steve Ebersole
 */
public class PayloadWrapperJavaType implements BasicJavaType<PayloadWrapper> {
	/**
	 * Singleton access
	 */
	public static final PayloadWrapperJavaType INSTANCE = new PayloadWrapperJavaType();

	private final MutableMutabilityPlan<PayloadWrapper> mutabilityPlan = new MutableMutabilityPlan<PayloadWrapper>() {
		@Override
		protected PayloadWrapper deepCopyNotNull(PayloadWrapper value) {
			return value == null ? null : new PayloadWrapper( value.getPayload() );
		}
	};

	private PayloadWrapperJavaType() {
	}

	@Override
	public Class getJavaTypeClass() {
		return PayloadWrapper.class;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		return PayloadWrapperJdbcType.INSTANCE;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public PayloadWrapper fromString(CharSequence string) {
		return CharSequenceHelper.isEmpty( string ) ? null : new PayloadWrapper( string.toString() );
	}

	@Override
	public PayloadWrapper wrap(Object value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if ( String.class.isInstance( value ) ) {
			return new PayloadWrapper( (String) value );
		}

		throw new UnsupportedOperationException( "Wrapping value as PayloadWrapperJavaType only supported for String or MyCustomJdbcType : " + value );
	}

	@Override
	public Object unwrap(PayloadWrapper value, Class type, WrapperOptions options) {
		if ( String.class.isAssignableFrom( type ) ) {
			return value.getPayload();
		}

		throw new UnsupportedOperationException( "Unwrapping PayloadWrapperJavaType value only supported for String or MyCustomJdbcType : " + value );
	}
}
