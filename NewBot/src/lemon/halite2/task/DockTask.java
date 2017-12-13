package lemon.halite2.task;

import java.util.HashSet;
import java.util.Set;

import hlt.DockMove;
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

public class DockTask implements Task {
	private Planet planet;
	private double innerBufferSquared;
	private double outerBufferSquared;
	private int dockSpaces;
	private Set<Integer> acceptedShips;
	public DockTask(Planet planet){
		this.planet = planet;
		this.innerBufferSquared = (planet.getRadius()+GameConstants.DOCK_RADIUS);
		this.outerBufferSquared = (innerBufferSquared+GameConstants.MAX_SPEED);
		innerBufferSquared = innerBufferSquared*innerBufferSquared;
		outerBufferSquared = outerBufferSquared*outerBufferSquared;
		acceptedShips = new HashSet<Integer>();
		if(planet.isOwned()) {
			if(planet.getOwner()==GameMap.INSTANCE.getMyPlayerId()) {
				this.dockSpaces = planet.getDockingSpots()-planet.getDockedShips().size();
			}
		}else {
			this.dockSpaces = planet.getDockingSpots();
		}
	}
	@Override
	public void accept(Ship ship) {
		acceptedShips.add(ship.getId());
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
			for(int i=7;i>0;--i) {
				for(int j=0;j<MathUtil.PI_DEGREES;++j) {
					int candidateA = MathUtil.normalizeDegrees(roundedDegrees+j*preferredSign);
					int candidateB = MathUtil.normalizeDegrees(roundedDegrees-j*preferredSign);
					if(ship.getPosition().add(Pathfinder.velocityVector[i-1][candidateA]).getDistanceSquared(planet.getPosition())<=innerBufferSquared) {
						if(pathfinder.getCandidate(i, candidateA, ObstacleType.PERMANENT, ObstacleType.ENEMY)==null) {
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
			}
		}
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
	public double getScore(Ship ship) {
		if(acceptedShips.size()>=dockSpaces) {
			return -Double.MAX_VALUE;
		}
		Vector projectedLanding = planet.getPosition().addPolar(planet.getRadius()+0.65,
				planet.getPosition().getDirectionTowards(ship.getPosition()));
		double closestFriendlyDistanceSquared = Double.MAX_VALUE;
		double closestEnemyDistanceSquared = Double.MAX_VALUE;
		for(Ship s: GameMap.INSTANCE.getShips()) {
			if(s.equals(ship)) {
				continue;
			}
			if(acceptedShips.contains(s.getId())) {
				continue;
			}
			if(s.getOwner()==GameMap.INSTANCE.getMyPlayerId()&&
					s.getDockingStatus()==DockingStatus.UNDOCKED) {
				double distanceSquared = projectedLanding.getDistanceSquared(s.getPosition());
				if(distanceSquared<closestFriendlyDistanceSquared) {
					closestFriendlyDistanceSquared = distanceSquared;
				}
			}else {
				if(s.getDockingStatus()==DockingStatus.UNDOCKED){
					double distanceSquared = projectedLanding.getDistanceSquared(s.getPosition());
					if(distanceSquared<closestEnemyDistanceSquared) {
						closestEnemyDistanceSquared = distanceSquared;
					}
				}else{
					double distanceSquared = projectedLanding.getDistanceTo(s.getPosition())+
							GameConstants.UNDOCK_TURNS*GameConstants.MAX_SPEED;
					distanceSquared = distanceSquared*distanceSquared;
					if(distanceSquared<closestEnemyDistanceSquared) {
						closestEnemyDistanceSquared = distanceSquared;
					}
				}
			}
		}
		//Project ships that would be created in the future
		for(Planet planet: GameMap.INSTANCE.getPlanets()){
			if(!planet.isOwned()){
				continue;
			}
			//calculate number of turns it would take to create a new ship
			int remainingProduction = GameConstants.TOTAL_PRODUCTION-planet.getCurrentProduction();
			int[] dockedProgress = new int[planet.getDockedShips().size()+1];
			int turns = 0;
			for(int i=0;i<dockedProgress.length-1;++i) {
				Ship s = GameMap.INSTANCE.getShip(planet.getOwner(), planet.getDockedShips().get(i));
				if(s.getDockingStatus()==DockingStatus.DOCKED) {
					dockedProgress[i] = 0;
				}else if(s.getDockingStatus()==DockingStatus.DOCKING) {
					dockedProgress[i] = s.getDockingProgress();
				}
			}
			dockedProgress[dockedProgress.length-1] = GameConstants.DOCK_TURNS+
					(int)Math.ceil(((double)(ship.getPosition().getDistanceTo(planet.getPosition())-planet.getRadius()))/7.0);
			while(remainingProduction>0) {
				for(int i=0;i<dockedProgress.length;++i) {
					if(dockedProgress[i]>0) {
						dockedProgress[i]--;
					}else {
						remainingProduction-=GameConstants.BASE_PRODUCTION;
					}
				}
			}
			Vector projection = planet.getPosition().addPolar(planet.getRadius()+GameConstants.SPAWN_RADIUS,
					planet.getPosition().getDirectionTowards(GameMap.INSTANCE.getCenterPosition()));
			double distanceSquared = projectedLanding.getDistanceTo(projection)+turns*GameConstants.MAX_SPEED;
			distanceSquared = distanceSquared*distanceSquared;
			if(planet.getOwner()==GameMap.INSTANCE.getMyPlayerId()){
				if(distanceSquared<closestFriendlyDistanceSquared){
					closestFriendlyDistanceSquared = distanceSquared;
				}
			}else{
				if(distanceSquared<closestEnemyDistanceSquared){
					closestEnemyDistanceSquared = distanceSquared;
				}
			}
		}
		if(closestEnemyDistanceSquared!=Double.MAX_VALUE&&closestEnemyDistanceSquared-120.0<=closestFriendlyDistanceSquared) {
			return -Double.MAX_VALUE;
		}
		double score = ship.getPosition().getDistanceTo(planet.getPosition())-planet.getRadius();
		score = score*score;
		return -score;
	}
}
