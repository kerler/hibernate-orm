/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.h2gis;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.KeyedSqmFunctionDescriptors;
import org.hibernate.spatial.contributor.ContributorImplementor;

public class H2GisDialectContributor implements ContributorImplementor {

	private final ServiceRegistry serviceRegistry;

	public H2GisDialectContributor(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void contributeJdbcTypes(TypeContributions typeContributions) {
		HSMessageLogger.LOGGER.typeContributions( this.getClass().getCanonicalName() );
		typeContributions.contributeJdbcTypeDescriptor( H2GISGeometryType.INSTANCE );
	}

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		HSMessageLogger.LOGGER.functionContributions( this.getClass().getCanonicalName() );
		final KeyedSqmFunctionDescriptors functions = new H2SqmFunctionDescriptors( functionContributions );
		final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		functions.asMap().forEach( (key, desc) -> {
			functionRegistry.register( key.getName(), desc );
			key.getAltName().ifPresent( altName -> functionRegistry.registerAlternateKey( altName, key.getName() ) );
		} );
	}


	@Override
	public ServiceRegistry getServiceRegistry() {
		return this.serviceRegistry;
	}
}
