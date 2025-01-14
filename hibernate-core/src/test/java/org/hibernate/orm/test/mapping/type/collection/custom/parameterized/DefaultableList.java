/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.type.collection.custom.parameterized;

import java.util.List;

/**
 * Our specialized collection contract
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
public interface DefaultableList extends List {
    public String getDefaultValue();
}
