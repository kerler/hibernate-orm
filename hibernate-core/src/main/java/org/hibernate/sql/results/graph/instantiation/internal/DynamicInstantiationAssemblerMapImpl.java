/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.results.graph.instantiation.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A QueryResultAssembler implementation representing handling for dynamic-
 * instantiations targeting a Map, e.g.`select new map( pers.name, pers.dateOfBirth ) ...`
 *
 * @author Steve Ebersole
 */
public class DynamicInstantiationAssemblerMapImpl implements DomainResultAssembler<Map<?,?>> {
	private final JavaType<Map<?,?>> mapJavaDescriptor;
	private final List<ArgumentReader<?>> argumentReaders;

	public DynamicInstantiationAssemblerMapImpl(
			JavaType<Map<?,?>> mapJavaDescriptor,
			List<ArgumentReader<?>> argumentReaders) {
		this.mapJavaDescriptor = mapJavaDescriptor;
		this.argumentReaders = argumentReaders;

		final Set<String> aliases = new HashSet<>();
		for ( ArgumentReader<?> argumentReader : argumentReaders ) {
			if ( argumentReader.getAlias() == null ) {
				throw new IllegalStateException( "alias for Map dynamic instantiation argument cannot be null" );
			}

			if ( ! aliases.add( argumentReader.getAlias() ) ) {
				throw new IllegalStateException( "Encountered duplicate alias for Map dynamic instantiation argument [" + argumentReader.getAlias() + "]" );
			}
		}

	}

	@Override
	public JavaType<Map<?,?>> getAssembledJavaTypeDescriptor() {
		return mapJavaDescriptor;
	}

	@Override
	public Map<?,?> assemble(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		final HashMap<String,Object> result = new HashMap<>();

		for ( ArgumentReader<?> argumentReader : argumentReaders ) {
			result.put(
					argumentReader.getAlias(),
					argumentReader.assemble( rowProcessingState, options )
			);
		}

		return result;
	}
}
