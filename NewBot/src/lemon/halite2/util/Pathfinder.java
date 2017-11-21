package lemon.halite2.util;

import hlt.Position;
import hlt.RoundPolicy;
import hlt.ThrustPlan;

public class Pathfinder {
	private Position position;
	private double buffer;
	
	public static final int UNCHECKED = -1;
	public static final int NO_CONFLICT = -2;
	public static final int CONFLICT = -3; // Planet, Ship that already moved
	//All positive integers represent uncertain; value = shipId to blame
	private static Position[][] velocityVector;
	
	private int stillCandidate;
	private int[][] candidates;
	
	private Obstacles obstacles;
	private int exception;
	
	public static void init() {
		velocityVector = new Position[7][MathUtil.TAU_DEGREES];
		for(int i=0;i<velocityVector.length;++i) {
			for(int j=0;j<velocityVector[0].length;++j) {
				double radians = Math.toRadians(j);
				velocityVector[i][j] = new Position((i+1)*Math.cos(radians), (i+1)*Math.sin(radians));
			}
		}
	}
	public Pathfinder(Position position, double buffer, Obstacles obstacles, int exception) {
		this.position = position;
		this.buffer = buffer;
		this.candidates = new int[7][MathUtil.TAU_DEGREES]; //1-7; 0 magnitude = special case to save memory
		for(int i=0;i<candidates.length;++i) {
			for(int j=0;j<candidates[0].length;++j) {
				candidates[i][j] = UNCHECKED;
			}
		}
		this.obstacles = obstacles;
		this.exception = exception;
	}
	public Position getPosition() {
		return position;
	}
	public void clearUncertainObstacles() {
		if(stillCandidate>=0) {
			stillCandidate = UNCHECKED;
		}
		for(int i=0;i<candidates.length;++i) {
			for(int j=0;j<candidates[0].length;++j) {
				if(candidates[i][j]>=0) {
					candidates[i][j] = UNCHECKED;
				}
			}
		}
	}
	public int getCandidate(ThrustPlan plan) {
		return getCandidate(plan.getThrust(), plan.getAngle());
	}
	public int getCandidate(int thrust, int angle) {
		if(thrust==0) {
			if(stillCandidate==UNCHECKED){
				evaluateStillCandidate();
			}
			return stillCandidate;
		}
		if(candidates[thrust-1][angle]==UNCHECKED){
			evaluateCandidate(thrust, angle);
		}
		return candidates[thrust-1][angle];
	}
	public void evaluateStillCandidate(){
		for(Obstacle obstacle: obstacles.getObstacles()){
			double distanceSquared = (obstacle.getCircle().getRadius()+buffer)*(obstacle.getCircle().getRadius()+buffer);
			if(obstacle.getThrustPlan()==null||obstacle.getThrustPlan().getThrust()==0){  //Static
				if(obstacle.getCircle().getPosition().getDistanceSquared(position)<=distanceSquared){
					stillCandidate = CONFLICT;
					return;
				}
			}else{ //Dynamic Obstacle
				if(Geometry.segmentPointDistanceSquared(obstacle.getCircle().getPosition(), obstacle.getCircle().getPosition().add(
						velocityVector[obstacle.getThrustPlan().getThrust()-1][obstacle.getThrustPlan().getAngle()]), position)<=distanceSquared){
					stillCandidate = CONFLICT;
					return;
				}
			}
		}
		for(Obstacle obstacle: obstacles.getUncertainObstacles().values()) {
			if(obstacle.getId()==exception) {
				continue;
			}
			double distanceSquared = (obstacle.getCircle().getRadius()+buffer)*(obstacle.getCircle().getRadius()+buffer);
			if(obstacle.getCircle().getPosition().getDistanceSquared(position)<=distanceSquared){
				stillCandidate = obstacle.getId();
				return;
			}
		}
		if(stillCandidate==UNCHECKED){
			stillCandidate = NO_CONFLICT;
		}
	}
	public void evaluateCandidate(int thrust, int angle){
		Position velocity = velocityVector[thrust-1][angle];
		for(Obstacle obstacle: obstacles.getObstacles()){
			double distanceSquared = (obstacle.getCircle().getRadius()+buffer)*(obstacle.getCircle().getRadius()+buffer);
			if(obstacle.getThrustPlan()==null||obstacle.getThrustPlan().getThrust()==0) { //Static or Uncertain
				if(Geometry.segmentPointDistanceSquared(position, position.add(velocity), obstacle.getCircle().getPosition())<=distanceSquared){
					candidates[thrust-1][angle] = CONFLICT;
					return;
				}
			}else {
				if(getMinDistanceSquared(position, obstacle.getCircle().getPosition(), velocity,
						velocityVector[obstacle.getThrustPlan().getThrust()-1][obstacle.getThrustPlan().getAngle()])<=distanceSquared){
					candidates[thrust-1][angle] = CONFLICT;
					return;
				}
			}
		}
		for(Obstacle obstacle: obstacles.getUncertainObstacles().values()) {
			if(obstacle.getId()==exception) {
				continue;
			}
			double distanceSquared = (obstacle.getCircle().getRadius()+buffer)*(obstacle.getCircle().getRadius()+buffer);
			if(Geometry.segmentPointDistanceSquared(position, position.add(velocity), obstacle.getCircle().getPosition())<=distanceSquared){
				candidates[thrust-1][angle] = obstacle.getId();
				return;
			}
		}
		if(candidates[thrust-1][angle]==UNCHECKED) {
			candidates[thrust-1][angle] = NO_CONFLICT;
		}
	}
	public static double getMinDistanceSquared(Position a, Position b, Position velocityA, Position velocityB) {
		double time = MathUtil.getMinTime(a, b, velocityA, velocityB);
		time = Math.max(0, Math.min(1, time)); //Clamp between 0 and 1
		return MathUtil.getDistanceSquared(a, b, velocityA, velocityB, time);
	}
	public ThrustPlan getGreedyPlan(Position target, double buffer) {
		double direction = position.getDirectionTowards(target);
		double directionDegrees = Math.toDegrees(direction);
		int roundedDegrees = RoundPolicy.ROUND.applyDegrees(direction);
		int preferredSign = directionDegrees-((int)directionDegrees)<0.5?1:-1; //opposite because roundedDegrees is rounded already
		//General Case
		for(int i=7;i>=1;--i){ //magnitude
			for(int j=0;j<=MathUtil.PI_DEGREES;++j){ //offset
				int candidateA = MathUtil.normalizeDegrees(roundedDegrees+j*preferredSign);
				int candidateB = MathUtil.normalizeDegrees(roundedDegrees-j*preferredSign);
				if(getCandidate(i, candidateA)!=CONFLICT){
					if(!Geometry.segmentCircleIntersection(position, position.add(velocityVector[i-1][candidateA]), target, buffer)) {
						return new ThrustPlan(i, candidateA);
					}
				}
				if(getCandidate(i, candidateB)!=CONFLICT){
					if(!Geometry.segmentCircleIntersection(position, position.add(velocityVector[i-1][candidateB]), target, buffer)) {
						return new ThrustPlan(i, candidateB);
					}
				}
			}
		}
		//Still case
		if(stillCandidate!=CONFLICT) {
			return new ThrustPlan(0, 0);
		}
		return null;
	}
}
