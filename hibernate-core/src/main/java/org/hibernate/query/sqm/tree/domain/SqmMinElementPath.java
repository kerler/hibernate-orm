/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;

/**
 * @author Steve Ebersole
 */
public class SqmMinElementPath<T> extends AbstractSqmSpecificPluralPartPath<T> {
	public static final String NAVIGABLE_NAME = "{min-element}";

	public SqmMinElementPath(SqmPath<?> pluralDomainPath) {
		//noinspection unchecked
		super(
				pluralDomainPath.getNavigablePath().getParent().append( pluralDomainPath.getNavigablePath().getLocalName(), NAVIGABLE_NAME ),
				pluralDomainPath,
				(PluralPersistentAttribute<?, ?, T>) pluralDomainPath.getReferencedPathSource()
		);
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPath<?> sqmPath = get( name );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return getPluralAttribute().getElementPathSource();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitMinElementPath( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "minelement(" );
		getLhs().appendHqlString( sb );
		sb.append( ')' );
	}
}
