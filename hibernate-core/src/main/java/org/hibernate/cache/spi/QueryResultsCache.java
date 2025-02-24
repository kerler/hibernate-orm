/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Defines the responsibility for managing query result data caching
 * in regards to a specific region.
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface QueryResultsCache {
	/**
	 * The underlying cache region being used.
	 */
	QueryResultsRegion getRegion();

	/**
	 * Put a result into the query cache.
	 *
	 * @param key The cache key
	 * @param result The results to cache
	 * @param session The originating session
	 *
	 * @return Whether the put actually happened.
	 *
	 * @throws HibernateException Indicates a problem delegating to the underlying cache.
	 */
	boolean put(
			QueryKey key,
			List result,
			SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Get results from the cache.
	 *
	 * @param key The cache key
	 * @param spaces The query spaces (used in invalidation plus validation checks)
	 * @param session The originating session
	 *
	 * @return The cached results; may be null.
	 *
	 * @throws HibernateException Indicates a problem delegating to the underlying cache.
	 */
	List get(
			QueryKey key,
			Set<String> spaces,
			SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Get results from the cache.
	 *
	 * @param key The cache key
	 * @param spaces The query spaces (used in invalidation plus validation checks)
	 * @param session The originating session
	 *
	 * @return The cached results; may be null.
	 *
	 * @throws HibernateException Indicates a problem delegating to the underlying cache.
	 */
	List get(
			QueryKey key,
			String[] spaces,
			SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Clear items from the query cache.
	 *
	 * @throws CacheException Indicates a problem delegating to the underlying cache.
	 */
	default void clear() throws CacheException {
		getRegion().clear();
	}

	default void destroy() {
		// nothing to do.. the region itself gets destroyed
	}
}
