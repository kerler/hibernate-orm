/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.hana;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.Geometry;

public class HANAGeometryType implements JdbcType {

	public static final HANAGeometryType CRS_LOADING_INSTANCE = new HANAGeometryType( true );
	public static final HANAGeometryType INSTANCE = new HANAGeometryType( false );
	private static final long serialVersionUID = -6978798264716544804L;
	final boolean determineCrsIdFromDatabase;

	public HANAGeometryType(boolean determineCrsIdFromDatabase) {
		this.determineCrsIdFromDatabase = determineCrsIdFromDatabase;
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.OTHER;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.GEOMETRY;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final Geometry<?> geometry = getJavaTypeDescriptor().unwrap( value, Geometry.class, options );
				st.setObject( index, HANASpatialUtils.toEWKB( geometry ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Geometry<?> geometry = getJavaTypeDescriptor().unwrap( value, Geometry.class, options );
				st.setObject( name, HANASpatialUtils.toEWKB( geometry ) );
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				if ( HANAGeometryType.this.determineCrsIdFromDatabase ) {
					throw new UnsupportedOperationException( "First need to refactor HANASpatialUtils" );
					//return getJavaTypeDescriptor().wrap( HANASpatialUtils.toGeometry( rs, paramIndex ), options );
				}
				else {
					return getJavaTypeDescriptor().wrap( HANASpatialUtils.toGeometry( rs.getObject( paramIndex ) ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaTypeDescriptor().wrap( HANASpatialUtils.toGeometry( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaTypeDescriptor().wrap( HANASpatialUtils.toGeometry( statement.getObject( name ) ), options );
			}
		};
	}

}
