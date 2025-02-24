/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EmbeddableAssembler implements DomainResultAssembler {
	protected final EmbeddableInitializer initializer;

	public EmbeddableAssembler(EmbeddableInitializer initializer) {
		this.initializer = initializer;
	}

	@Override
	public JavaType getAssembledJavaTypeDescriptor() {
		return initializer.getInitializedPart().getJavaTypeDescriptor();
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		initializer.resolveKey( rowProcessingState );
		initializer.resolveInstance( rowProcessingState );
		initializer.initializeInstance( rowProcessingState );
		return initializer.getCompositeInstance();
	}

}
