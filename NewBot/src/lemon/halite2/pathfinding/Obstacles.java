package lemon.halite2.pathfinding;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class Obstacles {
	private Set<Obstacle> obstacles;
	public Obstacles() {
		obstacles = new TreeSet<Obstacle>(new Comparator<Obstacle>() {
			@Override
			public int compare(Obstacle a, Obstacle b) {
				return a.getPriority()-b.getPriority();
			}
		});
	}
	public void addObstacle(Obstacle obstacle) {
		obstacles.add(obstacle);
	}
	public void removeObstacle(Obstacle obstacle) {
		obstacles.remove(obstacle);
	}
	public Set<Obstacle> getObstacles(){
		return obstacles;
	}
}
