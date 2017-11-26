package lemon.halite2.micro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import hlt.GameMap;
import hlt.Vector;
import hlt.RoundPolicy;
import hlt.Ship;
import hlt.Ship.DockingStatus;
import hlt.ThrustPlan;
import lemon.halite2.util.Geometry;
import lemon.halite2.util.MathUtil;
import lemon.halite2.util.MoveQueue;

public class MicroGame {
	private static final int[] MAGNITUDES = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
	private GameMap gameMap;
	private Set<Group> groups;
	public MicroGame(GameMap gameMap) {
		this.gameMap = gameMap;
		groups = new HashSet<Group>();
	}
	public Group getGroup(Ship ship) {
		for(Group group: groups) {
			if(group.getShips().containsKey(ship.getId())) {
				return group;
			}
		}
		return null;
	}
	public Group combineGroups(Group a, Group b, Vector target, MoveQueue moveQueue) {
		//check if close enough
		//if so, return new Group()
		double targetDistance = a.getCircle().getRadius()+b.getCircle().getRadius()+0.5;
		if(a.getCircle().getPosition().getDistanceSquared(b.getCircle().getPosition())<targetDistance*targetDistance) {
			//Create new group
			Map<Integer, Vector> ships = new HashMap<Integer, Vector>();
			for(Entry<Integer, Vector> entry: a.getShips().entrySet()) {
				ships.put(entry.getKey(), entry.getValue());
			}
			for(Entry<Integer, Vector> entry: b.getShips().entrySet()) {
				ships.put(entry.getKey(), entry.getValue());
			}
			return new Group(ships);
		}else {
			//Get them closer together
			double averageX = (a.getCircle().getPosition().getX()+b.getCircle().getPosition().getX())/2;
			double averageY = (a.getCircle().getPosition().getY()+b.getCircle().getPosition().getY())/2;
			Vector midpoint = new Vector(averageX, averageY);
			Map<Vector, ThrustPlan> possibilitiesA = 
					bruteforce(a.getCircle().getPosition(), a.getCircle().getRadius(), midpoint, target, MAGNITUDES);
			Map<Vector, ThrustPlan> possibilitiesB = 
					bruteforce(b.getCircle().getPosition(), b.getCircle().getRadius(), midpoint, target, MAGNITUDES);
			ThrustPlan bestPathfindPlanA = null;
			ThrustPlan bestPathfindPlanB = null;
			double bestDistanceSquared = Double.MAX_VALUE;
			for(Vector positionA: possibilitiesA.keySet()) {
				for(Vector positionB: possibilitiesB.keySet()) {
					double distanceSquared = positionA.getDistanceSquared(positionB);
					if(bestDistanceSquared>distanceSquared) {
						bestPathfindPlanA = possibilitiesA.get(positionA);
						bestPathfindPlanB = possibilitiesB.get(positionB);
						bestDistanceSquared = distanceSquared;
					}
				}
			}
			a.move(gameMap, moveQueue, bestPathfindPlanA);
			b.move(gameMap, moveQueue, bestPathfindPlanB);
			return null;
		}
	}
	private Map<Vector, ThrustPlan> bruteforce(Vector position, double buffer, Vector start, Vector end, int[] magnitudes){
		Map<Vector, ThrustPlan> possibilities = new HashMap<Vector, ThrustPlan>();
		Vector projection = Geometry.projectPointToLine(start, end, position);
		double projectionDistance = projection.getDistanceTo(position)-buffer;
		double projectionDirection = position.getDirectionTowards(projection);
		double targetDirection = position.getDirectionTowards(end);
		RoundPolicy roundPolicy;
		int sign = 0;
		if(MathUtil.angleBetweenRadians(RoundPolicy.FLOOR.applyRadians(projectionDirection), targetDirection)<
				MathUtil.angleBetweenRadians(RoundPolicy.CEIL.applyRadians(projectionDirection), targetDirection)) {
			sign = -1;
			roundPolicy = RoundPolicy.FLOOR;
		}else {
			sign = 1;
			roundPolicy = RoundPolicy.CEIL;
		}
		for(int magnitude: magnitudes) {
			if(magnitude<projectionDistance) {
				double direction = roundPolicy.applyRadians(projectionDirection);
				possibilities.put(position.addPolar(magnitude, direction), new ThrustPlan(magnitude, direction, RoundPolicy.ROUND));
			}else {
				double theta = Math.acos(projectionDistance/magnitude);
				double direction = roundPolicy.applyRadians(projectionDirection+theta*sign);
				possibilities.put(position.addPolar(magnitude, direction), new ThrustPlan(magnitude, direction, RoundPolicy.ROUND));
			}
		}
		return possibilities;
	}
	public void update() {
		for(Ship ship: gameMap.getMyPlayer().getShips()){
			Group group = getGroup(ship);
			if(ship.getDockingStatus()!=DockingStatus.UNDOCKED&&group!=null){
				groups.remove(group);
			}
		}
		for(Ship ship: gameMap.getMyPlayer().getShips()){
			if(ship.getDockingStatus()==DockingStatus.UNDOCKED&&getGroup(ship)==null){
				groups.add(new Group(ship));
			}
		}
		List<Group> deadGroups = new ArrayList<Group>();
		for(Group group: groups) {
			if(!group.update(gameMap)) {
				deadGroups.add(group);
			}
		}
		for(Group dead: deadGroups) {
			groups.remove(dead);
		}
	}
	public Set<Group> getGroups(){
		return groups;
	}
}
