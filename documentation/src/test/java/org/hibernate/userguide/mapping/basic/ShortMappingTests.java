/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for mapping `short` values
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = ShortMappingTests.EntityOfShorts.class)
@SessionFactory
public class ShortMappingTests {

	@Test
	public void testMappings(SessionFactoryScope scope) {
		// first, verify the type selections...
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor(EntityOfShorts.class);

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapper");
			assertThat(attribute.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Short.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Short.class));
			assertThat(jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), is(Types.SMALLINT));
		}

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitive");
			assertThat(attribute.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Short.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Short.class));
			assertThat(jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), is(Types.SMALLINT));
		}


		// and try to use the mapping
		scope.inTransaction(
				(session) -> session.persist(new EntityOfShorts(1, (short) 3, (short) 5))
		);
		scope.inTransaction(
				(session) -> session.get(EntityOfShorts.class, 1)
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery("delete EntityOfShorts").executeUpdate()
		);
	}

	@Entity(name = "EntityOfShorts")
	@Table(name = "EntityOfShorts")
	public static class EntityOfShorts {
		@Id
		Integer id;

		//tag::basic-short-example-implicit[]
		// these will both be mapped using SMALLINT
		Short wrapper;
		short primitive;
		//end::basic-short-example-implicit[]

		public EntityOfShorts() {
		}

		public EntityOfShorts(Integer id, Short wrapper, short primitive) {
			this.id = id;
			this.wrapper = wrapper;
			this.primitive = primitive;
		}
	}
}
