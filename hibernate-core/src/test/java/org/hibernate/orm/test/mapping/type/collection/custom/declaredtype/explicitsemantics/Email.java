/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.collection.custom.declaredtype.explicitsemantics;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@Entity(name = "Email")
public class Email {

	private Long id;
	private String address;

	Email() {
	}

	public Email(String address) {
		this.address = address;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String type) {
		this.address = type;
	}

	@Override
	public boolean equals(Object that) {
		if ( !( that instanceof Email ) ) {
			return false;
		}
		Email p = (Email) that;
		return this.address.equals( p.address );
	}

	@Override
	public int hashCode() {
		return address.hashCode();
	}

}
