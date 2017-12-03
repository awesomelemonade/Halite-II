package lemon.halite2.task;

import hlt.Move;
import hlt.Ship;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;

public class DefendDockedShipTask implements Task {
	@Override
	public void accept(Ship ship) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public double getScore(Ship ship) {
		// TODO Auto-generated method stub
		return -Double.MAX_VALUE;
	}
}
