package lemon.halite2.task.experimental;

import hlt.Move;
import hlt.Ship;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.task.BlameMap;
import lemon.halite2.task.Task;
import lemon.halite2.util.BiMap;

public class AbandonTask implements Task {
	public AbandonTask(){
		//Check if we should abandon getting #1
		//Find targets of #3 and #4
	}
	@Override
	public void accept(Ship ship) {
		//Assign to ship from team #3 or #4 (using a map)
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
