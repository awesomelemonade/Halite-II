package lemon.halite2.task;

import java.util.Map;
import java.util.Set;

import hlt.Move;
import hlt.Ship;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;

public interface Task {
	public void accept(Ship ship);
	public Move execute(Ship ship, Pathfinder pathfinder, Map<Integer, Set<Integer>> blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles);
	public double getScore(Ship ship);
}
