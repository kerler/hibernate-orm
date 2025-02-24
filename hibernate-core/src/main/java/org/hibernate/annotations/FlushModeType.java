/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Enumeration extending jakarta.persistence flush modes.
 *
 * @author Carlos Gonzalez-Cadenas
 */
public enum FlushModeType {
	/**
	 * Corresponds to {@link org.hibernate.FlushMode#ALWAYS}.
	 */
	ALWAYS,
	/**
	 * Corresponds to  {@link org.hibernate.FlushMode#AUTO}.
	 */
	AUTO,
	/**
	 * Corresponds to  {@link org.hibernate.FlushMode#COMMIT}.
	 */
	COMMIT,
	/**
	 * Corresponds to  {@link org.hibernate.FlushMode#MANUAL}.
	 */
	MANUAL,
	/**
	 * Current flush mode of the persistence context at the time the query is executed.
	 */
	PERSISTENCE_CONTEXT
}
