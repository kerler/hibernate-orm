/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations;
import java.io.Serializable;
import java.util.Collection;
import java.util.SortedSet;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.SortComparator;

import static org.hibernate.annotations.CascadeType.ALL;


/**
 * Company customer
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Customer implements Serializable {
	Long id;
	String name;
	SortedSet<Ticket> tickets;
	Collection discountTickets;
	Passport passport;

	public Customer() {
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setId(Long long1) {
		id = long1;
	}

	public void setName(String string) {
		name = string;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "CUST_ID")
	@SortComparator(TicketComparator.class)
	public SortedSet<Ticket> getTickets() {
		return tickets;
	}

	public void setTickets(SortedSet<Ticket> tickets) {
		this.tickets = tickets;
	}

	@OneToMany(targetEntity = Discount.class,
			cascade = CascadeType.ALL, mappedBy = "owner")
	@Cascade({ALL})
	public Collection getDiscountTickets() {
		return discountTickets;
	}

	public void setDiscountTickets(Collection collection) {
		discountTickets = collection;
	}

	@OneToOne(cascade = CascadeType.ALL)
	public Passport getPassport() {
		return passport;
	}

	public void setPassport(Passport passport) {
		this.passport = passport;
	}

}

