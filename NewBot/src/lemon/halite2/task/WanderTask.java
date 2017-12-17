package lemon.halite2.task;

import java.util.Random;

import hlt.GameMap;
import hlt.Move;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustPlan;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;

public class WanderTask implements Task {
	private Random r;
	public WanderTask() {
		long hash = GameMap.INSTANCE.getWidth();
		hash = hash*500+GameMap.INSTANCE.getHeight();
		hash = hash*50+GameMap.INSTANCE.getPlanets().size();
		hash = hash*4+GameMap.INSTANCE.getMyPlayerId();
		hash = hash*4+GameMap.INSTANCE.getPlayers().size();
		hash = hash*300+GameMap.INSTANCE.getTurnNumber();
		r = new Random(hash);
	}
	@Override
	public void accept(Ship ship) {
		
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		int randomMagnitude = r.nextInt(8);
		int randomDirection = r.nextInt(360);
		if(pathfinder.getCandidate(randomMagnitude, randomDirection, ObstacleType.PERMANENT, ObstacleType.UNCERTAIN)==null) {
			return new ThrustMove(ship.getId(), new ThrustPlan(randomMagnitude, randomDirection));
		}
		return new ThrustMove(ship.getId(), new ThrustPlan(0, 0));
	}
	@Override
	public double getScore(Ship ship) {
		return -Double.MAX_VALUE+Math.ulp(Double.MAX_VALUE);
	}
}
