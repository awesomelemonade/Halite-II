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
			return Geometry.segmentPointDistanceSquared(circle.getPosition(), endPoint,
					position)<=(buffer+circle.getRadius())*(buffer+circle.getRadius());
		}else {
			return getMinDistanceSquared(circle.getPosition(), position, this.velocity, velocity)<=(buffer+circle.getRadius())*(buffer+circle.getRadius());
		}
	}
	@Override
	public int getPriority() {
		return priority;
	}
	public static double getMinDistanceSquared(Position a, Position b, Position velocityA, Position velocityB) {
		double time = MathUtil.getMinTime(a, b, velocityA, velocityB);
		time = Math.max(0, Math.min(1, time)); //Clamp between 0 and 1
		return MathUtil.getDistanceSquared(a, b, velocityA, velocityB, time);
	}
}
