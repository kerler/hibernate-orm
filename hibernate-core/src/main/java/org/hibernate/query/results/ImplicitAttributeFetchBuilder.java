/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.function.BiFunction;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.implicit.ImplicitFetchBuilder;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * FetchBuilder used when an explicit mapping was not given
 *
 * @author Steve Ebersole
 */
public class ImplicitAttributeFetchBuilder implements FetchBuilder, ImplicitFetchBuilder {
	private final NavigablePath navigablePath;
	private final AttributeMapping attributeMapping;

	public ImplicitAttributeFetchBuilder(NavigablePath navigablePath, AttributeMapping attributeMapping) {
		this.navigablePath = navigablePath;
		this.attributeMapping = attributeMapping;
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		assert fetchPath.equals( navigablePath );

		return parent.generateFetchableFetch(
				attributeMapping,
				fetchPath,
				FetchTiming.IMMEDIATE,
				true,
				null,
				domainResultCreationState
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ImplicitAttributeFetchBuilder that = (ImplicitAttributeFetchBuilder) o;
		return navigablePath.equals( that.navigablePath )
				&& attributeMapping.equals( that.attributeMapping );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + attributeMapping.hashCode();
		return result;
	}
}
