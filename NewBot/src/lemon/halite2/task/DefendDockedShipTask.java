package lemon.halite2.task;

import java.util.HashMap;
import java.util.Map;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Move;
import hlt.RoundPolicy;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustPlan;
import hlt.Vector;
import hlt.Ship.DockingStatus;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.MathUtil;

public class DefendDockedShipTask implements Task {
	private Ship enemyShip;
	private Ship dockedShip;
	private double enemyDirection;
	private boolean activate;
	private Vector intersection;
	private Map<Integer, Double> intersectionDistances;
	public DefendDockedShipTask(Ship enemyShip) {
		this.enemyShip = enemyShip;
		this.intersectionDistances = new HashMap<Integer, Double>();
		double closestDistanceSquared = Double.MAX_VALUE;
		for(Ship ship: GameMap.INSTANCE.getMyPlayer().getShips()) {
			if(ship.getDockingStatus()==DockingStatus.UNDOCKED) {
				continue;
			}
			double distanceSquared = ship.getPosition().getDistanceSquared(enemyShip.getPosition());
			if(distanceSquared<closestDistanceSquared) {
				dockedShip = ship;
				closestDistanceSquared = distanceSquared;
			}
		}
		this.activate = dockedShip!=null;
		if(dockedShip!=null) {
			enemyDirection = enemyShip.getPosition().getDirectionTowards(dockedShip.getPosition());
		}
	}
	@Override
	public void accept(Ship ship) {
		intersection = enemyShip.getPosition().addPolar(intersectionDistances.get(ship.getId()), enemyDirection);
		activate = false;
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		double direction = ship.getPosition().getDirectionTowards(intersection);
		double directionDegrees = Math.toDegrees(direction);
		int roundedDegrees = RoundPolicy.ROUND.applyDegrees(direction);
		int preferredSign = directionDegrees-((int)directionDegrees)<0.5?1:-1;
		//try greediest
		if(ship.getPosition().getDistanceSquared(intersection)<=(2*GameConstants.SHIP_RADIUS+GameConstants.MAX_SPEED)*
				(2*GameConstants.SHIP_RADIUS+GameConstants.MAX_SPEED)){
			int bestCandidateThrust = 0;
			int bestCandidateAngle = 0;
			double bestCandidateDistanceSquared = ship.getPosition().getDistanceSquared(intersection);
			for(int i=1;i<=7;++i) {
				for(int j=0;j<MathUtil.TAU_DEGREES;++j) {
					if(pathfinder.getCandidate(i, j, ObstacleType.PERMANENT)==null) {
						double distanceSquared = ship.getPosition().add(Pathfinder.velocityVector[i-1][j]).getDistanceSquared(intersection);
						if(distanceSquared<bestCandidateDistanceSquared) {
							bestCandidateDistanceSquared = distanceSquared;
							bestCandidateThrust = i;
							bestCandidateAngle = j;
						}
					}
				}
			}
			Obstacle obstacle = pathfinder.getCandidate(bestCandidateThrust, bestCandidateAngle, ObstacleType.UNCERTAIN);
			if(obstacle==null) {
				return new ThrustMove(ship.getId(), new ThrustPlan(bestCandidateThrust, bestCandidateAngle));
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
	public double getScore(Ship ship) {
		if(!activate) {
			return -Double.MAX_VALUE;
		}
		double theta = MathUtil.angleBetweenRadians(enemyDirection, enemyShip.getPosition().getDirectionTowards(ship.getPosition()));
		if(theta>=Math.PI/2) {
			return -Double.MAX_VALUE;
		}
		double distance = ship.getPosition().getDistanceTo(enemyShip.getPosition());
		double intersectionDistance = Math.sin(theta)/(Math.sin(Math.PI-2*theta)/distance);
		intersectionDistances.put(ship.getId(), intersectionDistance);
		return intersectionDistance;
	}
}
