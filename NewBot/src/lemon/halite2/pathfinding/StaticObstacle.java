package lemon.halite2.pathfinding;

import hlt.Position;
import lemon.halite2.util.Circle;
import lemon.halite2.util.Geometry;

public class StaticObstacle implements Obstacle {
	private Circle circle;
	private int priority;
	public StaticObstacle(Circle circle, int priority) {
		this.circle = circle;
		this.priority = priority;
	}
	@Override
	public boolean willCollide(Position position, Position velocity, double buffer) {
		if(Position.ZERO.equals(velocity)) {
			return circle.getPosition().getDistanceSquared(position)<=(circle.getRadius()+buffer)*(circle.getRadius()+buffer);
		}else {
			return Geometry.segmentCircleIntersection(position, position.add(velocity), circle.getPosition(), buffer+circle.getRadius());
		}
	}
	@Override
	public int getPriority() {
		return priority;
	}
}
