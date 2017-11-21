package lemon.halite2.util;

import java.util.Map.Entry;

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
	
	private List<Obstacle> obstacles;
	
	public static void init() {
		velocityVector = new Position[7][MathUtil.TAU_DEGREES];
		for(int i=0;i<velocityVector.length;++i) {
			for(int j=0;j<velocityVector[0].length;++j) {
				double radians = Math.toRadians(j);
				velocityVector[i][j] = new Position((i+1)*Math.cos(radians), (i+1)*Math.sin(radians));
			}
		}
	}
	public Pathfinder(Position position, double buffer) {
		this.position = position;
		this.buffer = buffer;
		this.candidates = new int[7][MathUtil.TAU_DEGREES]; //1-7; 0 magnitude = special case to save memory
		for(int i=0;i<candidates.length;++i) {
			for(int j=0;j<candidates[0].length;++j) {
				candidates[i][j] = UNCHECKED;
			}
		}
		obstacles = new ArrayList<Obstacle>();
	}
	public Position getPosition() {
		return position;
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
		for(Obstacle obstacle: obstacles){
			double distanceSquared = (obstacle.getCircle().getRadius()+buffer)*(obstacle.getCircle().getRadius()+buffer);
			if(obstacle.getThrustPlan()==null||obstacle.getThrustPlan().getThrust()==0){  //Static or Uncertain
				if(obstacle.getCircle().getPosition().getDistanceSquared(position)<=distanceSquared){
					if(obstacle.getId()==-1){
						stillCandidate = CONFLICT;
						return;
					}else{
						stillCandidate = obstacle.getId();
					}
				}
			}else{ //Dynamic Obstacle
				if(Geometry.segmentPointDistanceSquared(obstacle.getCircle().getPosition(),obstacle.getCircle().getPosition().add(
						velocityVector[obstacle.getThrustPlan().getThrust()-1][obstacle.getThrustPlan().getAngle()]), position)<=distanceSquared){
					stillCandidate = CONFLICT;
					return;
				}
			}
		}
		if(stillCandidate==UNCHECKED){
			stillCandidate = NO_CONFLICT;
		}
	}
	public void evaluateCandidate(int thrust, int angle){
		for(Obstacle obstacle: obstacles){
			
		}
	}
	public void resolveStaticObstacles() {
		for(Circle circle: Obstacles.getStaticObstacles()) {
			resolveStaticObstacle(circle);
		}
	}
	public void resolveDynamicObstacles() {
		for(Entry<Circle, ThrustPlan> entry: Obstacles.getDynamicObstacles().entrySet()) {
			resolveDynamicObstacle(entry.getKey(), entry.getValue());
		}
	}
	public void resolveUncertainObstacles(int exception) {
		for(Entry<Integer, Circle> entry: Obstacles.getUncertainObstacles().entrySet()) {
			if(exception==entry.getKey()) {
				continue;
			}
			resolveUncertainObstacle(entry.getValue(), entry.getKey());
		}
	}
	public void resolveStaticObstacle(Circle circle) {
		//resolve still candidate
		if(stillCandidate!=CONFLICT&&position.getDistanceSquared(circle.getPosition())<=
				(buffer+circle.getRadius())*(buffer+circle.getRadius())) {
			stillCandidate = CONFLICT;
		}
		//resolve all other candidates
		for(int i=0;i<candidates.length;++i) {
			for(int j=0;j<candidates[0].length;++j) {
				if(candidates[i][j]!=CONFLICT&&Geometry.segmentCircleIntersection(position, position.add(velocityVector[i][j]),
						circle.getPosition(), circle.getRadius()+buffer)) {
					candidates[i][j] = CONFLICT;
				}
			}
		}
	}
	public void resolveDynamicObstacle(Circle circle, ThrustPlan plan) {
		if(plan.getThrust()==0) {
			resolveStaticObstacle(circle);
			return;
		}
		Position velocity = velocityVector[plan.getThrust()-1][plan.getAngle()];
		//resolve still candidate
		if(stillCandidate!=CONFLICT&&Geometry.segmentCircleIntersection(circle.getPosition(),
				circle.getPosition().add(velocity), position, buffer+circle.getRadius())) {
			stillCandidate = CONFLICT;
		}
		//resolve all other candidates
		for(int i=0;i<candidates.length;++i) {
			for(int j=0;j<candidates[0].length;++j) {
				if(candidates[i][j]!=CONFLICT&&checkCollisions(circle.getPosition(), position, velocity, velocityVector[i][j],
						circle.getRadius()+buffer)) {
					candidates[i][j] = CONFLICT;
				}
			}
		}
	}
	public void clearUncertainObstacles() {
		if(stillCandidate>=0) {
			stillCandidate = NO_CONFLICT;
		}
		for(int i=0;i<candidates.length;++i) {
			for(int j=0;j<candidates[0].length;++j) {
				if(candidates[i][j]>=0) {
					candidates[i][j] = NO_CONFLICT;
				}
			}
		}
	}
	public void resolveUncertainObstacle(Circle circle, int id) {
		//resolve still candidate
		if(stillCandidate==NO_CONFLICT&&position.getDistanceSquared(circle.getPosition())<=
				(buffer+circle.getRadius())*(buffer+circle.getRadius())) {
			stillCandidate = id;
		}
		//resolve all other candidates
		for(int i=0;i<candidates.length;++i) {
			for(int j=0;j<candidates[0].length;++j) {
				if(candidates[i][j]==NO_CONFLICT&&Geometry.segmentCircleIntersection(position,
						position.add(velocityVector[i][j]), circle.getPosition(), buffer+circle.getRadius())) {
					candidates[i][j] = id;
				}
			}
		}
	}
	public static boolean checkCollisions(Position a, Position b, Position velocityA, Position velocityB, double buffer) {
		double time = MathUtil.getMinTime(a, b, velocityA, velocityB);
		time = Math.max(0, Math.min(1, time)); //Clamp between 0 and 1
		return buffer*buffer>=MathUtil.getDistanceSquared(a, b, velocityA, velocityB, time);
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
