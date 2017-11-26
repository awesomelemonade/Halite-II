package lemon.halite2.util;

import hlt.Vector;

public class Geometry {
	public static Vector projectPointToLine(Vector start, Vector end, Vector point) {
		// Standard form of the line ax+by+c = 0
		double a = start.getY() - end.getY();
		double b = end.getX() - start.getX();
		double c = (start.getX() - end.getX()) * start.getY() + (end.getY() - start.getY()) * start.getX();
		// Calculate closest to point on the line above
		double x = (b * (b * point.getX() - a * point.getY()) - a * c) / (a * a + b * b);
		double y = (a * (-b * point.getX() + a * point.getY()) - b * c) / (a * a + b * b);
		return new Vector(x, y);
	}
	// Segment - Point Distance
	public static double segmentPointDistanceSquared(Vector start, Vector end, Vector point) {
		Vector projection = projectPointToLine(start, end, point);
		boolean xTest = ((start.getX() <= projection.getX() && projection.getX() <= end.getX()) ||
				(start.getX() >= projection.getX() && projection.getX() >= end.getX()));
		boolean yTest = ((start.getY() <= projection.getY() && projection.getY() <= end.getY()) ||
				(start.getY() >= projection.getY() && projection.getY() >= end.getY()));
		// Check if (x, y) is between start and end
		if ((xTest||Math.abs(end.getX()-start.getX())<0.01)&&
				(yTest||Math.abs(end.getY()-start.getY())<0.01)) {
			return projection.getDistanceTo(point);
		} else {
			double i = point.getDistanceSquared(start);
			double j = point.getDistanceSquared(end);
			return i < j ? i : j;
		}
	}
	public static boolean segmentCircleIntersection(Vector a, Vector b, Vector center, double buffer) {
		return segmentPointDistanceSquared(a, b, center)<=buffer*buffer;
	}
	// Used in Segment-Segment Intersection; Intersection between segment a, b and point c
	public static boolean segmentPointIntersection(Vector a, Vector b, Vector c) {
		return (c.getX()<=Math.max(a.getX(), b.getX())&&c.getX()>=Math.min(a.getX(), b.getX())&&
				c.getY()<=Math.max(a.getY(), b.getY())&&c.getY()>=Math.min(a.getY(), b.getY()));
	}
	// Used in Segment-Segment Intersection; Retrieves Colinearity, Clockwise, or Counter-Clockwise
	public static int getOrientation(Vector a, Vector b, Vector c) {
		double val = (b.getY()-a.getY())*(c.getX()-b.getX())-(b.getX()-a.getX())*(c.getY()-b.getY());
		if(val==0) {
			return 0;
		}
		return val>0?1:-1;
	}
	// http://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/
	public static boolean segmentSegmentIntersection(Vector a, Vector b, Vector c, Vector d) {
		int o1 = getOrientation(a, b, c);
		int o2 = getOrientation(a, b, d);
		int o3 = getOrientation(c, d, a);
		int o4 = getOrientation(c, d, b);
		if(o1!=o2&&o3!=o4) {
			return true;
		}
		if(o1==0&&segmentPointIntersection(a, b, c)) {
			return true;
		}
		if(o2==0&&segmentPointIntersection(a, b, d)) {
			return true;
		}
		if(o3==0&&segmentPointIntersection(c, d, a)) {
			return true;
		}
		if(o4==0&&segmentPointIntersection(c, d, b)) {
			return true;
		}
		return false;
	}
	public static double segmentSegmentDistanceSquared(Vector a, Vector b, Vector c, Vector d) {
		if(segmentSegmentIntersection(a, b, c, d)) {
			return 0;
		}else {
			double i = Geometry.segmentPointDistanceSquared(a, b, c);
			double j = Geometry.segmentPointDistanceSquared(a, b, d);
			double k = Geometry.segmentPointDistanceSquared(c, d, a);
			double l = Geometry.segmentPointDistanceSquared(c, d, b);
			return Math.min(Math.min(i, j), Math.min(k, l));
		}
	}
}
