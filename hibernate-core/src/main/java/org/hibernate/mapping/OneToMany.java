/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * A mapping for a one-to-many association
 *
 * @author Gavin King
 */
public class OneToMany implements Value {
	private final MetadataBuildingContext buildingContext;
	private final Table referencingTable;

	private String referencedEntityName;
	private PersistentClass associatedClass;
	private boolean ignoreNotFound;

	public OneToMany(MetadataBuildingContext buildingContext, PersistentClass owner) throws MappingException {
		this.buildingContext = buildingContext;
		this.referencingTable = ( owner == null ) ? null : owner.getTable();
	}

	public MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return buildingContext.getBuildingOptions().getServiceRegistry();
	}

	private EntityType getEntityType() {
		return MappingHelper.manyToOne(
				getReferencedEntityName(),
				true,
				null,
				null,
				false,
				false,
				isIgnoreNotFound(),
				false,
				buildingContext
		);
	}

	public PersistentClass getAssociatedClass() {
		return associatedClass;
	}

	/**
	 * Associated entity on the many side
	 */
	public void setAssociatedClass(PersistentClass associatedClass) {
		this.associatedClass = associatedClass;
	}

	public void createForeignKey() {
		// no foreign key element for a one-to-many
	}

	public Iterator<Selectable> getColumnIterator() {
		return associatedClass.getKey().getColumnIterator();
	}

	@Override
	public List<Selectable> getSelectables() {
		return associatedClass.getKey().getSelectables();
	}

	public int getColumnSpan() {
		return associatedClass.getKey().getColumnSpan();
	}

	public FetchMode getFetchMode() {
		return FetchMode.JOIN;
	}

	/**
	 * Table of the owner entity (the "one" side)
	 */
	public Table getTable() {
		return referencingTable;
	}

	public Type getType() {
		return getEntityType();
	}

	public boolean isNullable() {
		return false;
	}

	public boolean isSimpleValue() {
		return false;
	}

	public boolean isAlternateUniqueKey() {
		return false;
	}

	public boolean hasFormula() {
		return false;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		if ( referencedEntityName == null ) {
			throw new MappingException( "one to many association must specify the referenced entity" );
		}
		return true;
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	/**
	 * Associated entity on the "many" side
	 */
	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName == null ? null : referencedEntityName.intern();
	}

	public void setTypeUsingReflection(String className, String propertyName) {
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept( this );
	}

	@Override
	public boolean isSame(Value other) {
		return this == other || other instanceof OneToMany && isSame( (OneToMany) other );
	}

	public boolean isSame(OneToMany other) {
		return Objects.equals( referencingTable, other.referencingTable )
				&& Objects.equals( referencedEntityName, other.referencedEntityName )
				&& Objects.equals( associatedClass, other.associatedClass );
	}

	public boolean[] getColumnInsertability() {
		//TODO: we could just return all false...
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasAnyInsertableColumns() {
		return false;
	}

	public boolean[] getColumnUpdateability() {
		//TODO: we could just return all false...
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasAnyUpdatableColumns() {
		return false;
	}

	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}

}
