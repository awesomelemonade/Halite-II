package lemon.halite2.pathfinding;

import hlt.Position;
import hlt.ThrustPlan;
import lemon.halite2.util.Circle;
import lemon.halite2.util.Geometry;
import lemon.halite2.util.MathUtil;

public class DynamicObstacle implements Obstacle {
	private Circle circle;
	private Position velocity;
	private Position endPoint; // so no recalculation in willCollide
	private int priority;
	public DynamicObstacle(Circle circle, ThrustPlan plan, int priority) {
		this.circle = circle;
		if(plan.getThrust()==0) {
			this.velocity = Position.ZERO;
		}else {
			this.velocity = Pathfinder.velocityVector[plan.getThrust()-1][plan.getAngle()];
		}
		this.priority = priority;
		this.endPoint = circle.getPosition().add(velocity);
	}
	@Override
	public boolean willCollide(Position position, Position velocity, double buffer) {
		if(Position.ZERO.equals(velocity)) {
			return Geometry.segmentCircleIntersection(circle.getPosition(), endPoint, position, buffer+circle.getRadius());
		}else {
			return MathUtil.getMinDistanceSquared(circle.getPosition(), this.velocity, position, velocity)<=(buffer+circle.getRadius())*(buffer+circle.getRadius());
		}
	}
	@Override
	public int getPriority() {
		return priority;
	}
}
