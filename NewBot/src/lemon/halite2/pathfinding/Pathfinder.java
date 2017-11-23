package lemon.halite2.pathfinding;

import java.util.function.Predicate;

import hlt.Position;
import hlt.RoundPolicy;
import hlt.ThrustPlan;
import lemon.halite2.util.Geometry;
import lemon.halite2.util.MathUtil;

public class Pathfinder {
	private Position position;
	private double buffer;
	
	public static Position[][] velocityVector;
	
	private static final Obstacle NO_CONFLICT = new Obstacle() {
		@Override
		public boolean willCollide(Position position, Position velocity, double buffer) {
			return false;
		}
		@Override
		public int getPriority() {
			return 0;
		}
	};
	
	private Obstacle stillCandidate;
	private Obstacle[][] candidates;
	//null = not checked; stores blame
	
	private Obstacles obstacles;
	private Predicate<Obstacle> exceptions;
	
	public static void init() {
		velocityVector = new Position[7][MathUtil.TAU_DEGREES];
		for(int i=0;i<velocityVector.length;++i) {
			for(int j=0;j<velocityVector[0].length;++j) {
				double radians = Math.toRadians(j);
				velocityVector[i][j] = new Position((i+1)*Math.cos(radians), (i+1)*Math.sin(radians));
			}
		}
	}
	public Pathfinder(Position position, double buffer, Obstacles obstacles, Predicate<Obstacle> exceptions) {
		this.position = position;
		this.buffer = buffer;
		this.candidates = new Obstacle[7][MathUtil.TAU_DEGREES]; //1-7; 0 magnitude = special case to save memory
		this.obstacles = obstacles;
		this.exceptions = exceptions;
	}
	public Position getPosition() {
		return position;
	}
	public void clearObstacles(int priority) {
		if(stillCandidate.getPriority()<=priority) {
			stillCandidate = null;
		}
		for(int i=0;i<candidates.length;++i) {
			for(int j=0;j<candidates[0].length;++j) {
				if(candidates[i][j].getPriority()<=priority) {
					candidates[i][j] = null;
				}
			}
		}
	}
	public Obstacle getCandidate(ThrustPlan plan) {
		return getCandidate(plan.getThrust(), plan.getAngle());
	}
	public Obstacle getCandidate(int thrust, int angle) {
		if(thrust==0) {
			if(stillCandidate==null){
				evaluateStillCandidate();
			}
			return stillCandidate;
		}
		if(candidates[thrust-1][angle]==null){
			evaluateCandidate(thrust, angle);
		}
		return candidates[thrust-1][angle];
	}
	public void evaluateStillCandidate(){
		for(Obstacle obstacle: obstacles.getObstacles()) {
			if(exceptions.test(obstacle)) {
				continue;
			}
			if(obstacle.willCollide(position, Position.ZERO, buffer)){
				stillCandidate = obstacle;
				return;
			}
		}
		if(stillCandidate==null) {
			stillCandidate = NO_CONFLICT;
		}
	}
	public void evaluateCandidate(int thrust, int angle){
		Position velocity = velocityVector[thrust-1][angle];
		for(Obstacle obstacle: obstacles.getObstacles()) {
			if(exceptions.test(obstacle)) {
				continue;
			}
			if(obstacle.willCollide(position, velocity, buffer)) {
				candidates[thrust-1][angle] = obstacle;
				return;
			}
		}
		if(candidates[thrust-1][angle]==null) {
			candidates[thrust-1][angle] = NO_CONFLICT;
		}
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
				if(getCandidate(i, candidateA)==NO_CONFLICT){
					if(!Geometry.segmentCircleIntersection(position, position.add(velocityVector[i-1][candidateA]), target, buffer)) {
						return new ThrustPlan(i, candidateA);
					}
				}
				if(getCandidate(i, candidateB)==NO_CONFLICT){
					if(!Geometry.segmentCircleIntersection(position, position.add(velocityVector[i-1][candidateB]), target, buffer)) {
						return new ThrustPlan(i, candidateB);
					}
				}
			}
		}
		//Still case
		if(stillCandidate==NO_CONFLICT) {
			return new ThrustPlan(0, 0);
		}
		return null;
	}
}
