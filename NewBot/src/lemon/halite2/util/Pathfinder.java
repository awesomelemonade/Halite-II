package lemon.halite2.util;

import hlt.Position;
import hlt.ThrustPlan;

public class Pathfinder {
	private Position position;
	private double buffer;
	
	public static final int NO_CONFLICT = -1;
	public static final int CONFLICT = -2; // Planet, Ship that already moved
	//All positive integers represent uncertain; value = shipId to blame
	private int stillCandidate;
	private int[][] candidates;
	
	private static Position[][] velocityVector;
	
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
				candidates[i][j] = NO_CONFLICT;
			}
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
	public void resolveDynamicObstacle(Circle circle, ThrustPlan plan, boolean[] directions) {
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
	public void resolveUncertainObstacle(Position position, double buffer, int shipId) {
		//resolve still candidate
		if(stillCandidate==NO_CONFLICT&&this.position.getDistanceSquared(position)<=(this.buffer+buffer)*(this.buffer+buffer)) {
			stillCandidate = shipId;
		}
		//resolve all other candidates
		for(int i=0;i<candidates.length;++i) {
			for(int j=0;j<candidates[0].length;++j) {
				if(candidates[i][j]==NO_CONFLICT&&Geometry.segmentCircleIntersection(this.position,
						this.position.add(velocityVector[i][j]), position, this.buffer+buffer)) {
					candidates[i][j] = shipId;
				}
			}
		}
	}
	public static boolean checkCollisions(Position a, Position b, Position velocityA, Position velocityB, double buffer) {
		double time = MathUtil.getMinTime(a, b, velocityA, velocityB);
		time = Math.max(0, Math.min(1, time)); //Clamp between 0 and 1
		return buffer*buffer>=MathUtil.getDistanceSquared(a, b, velocityA, velocityB, time);
	}
}
