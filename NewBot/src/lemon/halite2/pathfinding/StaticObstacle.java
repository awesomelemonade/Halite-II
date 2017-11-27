package lemon.halite2.pathfinding;

import hlt.Vector;
import lemon.halite2.util.Circle;
import lemon.halite2.util.Geometry;

public class StaticObstacle implements Obstacle {
	private Circle circle;
	public StaticObstacle(Circle circle) {
		this.circle = circle;
	}
	@Override
	public boolean willCollide(Vector position, Vector velocity, double buffer) {
		if(Vector.ZERO.equals(velocity)) {
			return circle.getPosition().getDistanceSquared(position)<=(circle.getRadius()+buffer)*(circle.getRadius()+buffer);
		}else {
			return Geometry.segmentCircleIntersection(position, position.add(velocity), circle.getPosition(), buffer+circle.getRadius());
		}
	}
}
