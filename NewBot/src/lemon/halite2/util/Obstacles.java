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
	private static Set<Circle> uncertainRegions;
	public static void init() {
		staticObstacles = new HashSet<Circle>();
		dynamicObstacles = new HashMap<Circle, ThrustPlan>();
		uncertainRegions = new HashSet<Circle>();
	}
	public static void clear() {
		staticObstacles.clear();
		dynamicObstacles.clear();
		uncertainRegions.clear();
	}
	public static void addStaticObstacle(Circle circle) {
		staticObstacles.add(circle);
	}
	public static void addDynamicObstacle(Circle circle, ThrustPlan plan) {
		dynamicObstacles.put(circle, plan);
	}
	public static void addUncertainRegion(Circle circle) {
		uncertainRegions.add(circle);
	}
	public static void removeUncertainRegion(Circle circle) {
		uncertainRegions.remove(circle);
	}
	public static Set<Circle> getStaticObstacles(){
		return Collections.unmodifiableSet(staticObstacles);
	}
	public static Map<Circle, ThrustPlan> getDynamicObstacles(){
		return Collections.unmodifiableMap(dynamicObstacles);
	}
	public static Set<Circle> getUncertainRegions(){
		return Collections.unmodifiableSet(uncertainRegions);
	}
}
