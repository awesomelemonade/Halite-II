package lemon.halite2.util;

import hlt.Position;

public class Geometry {
	// Arc - Circle
	// Segment - Point Distance
	public static double segmentPointDistance(Position start, Position end, Position point) {
		// Standard form of the line ax+by+c = 0
		double a = start.getY() - end.getY();
		double b = end.getX() - start.getX();
		double c = (start.getX() - end.getX()) * start.getY() + (end.getY() - start.getY()) * start.getX();
		// Calculate closest to point on the line above
		double x = (b * (b * point.getX() - a * point.getY()) - a * c) / (a * a + b * b);
		double y = (a * (-b * point.getX() + a * point.getY()) - b * c) / (a * a + b * b);
		boolean xTest = ((start.getX() <= x && x <= end.getX()) || (start.getX() >= x && x >= end.getX()));
		boolean yTest = ((start.getY() <= y && y <= end.getY()) || (start.getY() >= y && y >= end.getY()));
		// Check if (x, y) is between start and end
		if ((xTest||Math.abs(end.getX()-start.getX())<0.01)&&
				(yTest||Math.abs(end.getY()-start.getY())<0.01)) {
			return Math.sqrt((point.getX() - x) * (point.getX() - x) + (point.getY() - y) * (point.getY() - y));
		} else {
			double i = point.getDistanceSquared(start);
			double j = point.getDistanceSquared(end);
			return i < j ? Math.sqrt(i) : Math.sqrt(j);
		}
	}
	public static boolean segmentCircleIntersection(Position a, Position b, Position center, double buffer) {
		return segmentPointDistance(a, b, center)<=buffer;
	}
	// Used in Segment-Segment Intersection; Intersection between segment a, b and point c
	public static boolean segmentPointIntersection(Position a, Position b, Position c) {
		return (c.getX()<=Math.max(a.getX(), b.getX())&&c.getX()>=Math.min(a.getX(), b.getX())&&
				c.getY()<=Math.max(a.getY(), b.getY())&&c.getY()>=Math.min(a.getY(), b.getY()));
	}
	// Used in Segment-Segment Intersection; Retrieves Colinearity, Clockwise, or Counter-Clockwise
	public static int getOrientation(Position a, Position b, Position c) {
		double val = (b.getY()-a.getY())*(c.getX()-b.getX())-(b.getX()-a.getX())*(c.getY()-b.getY());
		if(val==0) {
			return 0;
		}
		return val>0?1:-1;
	}
	// http://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/
	public static boolean segmentSegmentIntersection(Position a, Position b, Position c, Position d) {
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
	public static double segmentSegmentDistance(Position a, Position b, Position c, Position d) {
		if(segmentSegmentIntersection(a, b, c, d)) {
			return 0;
		}else {
			double i = Geometry.segmentPointDistance(a, b, c);
			double j = Geometry.segmentPointDistance(a, b, d);
			double k = Geometry.segmentPointDistance(c, d, a);
			double l = Geometry.segmentPointDistance(c, d, b);
			return Math.min(Math.min(i, j), Math.min(k, l));
		}
	}
}
