/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
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
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isOneOf;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = OffsetTimeMappingTests.EntityWithOffsetTime.class)
@SessionFactory
public class OffsetTimeMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor(EntityWithOffsetTime.class);

		final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("offsetTime");
		final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(OffsetTime.class));
		assertThat(jdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode(), isOneOf(Types.TIME, Types.TIME_WITH_TIMEZONE));

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithOffsetTime(1, OffsetTime.now()));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithOffsetTime.class, 1)
		);
	}

	@Entity(name = "EntityWithOffsetTime")
	@Table(name = "EntityWithOffsetTime")
	public static class EntityWithOffsetTime {
		@Id
		private Integer id;

		//tag::basic-offsetTime-example[]
		// mapped as TIME or TIME_WITH_TIMEZONE
		private OffsetTime offsetTime;
		//end::basic-offsetTime-example[]

		public EntityWithOffsetTime() {
		}

		public EntityWithOffsetTime(Integer id, OffsetTime offsetTime) {
			this.id = id;
			this.offsetTime = offsetTime;
		}
	}
}
