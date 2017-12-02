package lemon.halite2.task;

import hlt.Move;
import hlt.Ship;
import lemon.halite2.pathfinding.Pathfinder;

public interface Task {
	public void accept(Ship ship);
	public Move execute(Ship ship, Pathfinder pathfinder);
	public double getScore(Ship ship);
}
