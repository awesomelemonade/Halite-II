package lemon.halite2.task;

import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Obstacles;
import lemon.halite2.pathfinding.Pathfinder;

public class DockTask implements Task {
	private Pathfinder pathfinder;
	private Obstacles<ObstacleType> obstacles;
	public DockTask(Pathfinder pathfinder, Obstacles<ObstacleType> obstacles){
		this.pathfinder = pathfinder;
		this.obstacles = obstacles;
	}
	@Override
	public void accept(int shipId) {
		
	}
	@Override
	public double getScore(int shipId) {
		return 0;
	}
}
