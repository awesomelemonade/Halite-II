package lemon.halite2.pathfinding;

import java.util.ArrayList;
import java.util.List;

public class Obstacles {
	private List<Obstacle> obstacles;
	public Obstacles() {
		obstacles = new ArrayList<Obstacle>();
	}
	public void addObstacle(Obstacle obstacle) {
		obstacles.add(obstacle);
	}
	public void removeObstacle(Obstacle obstacle) {
		obstacles.remove(obstacle);
	}
	public List<Obstacle> getObstacles(){
		return obstacles;
	}
	public List<Obstacle> getObstacles(int index){
		return obstacles.subList(index, obstacles.size());
	}
	public int getSize(){
		return obstacles.size();
	}
}
