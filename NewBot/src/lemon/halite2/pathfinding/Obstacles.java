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
				int temp = b.getPriority()-a.getPriority();
				if(temp==0) {
					temp = a.hashCode()-b.hashCode();
				}
				return temp;
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
