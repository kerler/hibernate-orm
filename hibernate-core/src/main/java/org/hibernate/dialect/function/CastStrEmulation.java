/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Gavin King
 */
public class CastStrEmulation
		extends AbstractSqmFunctionDescriptor {

	public CastStrEmulation(TypeConfiguration typeConfiguration) {
		super(
				"str",
				StandardArgumentsValidators.exactly( 1 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
		);
	}

	protected CastStrEmulation(
			String name,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( name, argumentsValidator, returnTypeResolver );
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		final SqmTypedNode<?> argument = arguments.get( 0 );
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "cast" )
				.generateSqmExpression(
						asList(
								argument,
								new SqmCastTarget<>(
										typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ),
										argument.nodeBuilder()
								)
						),
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
	}
}
