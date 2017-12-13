package lemon.halite2.pathfinding;

import java.util.function.Predicate;

import hlt.Vector;
import hlt.ThrustPlan;
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
		this.obstacles = obstacles;
	}
	public Vector getPosition() {
		return position;
	}
	public double getBuffer() {
		return buffer;
	}
	public Obstacle getStillCandidate(ObstacleType... types) {
		for(ObstacleType type: types) {
			Obstacle candidate = this.getStillCandidate(type);
			if(candidate!=null) {
				return candidate;
			}
		}
		return null;
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
	public Obstacle getCandidate(ThrustPlan plan, ObstacleType... types) {
		for(ObstacleType type: types) {
			Obstacle candidate = this.getCandidate(plan, type);
			if(candidate!=null) {
				return candidate;
			}
		}
		return null;
	}
	public Obstacle getCandidate(int thrust, int angle, ObstacleType... types) {
		for(ObstacleType type: types) {
			Obstacle candidate = this.getCandidate(thrust, angle, type);
			if(candidate!=null) {
				return candidate;
			}
		}
		return null;
	}
	public Obstacle getCandidate(ThrustPlan plan, ObstacleType type) {
		return getCandidate(plan.getThrust(), plan.getAngle(), type);
	}
	public Obstacle getCandidate(int thrust, int angle, ObstacleType type) {
		if(thrust==0){
			return getStillCandidate(type);
		}else{
			Vector velocity = velocityVector[thrust-1][angle];
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
}
