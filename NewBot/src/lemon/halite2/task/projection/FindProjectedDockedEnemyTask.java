package lemon.halite2.task.projection;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Move;
import hlt.Planet;
import hlt.Ship;
import hlt.Vector;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.task.BlameMap;
import lemon.halite2.task.Task;
import lemon.halite2.util.BiMap;

public class FindProjectedDockedEnemyTask implements Task {
	private Ship enemyShip;
	private Vector projection;
	private double distance;
	public FindProjectedDockedEnemyTask(Ship enemyShip){
		this.enemyShip = enemyShip;
		//Find Projection
		double bestDistance = Double.MAX_VALUE;
		Vector bestProjection = null;
		for(Planet planet: GameMap.INSTANCE.getPlanets()){
			double distance = planet.getPosition().getDistanceTo(enemyShip.getPosition())-planet.getRadius()-GameConstants.SHIP_RADIUS;
			if(distance<bestDistance){
				bestDistance = distance;
				bestProjection = planet.getPosition().addPolar(planet.getRadius()+GameConstants.SHIP_RADIUS, planet.getPosition().getDirectionTowards(enemyShip.getPosition()));
			}
		}
		this.projection = bestProjection;
		this.distance = bestDistance;
	}
	@Override
	public void accept(Ship ship) {
		
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		return null;
	}
	@Override
	public double getScore(Ship ship) {
		return -Double.MAX_VALUE;
	}
}