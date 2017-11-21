package lemon.halite2.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hlt.ThrustPlan;

public class Obstacles {
	private List<Obstacle> obstacles;
	private Map<Integer, Obstacle> uncertainObstacles;
	public Obstacles() {
		obstacles = new ArrayList<Obstacle>();
	}
	public void clear() {
		obstacles.clear();
	}
	public void addStaticObstacle(Circle circle) {
		obstacles.add(new Obstacle(circle));
	}
	public void addDynamicObstacle(Circle circle, ThrustPlan plan) {
		obstacles.add(new Obstacle(circle, plan));
	}
	public void addUncertainObstacle(Circle circle, int id) {
		uncertainObstacles.put(id, new Obstacle(circle, id));
	}
	public void removeUncertainObstacle(int id) {
		uncertainObstacles.remove(id);
	}
	public List<Obstacle> getObstacles(){
		return obstacles;
	}
	public Map<Integer, Obstacle> getUncertainObstacles(){
		return uncertainObstacles;
	}
}
