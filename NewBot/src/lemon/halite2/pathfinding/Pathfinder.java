package lemon.halite2.pathfinding;

import java.util.List;
import java.util.function.Predicate;

import hlt.Vector;
import hlt.RoundPolicy;
import hlt.ThrustPlan;
import lemon.halite2.util.Geometry;
import lemon.halite2.util.MathUtil;

public class Pathfinder {
	public static Vector[][] velocityVector;

	private Vector position;
	private double buffer;
	//null = not checked; stores blame
	
	private Obstacles<ObstacleType> obstacles;
	private Predicate<Obstacle> exceptions;
	
	public static void init() {
		velocityVector = new Vector[7][MathUtil.TAU_DEGREES];
		for(int i=0;i<velocityVector.length;++i) {
			for(int j=0;j<velocityVector[0].length;++j) {
				double radians = Math.toRadians(j);
				velocityVector[i][j] = new Vector((i+1)*Math.cos(radians), (i+1)*Math.sin(radians));
			}
		}
	}
	public Pathfinder(Vector position, double buffer, Obstacles<ObstacleType> obstacles, Predicate<Obstacle> exceptions) {
		this.position = position;
		this.buffer = buffer;
		this.exceptions = exceptions;
	}
	public Vector getVector() {
		return position;
	}
	public Obstacle getStillCandidate(ObstacleType type) {
		for(Obstacle obstacle: obstacles.getObstacles(type)) {
			if(exceptions.test(obstacle)) {
				continue;
			}
			if(obstacle.willCollide(position, Vector.ZERO, buffer)){
				return obstacle;
			}
		}
		return null;
	}
	public Obstacle getCandidate(ObstacleType type, ThrustPlan plan) {
		return getCandidate(type, plan.getThrust(), plan.getAngle());
	}
	public Obstacle getCandidate(ObstacleType type, int thrust, int angle) {
		Vector velocity = velocityVector[thrust-1][angle];
		if(thrust==0){
			return getStillCandidate(type);
		}else{
			for(Obstacle obstacle: obstacles.getObstacles(type)){
				if(exceptions.test(obstacle)){
					continue;
				}
				if(obstacle.willCollide(position, velocity, buffer)){
					return obstacle;
				}
			}
		}
		return null;
	}
	public ThrustPlan getGreediestPlan(Vector target, double innerBuffer, double outerBuffer) {
		int bestThrust = 0;
		int bestAngle = 0;
		double bestDistance = Double.MAX_VALUE;
		//General Case
		for(int j=0;j<MathUtil.TAU_DEGREES;++j){ //offset
			for(int i=1;i<=7;++i){ //magnitude
				Vector endPoint = position.add(velocityVector[i-1][j]);
				if(Geometry.segmentCircleIntersection(position, endPoint, target, innerBuffer)) {
					break;
				}else {
					double distance = target.getDistanceSquared(endPoint);
					if(distance<bestDistance) {
						bestDistance = distance;
						bestThrust = i;
						bestAngle = j;
					}
				}
			}
		}
		//Still case
		double distance = target.getDistanceSquared(position);
		if(distance<bestDistance) {
			bestDistance = distance;
			bestThrust = 0;
			bestAngle = 0;
		}
		return bestDistance==Double.MAX_VALUE?null:new ThrustPlan(bestThrust, bestAngle);
	}
	public ThrustPlan getGreedyPlan(Vector target, double innerBuffer, double outerBuffer, int priority) {
		if(position.getDistanceSquared(target)<=outerBuffer*outerBuffer) {
			ThrustPlan greediestPlan = getGreediestPlan(target, innerBuffer, outerBuffer);
			return greediestPlan;
		}
		double direction = position.getDirectionTowards(target);
		double directionDegrees = Math.toDegrees(direction);
		int roundedDegrees = RoundPolicy.ROUND.applyDegrees(direction);
		int preferredSign = directionDegrees-((int)directionDegrees)<0.5?1:-1; //opposite because roundedDegrees is rounded already
		//General Case
		for(int i=7;i>=1;--i){ //magnitude
			for(int j=0;j<=MathUtil.PI_DEGREES;++j){ //offset
				int candidateA = MathUtil.normalizeDegrees(roundedDegrees+j*preferredSign);
				int candidateB = MathUtil.normalizeDegrees(roundedDegrees-j*preferredSign);
				if(getCandidate(i, candidateA).getPriority()<=priority){
					Vector endPoint = position.add(velocityVector[i-1][candidateA]);
					if(!Geometry.segmentCircleIntersection(position, endPoint, target, innerBuffer)) {
						return new ThrustPlan(i, candidateA);
					}
				}
				if(getCandidate(i, candidateB).getPriority()<=priority){
					Vector endPoint = position.add(velocityVector[i-1][candidateB]);
					if(!Geometry.segmentCircleIntersection(position, endPoint, target, innerBuffer)) {
						return new ThrustPlan(i, candidateB);
					}
				}
			}
		}
		//Still case
		if(getStillCandidate().getPriority()<=priority) {
			return new ThrustPlan(0, 0);
		}
		return null;
	}
}
