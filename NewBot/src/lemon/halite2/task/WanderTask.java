package lemon.halite2.task;

import java.util.Random;

import hlt.GameMap;
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

public class WanderTask implements Task {
	private Random random;
	public WanderTask() {
		long hash = GameMap.INSTANCE.getWidth();
		hash = hash*500+GameMap.INSTANCE.getHeight();
		hash = hash*50+GameMap.INSTANCE.getPlanets().size();
		hash = hash*4+GameMap.INSTANCE.getMyPlayerId();
		hash = hash*4+GameMap.INSTANCE.getPlayers().size();
		hash = hash*300+GameMap.INSTANCE.getTurnNumber();
		random = new Random(hash);
	}
	@Override
	public void accept(Ship ship) {
		
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		double bestDistance = Double.MAX_VALUE;
		Planet bestPlanet = null;
		for(Planet planet: GameMap.INSTANCE.getPlanets()) {
			double distance = ship.getPosition().getDistanceTo(planet.getPosition())-planet.getRadius();
			if(distance<bestDistance) {
				bestDistance = distance;
				bestPlanet = planet;
			}
		}
		if(bestPlanet==null) {
			return new ThrustMove(ship.getId(), new ThrustPlan(0, 0));
		}
		double direction = ship.getPosition().getDirectionTowards(bestPlanet.getPosition()
				.addPolar(bestPlanet.getRadius(), bestPlanet.getPosition().getDirectionTowards(ship.getPosition())));
		double directionDegrees = Math.toDegrees(direction);
		int roundedDegrees = RoundPolicy.ROUND.applyDegrees(direction);
		int preferredSign = directionDegrees-((int)directionDegrees)<0.5?1:-1;
		//try candidates
		for(int i=7;i>0;--i) {
			for(int j=0;j<=MathUtil.PI_DEGREES;++j) {
				int candidateA = MathUtil.normalizeDegrees(roundedDegrees+j*preferredSign);
				int candidateB = MathUtil.normalizeDegrees(roundedDegrees-j*preferredSign);
				if(pathfinder.getCandidate(i, candidateA, ObstacleType.PERMANENT, ObstacleType.ENEMY)==null) {
					Obstacle obstacle = pathfinder.getCandidate(i, candidateA, ObstacleType.UNCERTAIN);
					if(obstacle==null) {
						return new ThrustMove(ship.getId(), new ThrustPlan(i, candidateA));
					}else {
						blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
						return null;
					}
				}
				if(pathfinder.getCandidate(i, candidateB, ObstacleType.PERMANENT, ObstacleType.ENEMY)==null) {
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
		if(pathfinder.getStillCandidate(ObstacleType.ENEMY)==null) {
			Obstacle obstacle = pathfinder.getStillCandidate(ObstacleType.UNCERTAIN);
			if(obstacle==null) {
				return new ThrustMove(ship.getId(), new ThrustPlan(0, 0));
			}else {
				blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
				return null;
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
		Obstacle obstacle = pathfinder.getStillCandidate(ObstacleType.UNCERTAIN);
		if(obstacle==null) {
			return new ThrustMove(ship.getId(), new ThrustPlan(0, 0));
		}else {
			blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
			return null;
		}
	}
	@Override
	public double getScore(Ship ship, double minScore) {
		return -Double.MAX_VALUE+Math.ulp(Double.MAX_VALUE);
	}
}
