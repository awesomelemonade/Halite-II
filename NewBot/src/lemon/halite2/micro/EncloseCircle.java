package lemon.halite2.micro;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import hlt.Position;
import lemon.halite2.util.Circle;

public class EncloseCircle {
	//https://www.nayuki.io/res/smallest-enclosing-circle/SmallestEnclosingCircle.java
	
	public static Circle create(Collection<Position> points) {
		List<Position> shuffled = new ArrayList<Position>(points);
		Collections.shuffle(shuffled);
		Circle circle = null;
		for(int i=0;i<shuffled.size();++i) {
			Position point = shuffled.get(i);
			if(circle==null||!circle.contains(point)) {
				circle = makeCircleFromOnePoint(shuffled.subList(0, i+1), point);
			}
		}
		return circle;
	}
	private static Circle makeCircleFromOnePoint(List<Position> points, Position p) {
		Circle circle = new Circle(p, 0);
		for(int i=0;i<points.size();++i) {
			Position q = points.get(i);
			if(!circle.contains(q)) {
				if(circle.getRadius()==0) {
					circle = makeDiameter(p, q);
				}else {
					circle = makeCircleFromTwoPoints(points.subList(0, i+1), p, q);
				}
			}
		}
		return circle;
	}
	private static Circle makeCircleFromTwoPoints(List<Position> points, Position p, Position q) {
		Circle circle = makeDiameter(p, q);
		Circle left = null;
		Circle right = null;
		Position pq = subtract(q, p);
		for(Position r: points) {
			if(circle.contains(r)) {
				continue;
			}
			double cross = cross(pq, subtract(r, q));
			Circle c = makeCircumcircle(p, q, r);
			if(c==null) {
				continue;
			}else if(cross>0&&(left==null||cross(pq, subtract(c.getPosition(), p))>cross(pq, subtract(left.getPosition(), p)))) {
				left = c;
			}else if(cross<0&&(right==null||cross(pq, subtract(c.getPosition(), p))<cross(pq, subtract(right.getPosition(), p)))) {
				right = c;
			}
		}
		if(left==null&&right==null) {
			return circle;
		}else if(left==null) {
			return right;
		}else if(right==null) {
			return left;
		}else {
			return left.getRadius()<=right.getRadius()?left:right;
		}
	}
	private static Circle makeDiameter(Position a, Position b) {
		Position c = new Position((a.getX()+b.getX())/2, (a.getY()+b.getY())/2);
		double r = Math.max(c.getDistanceSquared(a), c.getDistanceSquared(b));
		return new Circle(c, Math.sqrt(r));
	}
	private static Circle makeCircumcircle(Position a, Position b, Position c) {
		double ox = (Math.min(Math.min(a.getX(), b.getX()), c.getX())+Math.max(Math.min(a.getX(), b.getX()), c.getX()))/2;
		double oy = (Math.min(Math.min(a.getY(), b.getY()), c.getY())+Math.max(Math.min(a.getY(), b.getY()), c.getY()))/2;
		double ax = a.getX()-ox, ay = a.getY()-oy;
		double bx = b.getX()-ox, by = b.getY()-oy;
		double cx = c.getX()-ox, cy = c.getY()-oy;
		double d = (ax*(by-cy)+bx*(cy-ay)+cx*(ay-by))*2;
		if(d==0) {
			return null;
		}
		double x = ((ax*ax+ay*ay)*(by-cy)+(bx*bx+by*by)*(cy-ay)+(cx*cx+cy*cy)*(ay-by))/d;
		double y = ((ax*ax+ay*ay)*(cx-bx)+(bx*bx+by*by)*(ax-cx)+(cx*cx+cy*cy)*(bx-ax))/d;
		Position p = new Position(ox+x, oy+y);
		double r = Math.sqrt(Math.max(Math.max(p.getDistanceSquared(a), p.getDistanceSquared(b)), p.getDistanceSquared(c)));
		return new Circle(p, r);
	}
	private static Position subtract(Position a, Position b) {
		return new Position(a.getX()-b.getX(), a.getY()-b.getY());
	}
	private static double cross(Position a, Position b) {
		return a.getX()*b.getY()-a.getY()*b.getX();
	}
}
