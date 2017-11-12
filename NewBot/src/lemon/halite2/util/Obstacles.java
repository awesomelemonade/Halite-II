package lemon.halite2.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import hlt.ThrustPlan;

public class Obstacles {
	private static Set<Circle> staticObstacles;
	private static Map<Circle, ThrustPlan> dynamicObstacles;
	private static Map<Integer, Circle> uncertainObstacles;
	public static void init() {
		staticObstacles = new HashSet<Circle>();
		dynamicObstacles = new HashMap<Circle, ThrustPlan>();
		uncertainObstacles = new HashMap<Integer, Circle>();
	}
	public static void clear() {
		staticObstacles.clear();
		dynamicObstacles.clear();
		uncertainObstacles.clear();
	}
	public static void addStaticObstacle(Circle circle) {
		staticObstacles.add(circle);
	}
	public static void addDynamicObstacle(Circle circle, ThrustPlan plan) {
		dynamicObstacles.put(circle, plan);
	}
	public static void addUncertainObstacle(Circle circle, int id) {
		uncertainObstacles.put(id, circle);
	}
	public static void removeUncertainObstacle(int id) {
		uncertainObstacles.remove(id);
	}
	public static Set<Circle> getStaticObstacles(){
		return Collections.unmodifiableSet(staticObstacles);
	}
	public static Map<Circle, ThrustPlan> getDynamicObstacles(){
		return Collections.unmodifiableMap(dynamicObstacles);
	}
	public static Map<Integer, Circle> getUncertainObstacles(){
		return Collections.unmodifiableMap(uncertainObstacles);
	}
}
