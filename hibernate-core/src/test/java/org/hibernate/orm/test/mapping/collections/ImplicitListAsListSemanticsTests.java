/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.CollectionClassificationType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.CollectionClassification;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsListSemanticsTests.ImplicitListAsListProvider.class )
)
@DomainModel( annotatedClasses = ImplicitListAsListSemanticsTests.AnEntity.class )
public class ImplicitListAsListSemanticsTests {
	@Test
	void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( AnEntity.class, (descriptor) -> {
			final Property implicitList = descriptor.getProperty( "implicitList" );
			// because of `AvailableSettings.DEFAULT_LIST_SEMANTICS`, this should be LIST
			assertThat( implicitList.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );

			final Property implicitBag = descriptor.getProperty( "implicitBag" );
			assertThat( implicitBag.getValue() ).isInstanceOf( Bag.class );

			final Property explicitBag = descriptor.getProperty( "explicitBag" );
			assertThat( explicitBag.getValue() ).isInstanceOf( Bag.class );

			final Property explicitList = descriptor.getProperty( "explicitList" );
			assertThat( explicitList.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
		} );
	}

	@Entity( name = "AnEntity" )
	@Table( name = "t_entity" )
	public static class AnEntity {
	    @Id
	    private Integer id;
	    @Basic
		private String name;

		@ElementCollection
		private List<String> implicitList;

		@ElementCollection
		private Collection<String> implicitBag;

		@ElementCollection
		@CollectionClassificationType( CollectionClassification.BAG )
		private List<String> explicitBag;

		@ElementCollection
		@CollectionClassificationType( CollectionClassification.LIST )
		private List<String> explicitList;

		private AnEntity() {
			// for use by Hibernate
		}

		public AnEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class ImplicitListAsListProvider implements SettingProvider.Provider<CollectionClassification> {
		@Override
		public CollectionClassification getSetting() {
			return CollectionClassification.LIST;
		}
	}
}
