package lemon.halite2.task;

import hlt.DockMove;
import hlt.GameConstants;
import hlt.Move;
import hlt.Planet;
import hlt.RoundPolicy;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustPlan;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.MathUtil;

public class DockTask implements Task {
	private Planet planet;
	private double innerBufferSquared;
	private double outerBufferSquared;
	public DockTask(Planet planet){
		this.planet = planet;
		this.innerBufferSquared = (planet.getRadius()+GameConstants.DOCK_RADIUS);
		this.outerBufferSquared = (innerBufferSquared+GameConstants.MAX_SPEED);
		innerBufferSquared = innerBufferSquared*innerBufferSquared;
		outerBufferSquared = outerBufferSquared*outerBufferSquared;
	}
	@Override
	public void accept(Ship ship) {
		if(ship.getPosition().getDistanceSquared(planet.getPosition())<=
				(planet.getRadius()+GameConstants.DOCK_RADIUS+GameConstants.SHIP_RADIUS+GameConstants.MAX_SPEED)*
				(planet.getRadius()+GameConstants.DOCK_RADIUS+GameConstants.SHIP_RADIUS+GameConstants.MAX_SPEED)){
			//try greediest
			
		}
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		double distanceSquared = ship.getPosition().getDistanceSquared(planet.getPosition());
		if(distanceSquared<=innerBufferSquared) {
			return new DockMove(ship.getId(), planet.getId());
		}
		double direction = ship.getPosition().getDirectionTowards(planet.getPosition());
		double directionDegrees = Math.toDegrees(direction);
		int roundedDegrees = RoundPolicy.ROUND.applyDegrees(direction);
		int preferredSign = directionDegrees-((int)directionDegrees)<0.5?1:-1;
		if(distanceSquared<=outerBufferSquared) {
			//try greediest
			for(int i=1;i<=7;++i) {
				for(int j=0;j<MathUtil.PI_DEGREES;++j) {
					int candidateA = MathUtil.normalizeDegrees(roundedDegrees+j*preferredSign);
					int candidateB = MathUtil.normalizeDegrees(roundedDegrees-j*preferredSign);
					if(ship.getPosition().add(Pathfinder.velocityVector[i-1][candidateA]).getDistanceSquared(planet.getPosition())<=innerBufferSquared) {
						if(pathfinder.getCandidate(i, candidateA, ObstacleType.PERMANENT)==null) {
							Obstacle obstacle = pathfinder.getCandidate(i, candidateA, ObstacleType.UNCERTAIN);
							if(obstacle==null) {
								return new ThrustMove(ship.getId(), new ThrustPlan(i, candidateA));
							}else {
								blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
								return null;
							}
						}
					}
					if(ship.getPosition().add(Pathfinder.velocityVector[i-1][candidateB]).getDistanceSquared(planet.getPosition())<=innerBufferSquared) {
						if(pathfinder.getCandidate(i, candidateB, ObstacleType.PERMANENT)==null) {
							Obstacle obstacle = pathfinder.getCandidate(i, candidateB, ObstacleType.UNCERTAIN);
							if(obstacle==null) {
								return new ThrustMove(ship.getId(), new ThrustPlan(i, candidateB));
							}else {
								blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
								return null;
							}
						}
					}
				}
			}
		}
		//try candidates
		for(int i=7;i>0;--i) {
			for(int j=0;j<=MathUtil.PI_DEGREES;++j) {
				int candidateA = MathUtil.normalizeDegrees(roundedDegrees+j*preferredSign);
				int candidateB = MathUtil.normalizeDegrees(roundedDegrees-j*preferredSign);
				if(pathfinder.getCandidate(i, candidateA, ObstacleType.PERMANENT)==null) {
					Obstacle obstacle = pathfinder.getCandidate(i, candidateA, ObstacleType.UNCERTAIN);
					if(obstacle==null) {
						return new ThrustMove(ship.getId(), new ThrustPlan(i, candidateA));
					}else {
						blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
						return null;
					}
				}
				if(pathfinder.getCandidate(i, candidateB, ObstacleType.PERMANENT)==null) {
					Obstacle obstacle = pathfinder.getCandidate(i, candidateB, ObstacleType.UNCERTAIN);
					if(obstacle==null) {
						return new ThrustMove(ship.getId(), new ThrustPlan(i, candidateB));
					}else {
						blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
						return null;
					}
				}
			}
		}
		//try still candidate
		Obstacle obstacle = pathfinder.getStillCandidate(ObstacleType.UNCERTAIN);
		if(obstacle==null) {
			return new ThrustMove(ship.getId(), new ThrustPlan(0, 0));
		}else {
			blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
			return null;
		}
	}
	@Override
	public double getScore(Ship ship) {
		return -ship.getPosition().getDistanceSquared(planet.getPosition());
	}
}
