package lemon.halite2.util;

import java.util.Set;

import hlt.GameConstants;
import hlt.Position;
import hlt.RoundPolicy;
import hlt.ThrustPlan;

public class Pathfinder {
	private Position start;
	private double buffer;
	
	private int magnitude;
	private int directionDegrees;
	private int bestDirection;
	
	private boolean[] staticDirections;
	private boolean[] dynamicDirections;
	private boolean[] uncertainDirections;
	
	public Pathfinder(Position start, Position end, double buffer, double endBuffer) {
		this.start = start;
		this.buffer = buffer;
		this.magnitude = (int)Math.min(7, start.getDistanceTo(end)-buffer-endBuffer);
		this.directionDegrees = RoundPolicy.ROUND.applyDegrees(start.getDirectionTowards(end));
		this.bestDirection = -1;
		this.staticDirections = new boolean[MathUtil.TAU_DEGREES];
		this.dynamicDirections = new boolean[MathUtil.TAU_DEGREES];
		this.uncertainDirections = new boolean[MathUtil.TAU_DEGREES];
	}
	public int calcBestDirection() {
		for(int i=0;i<=MathUtil.PI_DEGREES;++i) {
			int pos = MathUtil.normalizeDegrees(directionDegrees+i);
			int neg = MathUtil.normalizeDegrees(directionDegrees-i);
			if(!isConflicting(pos)) {
				return pos;
			}
			if(!isConflicting(neg)) {
				return neg;
			}
		}
		return -1;
	}
	public boolean isConflicting(int direction) {
		return staticDirections[direction]||dynamicDirections[direction]||uncertainDirections[direction];
	}
	public boolean resolveConflicts() {
		//reresolve Uncertain Regions
		this.uncertainDirections = new boolean[MathUtil.TAU_DEGREES];
		resolveUncertainObstacles();
		bestDirection = calcBestDirection();
		return bestDirection!=-1;
	}
	public void resolveStaticObstacles() {
		resolveObstacles(Obstacles.getStaticObstacles(), staticDirections);
	}
	public void resolveUncertainObstacles() {
		resolveObstacles(Obstacles.getUncertainRegions(), uncertainDirections);
	}
	public void resolveObstacles(Set<Circle> obstacles, boolean[] directions) {
		//Resolve Static Obstacles
		for(Circle obstacle: obstacles) {
			double distSquared = start.getDistanceSquared(obstacle.getPosition());
			if(distSquared<(obstacle.getRadius()+magnitude)*(obstacle.getRadius()+magnitude)&&
					!obstacle.contains(start)){
				resolveStaticObstacle(obstacle, directions);
			}
		}
	}
	public void resolveStaticObstacle(Circle circle, boolean[] directions) {
		double direction = start.getDirectionTowards(circle.getPosition());
		double theta = Math.asin((circle.getRadius()+buffer)/start.getDistanceTo(circle.getPosition()));
		int start = RoundPolicy.COUNTER_CLOCKWISE.applyDegrees(direction-theta);
		int end = RoundPolicy.CLOCKWISE.applyDegrees(direction+theta);
		for(int i=start;i!=end;i=(i+1)%360) {
			directions[i] = true;
		}
	}
	public void resolveDynamicObstacle(Circle circle, ThrustPlan plan, boolean[] directions) {
		Position endPosition = circle.getPosition().addPolar(plan.getThrust(), Math.toRadians(plan.getAngle()));
		if(!Geometry.segmentCircleIntersection(circle.getPosition(), endPosition, start, circle.getRadius()+magnitude+GameConstants.SHIP_RADIUS)) {
			return;
		}
		Position velocity = new Position(plan.getThrust()*Math.cos(Math.toRadians(plan.getAngle())),
				plan.getThrust()*Math.sin(Math.toRadians(plan.getAngle())));
		double direction = start.getDirectionTowards(circle.getPosition());
		double direction2 = start.getDirectionTowards(endPosition);
		double theta = Math.asin((circle.getRadius()+buffer)/start.getDistanceTo(circle.getPosition()));
		double theta2 = Math.asin((circle.getRadius()+buffer)/start.getDistanceTo(endPosition));
		int a = RoundPolicy.COUNTER_CLOCKWISE.applyDegrees(direction-theta);
		int b = RoundPolicy.CLOCKWISE.applyDegrees(direction2+theta2);
		int c = RoundPolicy.CLOCKWISE.applyDegrees(direction+theta);
		int d = RoundPolicy.COUNTER_CLOCKWISE.applyDegrees(direction2-theta2);
		int start, end;
		if(MathUtil.angleBetweenDegrees(a, b)>MathUtil.angleBetweenDegrees(c, d)) {
			if(MathUtil.normalizeDegrees(b-a)>MathUtil.PI_DEGREES) {
				start = b;
				end = a;
			}else {
				start = a;
				end = b;
			}
		}else {
			if(MathUtil.normalizeDegrees(d-c)>MathUtil.PI_DEGREES) {
				start = d;
				end = c;
			}else {
				start = c;
				end = d;
			}
		}
		for(int i=start;i!=end;i=(i+1)%360) {
			Position velocity2 = new Position(magnitude*Math.cos(Math.toRadians(i)), magnitude*Math.sin(Math.toRadians(i)));
			if(checkCollisions(circle.getPosition(), this.start, velocity, velocity2, buffer+circle.getRadius())) {
				directions[i] = true;
			}
		}
	}
	public ThrustPlan getThrustPlan() {
		return new ThrustPlan(magnitude, bestDirection);
	}
	public static boolean checkCollisions(Position a, Position b, Position velocityA, Position velocityB, double buffer) {
		double time = MathUtil.getMinTime(a, b, velocityA, velocityB);
		time = Math.max(0, Math.min(1, time)); //Clamp between 0 and 1
		return buffer*buffer>=MathUtil.getDistanceSquared(a, b, velocityA, velocityB, time);
	}
}
