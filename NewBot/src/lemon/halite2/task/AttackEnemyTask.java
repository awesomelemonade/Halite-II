package lemon.halite2.task;

import java.util.Map;
import java.util.Set;

import hlt.Move;
import hlt.Ship;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;

public class AttackEnemyTask implements Task {
	@Override
	public void accept(Ship ship) {
		// TODO Auto-generated method stub
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, Map<Integer, Set<Integer>> blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public double getScore(Ship ship) {
		// TODO Auto-generated method stub
		return Integer.MIN_VALUE;
	}
}
