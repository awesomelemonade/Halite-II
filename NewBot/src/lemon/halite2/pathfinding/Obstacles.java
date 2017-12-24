package lemon.halite2.pathfinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Obstacles<T> {
	private Map<T, List<Obstacle>> obstacles;
	public Obstacles() {
		obstacles = new HashMap<T, List<Obstacle>>();
	}
	public boolean addObstacle(T type, Obstacle obstacle) {
		if(!obstacles.containsKey(type)) {
			obstacles.put(type, new ArrayList<Obstacle>());
		}
		return obstacles.get(type).add(obstacle);
	}
	public boolean removeObstacle(T type, Obstacle obstacle) {
		if(obstacles.containsKey(type)) {
			return obstacles.get(type).remove(obstacle);
		}
		return false;
	}
	public List<Obstacle> getObstacles(T type){
		if(!obstacles.containsKey(type)) {
			obstacles.put(type, new ArrayList<Obstacle>());
		}
		return obstacles.get(type);
	}
}
