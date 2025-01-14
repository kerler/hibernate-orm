/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.oracle;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.db.oracle.Decoders;
import org.geolatte.geom.codec.db.oracle.SDOGeometry;


//TODO -- requires cleanup and must be made package local

/**
 * ValueExtractor for SDO_GEOMETRY
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 8/22/11
 */
public class SDOGeometryValueExtractor<X> extends BasicExtractor<X> {

	/**
	 * Creates instance
	 *
	 * @param javaDescriptor the {@code JavaTypeDescriptor} to use
	 * @param sqlTypeDescriptor the {@code SqlTypeDescriptor} to use
	 */
	public SDOGeometryValueExtractor(JavaType<X> javaDescriptor, JdbcType sqlTypeDescriptor) {
		super( javaDescriptor, sqlTypeDescriptor );
	}

	@Override
	protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
		final Object geomObj = rs.getObject( paramIndex );
		return getJavaTypeDescriptor().wrap( convert( geomObj ), options );
	}

	@Override
	protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
		final Object geomObj = statement.getObject( index );
		return getJavaTypeDescriptor().wrap( convert( geomObj ), options );
	}

	@Override
	protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
		final Object geomObj = statement.getObject( name );
		return getJavaTypeDescriptor().wrap( convert( geomObj ), options );
	}

	/**
	 * Converts an oracle to a JTS Geometry
	 *
	 * @param struct The Oracle STRUCT representation of an SDO_GEOMETRY
	 *
	 * @return The JTS Geometry value
	 */
	public Geometry convert(Object struct) {
		if ( struct == null ) {
			return null;
		}
		final SDOGeometry sdogeom = SDOGeometry.load( (Struct) struct );
		return toGeomerty( sdogeom );
	}

	private Geometry toGeomerty(SDOGeometry sdoGeom) {
		return Decoders.decode( sdoGeom );
	}

}
