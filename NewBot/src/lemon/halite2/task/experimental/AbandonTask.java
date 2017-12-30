package lemon.halite2.task.experimental;

import hlt.GameMap;
import hlt.Move;
import hlt.Planet;
import hlt.RoundPolicy;
import hlt.Ship;
import hlt.Ship.DockingStatus;
import hlt.ThrustMove;
import hlt.ThrustPlan;
import hlt.Vector;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.task.BlameMap;
import lemon.halite2.task.Task;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.MathUtil;

public class AbandonTask implements Task {
	private static final int NUM_PLAYERS = 4;
	private static final double PLANET_FACTOR = 0.75;
	private static final double SHIP_FACTOR = 10;
	private static boolean abandon = false;
	public AbandonTask() {
		if(GameMap.INSTANCE.getPlayers().size()==NUM_PLAYERS) { //Only abandon in 4 player games
			abandon = false;
			int[] planetCount = new int[NUM_PLAYERS];
			int[] shipCount = new int[NUM_PLAYERS];
			for(Planet planet: GameMap.INSTANCE.getPlanets()) {
				if(planet.isOwned()) {
					planetCount[planet.getOwner()]++;
				}
			}
			for(Ship ship: GameMap.INSTANCE.getShips()) {
				shipCount[ship.getOwner()]++;
			}
			for(int i=0;i<NUM_PLAYERS;++i) {
				if(i==GameMap.INSTANCE.getMyPlayerId()) { //Don't abandon if you're winning!
					continue;
				}
				if(((double)planetCount[i])/((double)GameMap.INSTANCE.getPlanets().size())>PLANET_FACTOR) {
					abandon = true;
					return;
				}
				if(((double)shipCount[i])/((double)shipCount[GameMap.INSTANCE.getMyPlayerId()])>=SHIP_FACTOR) {
					abandon = true;
					return;
				}
			}
		}
	}
	@Override
	public void accept(Ship ship) {
		//TODO: Assign to ship from team #3 or #4 (using a map)
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		Vector closestPosition = null;
		double closestDistanceSquared = Double.MAX_VALUE;
		for(Ship s: GameMap.INSTANCE.getShips()) {
			if(s.getOwner()==GameMap.INSTANCE.getMyPlayerId()) {
				continue;
			}
			if(s.getDockingStatus()!=DockingStatus.UNDOCKED) {
				continue;
			}
			double distanceSquared = ship.getPosition().getDistanceSquared(s.getPosition());
			if(distanceSquared<closestDistanceSquared) {
				closestDistanceSquared = distanceSquared;
				closestPosition = s.getPosition();
			}
		}
		double direction;
		if(closestPosition==null) {
			direction = ship.getPosition().getDirectionTowards(GameMap.INSTANCE.getCenterPosition());
		}else {
			direction = closestPosition.getDirectionTowards(ship.getPosition());
		}
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
		return abandon?Double.MAX_VALUE:-Double.MAX_VALUE;
	}
}
