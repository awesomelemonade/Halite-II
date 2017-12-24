package lemon.halite2.task.projection;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Move;
import hlt.Planet;
import hlt.RoundPolicy;
import hlt.Ship;
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

public class FindProjectedDockedEnemyTask implements Task {
	private Vector projection;
	private double distanceSquared;
	private boolean activate;
	public FindProjectedDockedEnemyTask(Ship enemyShip){
		//Find Projection
		Planet bestPlanet = null;
		double bestDistance = Double.MAX_VALUE;
		Vector bestProjection = null;
		for(Planet planet: GameMap.INSTANCE.getPlanets()){
			if(planet.isOwned()) {
				if(planet.getOwner()==enemyShip.getOwner()){
					if(planet.isFull()) {
						continue;
					}
				}
			}
			double distance = planet.getPosition().getDistanceTo(enemyShip.getPosition())-planet.getRadius()-GameConstants.SHIP_RADIUS;
			if(distance<bestDistance){
				bestPlanet = planet;
				bestDistance = distance;
				bestProjection = planet.getPosition().addPolar(planet.getRadius()+GameConstants.SHIP_RADIUS, planet.getPosition().getDirectionTowards(enemyShip.getPosition()));
			}
		}
		if(bestPlanet!=null&&(!bestPlanet.isOwned())){
			Projection projection = ProjectionManager.INSTANCE.calculate(bestProjection, 1, s->false);
			if(projection.getFriendlyProjectionItems().first().getDistanceSquared()>64*GameConstants.MAX_SPEED*GameConstants.MAX_SPEED) {
				this.activate = true;
				this.projection = bestProjection;
				this.distanceSquared = bestDistance*bestDistance;
			}
		}else{
			this.activate = false;
		}
	}
	@Override
	public void accept(Ship ship) {
		this.activate = false;
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		double direction = ship.getPosition().getDirectionTowards(projection);
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
		if(activate){
			double distanceSquared = projection.getDistanceSquared(ship.getPosition());
			if(distanceSquared>this.distanceSquared){
				return -distanceSquared;
			}
		}
		return -Double.MAX_VALUE;
	}
}