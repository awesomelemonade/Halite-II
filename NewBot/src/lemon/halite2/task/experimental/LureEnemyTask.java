package lemon.halite2.task.experimental;

import hlt.Move;
import hlt.Ship;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.task.BlameMap;
import lemon.halite2.task.Task;
import lemon.halite2.util.BiMap;

public class LureEnemyTask implements Task {
	public LureEnemyTask(Ship ship){
		//Detect if the ships around you have an extreme density of enemies
		//Figure out a common enemy if it is a 4-player game
		//Fall back on opposite side of map
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
	public double getScore(Ship ship, double minScore) {
		return -Double.MAX_VALUE;
	}
}
