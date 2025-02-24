/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.ScrollMode;
import org.hibernate.internal.EmptyScrollableResults;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public class AggregatedSelectQueryPlanImpl<R> implements SelectQueryPlan<R> {
	private final SelectQueryPlan<R>[] aggregatedQueryPlans;

	public AggregatedSelectQueryPlanImpl(SelectQueryPlan<R>[] aggregatedQueryPlans) {
		this.aggregatedQueryPlans = aggregatedQueryPlans;
	}

	@Override
	public List<R> performList(DomainQueryExecutionContext executionContext) {
		final Limit effectiveLimit = executionContext.getQueryOptions().getEffectiveLimit();
		final int maxRowsJpa = effectiveLimit.getMaxRowsJpa();
		if ( maxRowsJpa == 0 ) {
			return Collections.emptyList();
		}
		int elementsToSkip = effectiveLimit.getFirstRowJpa();
		final List<R> overallResults = new ArrayList<>();

		for ( SelectQueryPlan<R> aggregatedQueryPlan : aggregatedQueryPlans ) {
			final List<R> list = aggregatedQueryPlan.performList( executionContext );
			final int size = list.size();
			if ( size <= elementsToSkip ) {
				// More elements to skip than the collection size
				elementsToSkip -= size;
				continue;
			}
			final int availableElements = size - elementsToSkip;
			if ( overallResults.size() + availableElements >= maxRowsJpa ) {
				// This result list is the last one i.e. fulfills the limit
				final int end = elementsToSkip + ( maxRowsJpa - overallResults.size() );
				for ( int i = elementsToSkip; i < end; i++ ) {
					overallResults.add( list.get( i ) );
				}
				break;
			}
			else if ( elementsToSkip > 0 ) {
				// We can skip a part of this result list
				for ( int i = availableElements; i < size; i++ ) {
					overallResults.add( list.get( i ) );
				}
				elementsToSkip = 0;
			}
			else {
				overallResults.addAll( list );
			}
		}

		return overallResults;
	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, DomainQueryExecutionContext executionContext) {
		if ( executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return EmptyScrollableResults.INSTANCE;
		}
		throw new NotYetImplementedFor6Exception();
	}
}
