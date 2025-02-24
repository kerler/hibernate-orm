/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.referencedcolumnname;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Inhabitant implements Serializable {
	private Integer id;
	private String name;
	private Set<House> livesIn = new HashSet<House>();

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToMany(mappedBy = "hasInhabitants")
	public Set<House> getLivesIn() {
		return livesIn;
	}

	public void setLivesIn(Set<House> livesIn) {
		this.livesIn = livesIn;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Inhabitant ) ) return false;

		final Inhabitant inhabitant = (Inhabitant) o;

		if ( name != null ? !name.equals( inhabitant.name ) : inhabitant.name != null ) return false;

		return true;
	}

	public int hashCode() {
		return ( name != null ? name.hashCode() : 0 );
	}
}
