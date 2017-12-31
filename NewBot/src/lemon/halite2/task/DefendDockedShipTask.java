package lemon.halite2.task;

import java.util.HashMap;
import java.util.Map;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Move;
import hlt.Planet;
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
	private static final double DETECTION_BUFFER = GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS+GameConstants.MAX_SPEED+1.0;
	private static final double MOVE_BUFFER = DETECTION_BUFFER; //1.0 for uncertainty
	private Map<Integer, Double> cache;
	private Vector defendPoint;
	private Ship enemyShip;
	private double enemyDirection;
	private double enemyDistance;
	private boolean activate;
	private int counter;
	public DefendDockedShipTask(Ship enemyShip) {
		this.cache = new HashMap<Integer, Double>();
		this.enemyShip = enemyShip;
		double closestDistanceSquared = Double.MAX_VALUE;
		for(Ship ship: GameMap.INSTANCE.getMyPlayer().getShips()) {
			if(ship.getDockingStatus()==DockingStatus.UNDOCKED) {
				continue;
			}
			double distanceSquared = ship.getPosition().getDistanceSquared(enemyShip.getPosition());
			if(distanceSquared<closestDistanceSquared) {
				defendPoint = ship.getPosition();
				closestDistanceSquared = distanceSquared;
			}
		}
		for(Planet planet: GameMap.INSTANCE.getPlanets()) {
			if(!planet.isOwned()) {
				continue;
			}
			if(planet.getOwner()!=GameMap.INSTANCE.getMyPlayerId()) {
				continue;
			}
			Vector spawnPoint = planet.getPosition().addPolar(planet.getRadius()+GameConstants.SPAWN_RADIUS, planet.getPosition().getDirectionTowards(GameMap.INSTANCE.getCenterPosition()));
			double distanceSquared = spawnPoint.getDistanceSquared(enemyShip.getPosition());
			if(distanceSquared<closestDistanceSquared) {
				defendPoint = spawnPoint;
				closestDistanceSquared = distanceSquared;
			}
		}
		if(defendPoint!=null){
			this.enemyDirection = this.enemyShip.getPosition().getDirectionTowards(defendPoint);
			this.enemyDistance = Math.sqrt(closestDistanceSquared);
			this.activate = true;
		}
		this.counter = 0;
	}
	@Override
	public void accept(Ship ship) {
		TaskManager.INSTANCE.addHandledEnemies(enemyShip.getId());
		counter++;
		activate = false;
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		Vector propagation = enemyShip.getPosition().addPolar(Math.min(enemyDistance, MOVE_BUFFER), enemyDirection);
		double intersectionDistance = calculateIntersectionDistance(propagation, enemyDirection, ship.getPosition())+1.0;
		Vector intersection;
		if(intersectionDistance>enemyDistance-MOVE_BUFFER){
			intersection = defendPoint;
		}else{
			intersection = enemyShip.getPosition().addPolar(intersectionDistance, enemyDirection);
		}
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
	public double getScore(Ship ship, double minScore) {
		if(!activate){
			return -Double.MAX_VALUE;
		}
		if(counter==0&&TaskManager.INSTANCE.containsHandledEnemies(enemyShip.getId())) {
			return -Double.MAX_VALUE;
		}
		if(!cache.containsKey(ship.getId())) {
			double intersectionDistance = calculateIntersectionDistance(enemyShip.getPosition(), enemyDirection, ship.getPosition());
			if(intersectionDistance<=enemyDistance) {
				cache.put(ship.getId(), -intersectionDistance*intersectionDistance);
			}else {
				cache.put(ship.getId(), -ship.getPosition().getDistanceSquared(defendPoint));
			}
		}
		return cache.get(ship.getId());
	}
	public double calculateIntersectionDistance(Vector enemyPosition, double enemyDirection, Vector shipPosition){
		double theta = MathUtil.angleBetweenRadians(enemyDirection, enemyPosition.getDirectionTowards(shipPosition));
		if(theta>=Math.PI/2) {
			return Double.MAX_VALUE;
		}
		double distance = shipPosition.getDistanceTo(enemyPosition);
		return Math.sin(theta)/(Math.sin(Math.PI-2*theta)/distance);
	}
}
