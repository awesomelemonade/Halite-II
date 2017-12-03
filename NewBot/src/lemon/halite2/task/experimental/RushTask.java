package lemon.halite2.task.experimental;

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

public class RushTask implements Task {
	private static final double buffer = GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS+10*GameConstants.MAX_SPEED;
	private boolean activate;
	private Planet targetPlanet;
	public RushTask(int teamId) {
		activate = GameMap.INSTANCE.getTurnNumber()<40;
		if(!activate) {
			return;
		}
		double sumX = 0;
		double sumY = 0;
		double count = 0;
		for(Ship ship: GameMap.INSTANCE.getShips()) {
			if(ship.getOwner()==teamId) {
				sumX+=ship.getPosition().getX();
				sumY+=ship.getPosition().getY();
				count++;
			}
		}
		Vector average = new Vector(sumX/count, sumY/count);
		double bestDistance = Double.MAX_VALUE;
		Planet bestPlanet = null;
		for(Planet planet: GameMap.INSTANCE.getPlanets()) {
			double distance = average.getDistanceTo(planet.getPosition())-planet.getRadius();
			if(distance<bestDistance) {
				bestDistance = distance;
				bestPlanet = planet;
			}
		}
		this.targetPlanet = bestPlanet;
	}
	@Override
	public void accept(Ship ship) {
		
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public double getScore(Ship ship) {
		if(!activate) {
			return -Double.MAX_VALUE;
		}
		if(ship.getPosition().getDistanceSquared(targetPlanet.getPosition())<buffer*buffer) {
			return -Double.MAX_VALUE;
		}
		return -Double.MAX_VALUE;
	}
}
