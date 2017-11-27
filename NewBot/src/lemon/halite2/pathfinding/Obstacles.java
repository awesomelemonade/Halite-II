package lemon.halite2.pathfinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Obstacles<T> {
	private Map<T, List<Obstacle>> obstacles;
	public Obstacles() {
		obstacles = new HashMap<T, List<Obstacle>>();
	}
	public void addObstacle(T type, Obstacle obstacle) {
		obstacles.get(type).add(obstacle);
	}
	public void removeObstacle(T type, Obstacle obstacle) {
		obstacles.get(type).remove(obstacle);
	}
	public List<Obstacle> getObstacles(T type){
		return obstacles.get(type);
	}
}
