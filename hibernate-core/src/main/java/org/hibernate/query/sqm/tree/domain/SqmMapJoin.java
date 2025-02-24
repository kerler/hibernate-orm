/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Map;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaMapJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmMapJoin<O, K, V>
		extends AbstractSqmPluralJoin<O, Map<K, V>, V>
		implements JpaMapJoin<O, K, V> {
	public SqmMapJoin(
			SqmFrom<?,O> lhs,
			MapPersistentAttribute<O, K, V> pluralValuedNavigable,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, pluralValuedNavigable, alias, sqmJoinType, fetched, nodeBuilder );
	}

	protected SqmMapJoin(
			SqmFrom<?, O> lhs,
			NavigablePath navigablePath,
			MapPersistentAttribute<O, K, V> pluralValuedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, pluralValuedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public MapPersistentAttribute<O, K, V> getReferencedPathSource() {
		return(MapPersistentAttribute<O, K, V>) super.getReferencedPathSource();
	}

	@Override
	public MapPersistentAttribute<O, K, V> getModel() {
		return (MapPersistentAttribute<O, K, V>) super.getModel();
	}

	@Override
	public MapPersistentAttribute<O, K, V> getAttribute() {
		//noinspection unchecked
		return (MapPersistentAttribute<O, K, V>) super.getAttribute();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmPath<K> key() {
		final SqmPathSource<K> keyPathSource = getReferencedPathSource().getKeyPathSource();
		return resolvePath( keyPathSource.getPathName(), keyPathSource );
	}

	@Override
	public Path<V> value() {
		final SqmPathSource<V> elementPathSource = getReferencedPathSource().getElementPathSource();
		return resolvePath( elementPathSource.getPathName(), elementPathSource );
	}

	@Override
	public Expression<Map.Entry<K, V>> entry() {
		return new SqmMapEntryReference<>( this, nodeBuilder() );
	}

	@Override
	public SqmMapJoin<O, K, V> on(JpaExpression<Boolean> restriction) {
		return (SqmMapJoin<O, K, V>) super.on( restriction );
	}

	@Override
	public SqmMapJoin<O, K, V> on(Expression<Boolean> restriction) {
		return (SqmMapJoin<O, K, V>) super.on( restriction );
	}

	@Override
	public SqmMapJoin<O, K, V> on(JpaPredicate... restrictions) {
		return (SqmMapJoin<O, K, V>) super.on( restrictions );
	}

	@Override
	public SqmMapJoin<O, K, V> on(Predicate... restrictions) {
		return (SqmMapJoin<O, K, V>) super.on( restrictions );
	}

	@Override
	public SqmCorrelatedMapJoin<O, K, V> createCorrelation() {
		return new SqmCorrelatedMapJoin<>( this );
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<O, K, V, S> treatAs(Class<S> treatJavaType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<O, K, V, S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<O, K, V, S> treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ), alias );
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<O, K, V, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		final SqmTreatedMapJoin<O, K, V, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedMapJoin<>( this, treatTarget, alias ) );
		}
		return treat;
	}

	@Override
	public SqmMapJoin<O, K, V> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmMapJoin<>(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}
