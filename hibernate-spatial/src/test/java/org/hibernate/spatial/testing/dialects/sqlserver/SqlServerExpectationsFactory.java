/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.sqlserver;

import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.NativeSQLStatement;

import org.geolatte.geom.codec.db.sqlserver.Decoders;
import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;


/**
 * Implementation of an <code>AbstractExpectationsFactory</code>
 * for Microsoft SQL Server (2008).
 */
public class SqlServerExpectationsFactory extends AbstractExpectationsFactory {


	public SqlServerExpectationsFactory() {
		super();
	}

	@Override
	public NativeSQLStatement createNativeDimensionSQL() {
		return createNativeSQLStatement( "select t.id, t.geom.STDimension() from GeomTest t" );
	}

	@Override
	public NativeSQLStatement createNativeBufferStatement(Double distance) {
		return createNativeSQLStatement(
				"select t.id, t.geom.STBuffer(?) from GeomTest t where t.geom.STSrid = 4326",
				new Object[] { distance }
		);
	}

	@Override
	public NativeSQLStatement createNativeConvexHullStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STUnion(geometry::STGeomFromText(?, 4326)).STConvexHull() from GeomTest t where t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeIntersectionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STIntersection(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STDifference(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeSymDifferenceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STSymDifference(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeGeomUnionStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STUnion(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeAsTextStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.STAsText() from GeomTest t" );
	}

	@Override
	public NativeSQLStatement createNativeSridStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.STSrid from GeomTest t" );
	}

	@Override
	public NativeSQLStatement createNativeIsSimpleStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.STIsSimple() from GeomTest t" );
	}

	@Override
	public NativeSQLStatement createNativeIsEmptyStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.STIsEmpty() from GeomTest t" );
	}

	@Override
	public NativeSQLStatement createNativeIsNotEmptyStatement() {
		return createNativeSQLStatement( "select t.id, ~t.geom.STIsEmpty() from GeomTest t" );
	}

	@Override
	public NativeSQLStatement createNativeBoundaryStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.STBoundary() from GeomTest t" );
	}

	@Override
	public NativeSQLStatement createNativeEnvelopeStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.STEnvelope() from GeomTest t" );
	}

	@Override
	public NativeSQLStatement createNativeAsBinaryStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.STAsBinary() from GeomTest t" );
	}

	@Override
	public NativeSQLStatement createNativeGeometryTypeStatement() {
		return createNativeSQLStatement( "select t.id, t.geom.STGeometryType() from GeomTest t" );
	}

	@Override
	protected Geometry decode(Object o) {
		return JTS.to( Decoders.decode( (byte[]) o ) );
	}

	@Override
	public NativeSQLStatement createNativeWithinStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STWithin(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STWithin(geometry::STGeomFromText(?, 4326)) = 'true' and t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeEqualsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STEquals(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STEquals(geometry::STGeomFromText(?, 4326)) = 'true' and t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeCrossesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STCrosses(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STCrosses(geometry::STGeomFromText(?, 4326)) = 'true' and t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeContainsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STContains(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STContains(geometry::STGeomFromText(?, 4326)) = 'true' and t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeDisjointStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STDisjoint(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STDisjoint(geometry::STGeomFromText(?, 4326)) = 'true' and t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeTransformStatement(int epsg) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeSQLStatement createNativeHavingSRIDStatement(int srid) {
		return createNativeSQLStatement( "select t.id, 1 from GeomTest t where t.geom.STSrid =  " + srid );
	}

	@Override
	public NativeSQLStatement createNativeIntersectsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STIntersects(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STIntersects(geometry::STGeomFromText(?, 4326)) = 'true' and t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeFilterStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.Filter(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.Filter(geometry::STGeomFromText(?, 4326)) = 1 and t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeTouchesStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STTouches(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STTouches(geometry::STGeomFromText(?, 4326)) = 'true' and t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeOverlapsStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STOverlaps(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STOverlaps(geometry::STGeomFromText(?, 4326)) = 'true' and t.geom.STSrid = 4326",
				geom.toText()
		);
	}

	@Override
	public NativeSQLStatement createNativeRelateStatement(Geometry geom, String matrix) {
		String sql = "select t.id, t.geom.STRelate(geometry::STGeomFromText(?, 4326), '" + matrix + "' ) from GeomTest t where t.geom.STRelate(geometry::STGeomFromText(?, 4326), '" + matrix + "') = 'true' and t.geom.STSrid = 4326";
		return createNativeSQLStatementAllWKTParams( sql, geom.toText() );
	}

	@Override
	public NativeSQLStatement createNativeDwithinStatement(Point geom, double distance) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeSQLStatement createNativeDistanceStatement(Geometry geom) {
		return createNativeSQLStatementAllWKTParams(
				"select t.id, t.geom.STDistance(geometry::STGeomFromText(?, 4326)) from GeomTest t where t.geom.STSrid = 4326",
				geom.toText()
		);
	}

}
