/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.type.BasicType;

/**
 * Represents a replication strategy.
 *
 * @author Gavin King
 * @see Session#replicate(Object, ReplicationMode)
 */
public enum ReplicationMode {
	/**
	 * Throw an exception when a row already exists.
	 */
	EXCEPTION {
		@Override
		public boolean shouldOverwriteCurrentVersion(Object entity, Object currentVersion, Object newVersion, BasicType<Object> versionType) {
			throw new AssertionFailure( "should not be called" );
		}
	},
	/**
	 * Ignore replicated entities when a row already exists.
	 */
	IGNORE {
		@Override
		public boolean shouldOverwriteCurrentVersion(Object entity, Object currentVersion, Object newVersion, BasicType<Object> versionType) {
			return false;
		}
	},
	/**
	 * Overwrite existing rows when a row already exists.
	 */
	OVERWRITE {
		@Override
		public boolean shouldOverwriteCurrentVersion(Object entity, Object currentVersion, Object newVersion, BasicType<Object> versionType) {
			return true;
		}
	},
	/**
	 * When a row already exists, choose the latest version.
	 */
	LATEST_VERSION {
		@Override
		@SuppressWarnings("unchecked")
		public boolean shouldOverwriteCurrentVersion(Object entity, Object currentVersion, Object newVersion, BasicType<Object> versionType) {
			// always overwrite non-versioned data (because we don't know which is newer)
			return versionType == null || versionType.getJavaTypeDescriptor().getComparator().compare( currentVersion, newVersion ) <= 0;
		}
	};

	/**
	 * Determine whether the mode dictates that the data being replicated should overwrite the data found.
	 *
	 * @param entity The entity being replicated
	 * @param currentVersion The version currently on the target database table.
	 * @param newVersion The replicating version
	 * @param versionType The version type
	 *
	 * @return {@code true} indicates the data should be overwritten; {@code false} indicates it should not.
	 */
	public abstract boolean shouldOverwriteCurrentVersion(Object entity, Object currentVersion, Object newVersion, BasicType<Object> versionType);

}
