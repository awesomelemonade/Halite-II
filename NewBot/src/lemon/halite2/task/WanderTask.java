package lemon.halite2.task;

import hlt.Move;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustPlan;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;

public class WanderTask implements Task {
	@Override
	public void accept(Ship ship) {
		
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		int randomMagnitude = (int)(7*Math.random());
		int randomDirection = (int)(360*Math.random());
		if(pathfinder.getCandidate(randomMagnitude, randomDirection, ObstacleType.PERMANENT, ObstacleType.UNCERTAIN)==null) {
			return new ThrustMove(ship.getId(), new ThrustPlan(0, 0));
		}
		return new ThrustMove(ship.getId(), new ThrustPlan(0, 0));
	}
	@Override
	public double getScore(Ship ship) {
		return -Double.MAX_VALUE+Double.MIN_VALUE;
	}
}
