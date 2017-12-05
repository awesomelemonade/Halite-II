package lemon.halite2.task;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Move;
import hlt.RoundPolicy;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustPlan;
import hlt.Ship.DockingStatus;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.MathUtil;

public class DefendDockedShipTask implements Task {
	private static final double DETECTION_RADIUS_SQUARED = (2*GameConstants.SHIP_RADIUS+5*GameConstants.MAX_SPEED)*
			(2*GameConstants.SHIP_RADIUS+5*GameConstants.MAX_SPEED);
	private Ship ship;
	private int counter;
	private double closestDistanceSquared;
	public DefendDockedShipTask(Ship ship) {
		this.ship = ship;
		this.closestDistanceSquared = Double.MAX_VALUE;
		int enemyShipCount = 0;
		for(Ship s: GameMap.INSTANCE.getShips()) {
			if(s.getOwner()==GameMap.INSTANCE.getMyPlayerId()) {
				if(s.getDockingStatus()==DockingStatus.UNDOCKED) {
					if(s.getPosition().getDistanceSquared(ship.getPosition())<=DETECTION_RADIUS_SQUARED) {
						enemyShipCount--;
					}
				}
			}else {
				double distanceSquared = s.getPosition().getDistanceSquared(ship.getPosition());
				if(distanceSquared<=DETECTION_RADIUS_SQUARED) {
					if(distanceSquared<closestDistanceSquared) {
						distanceSquared = closestDistanceSquared;
					}
					enemyShipCount++;
				}
			}
		}
		this.counter = enemyShipCount;
	}
	@Override
	public void accept(Ship ship) {
		counter--;
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		double direction = ship.getPosition().getDirectionTowards(this.ship.getPosition());
		double directionDegrees = Math.toDegrees(direction);
		int roundedDegrees = RoundPolicy.ROUND.applyDegrees(direction);
		int preferredSign = directionDegrees-((int)directionDegrees)<0.5?1:-1;
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
	public double getScore(Ship ship) {
		if(counter>0) {
			return -(ship.getPosition().getDistanceSquared(this.ship.getPosition())*0.6)-closestDistanceSquared*1.2;
		}else {
			return -Double.MAX_VALUE;
		}
	}
}
