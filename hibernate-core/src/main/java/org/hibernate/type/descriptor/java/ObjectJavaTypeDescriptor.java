/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Steve Ebersole
 */
public class ObjectJavaTypeDescriptor extends AbstractClassJavaTypeDescriptor<Object> {
	/**
	 * Singleton access
	 */
	public static final ObjectJavaTypeDescriptor INSTANCE = new ObjectJavaTypeDescriptor();

	public ObjectJavaTypeDescriptor() {
		super( Object.class );
	}

	@Override
	public <X> X unwrap(Object value, Class<X> type, WrapperOptions options) {
		//noinspection unchecked
		return (X) value;
	}

	@Override
	public <X> Object wrap(X value, WrapperOptions options) {
		return value;
	}
}
