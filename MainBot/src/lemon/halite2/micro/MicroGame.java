package lemon.halite2.micro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import hlt.GameMap;
import hlt.Position;
import hlt.ThrustMove.RoundPolicy;
import lemon.halite2.util.Geometry;
import lemon.halite2.util.MathUtil;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.PathfindPlan;
import lemon.halite2.util.Pathfinder;

public class MicroGame {
	private static final int[] MAGNITUDES = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
	private GameMap gameMap;
	private Set<Group> groups;
	public MicroGame(GameMap gameMap) {
		this.gameMap = gameMap;
		groups = new HashSet<Group>();
	}
	public Group combineGroups(Group a, Group b, Position target, MoveQueue moveQueue) {
		//check if close enough
		//if so, return new Group()
		double targetDistance = a.getCircle().getRadius()+b.getCircle().getRadius()+0.5;
		if(a.getCircle().getPosition().getDistanceSquared(b.getCircle().getPosition())<targetDistance*targetDistance) {
			//Create new group
			Map<Integer, Position> ships = new HashMap<Integer, Position>();
			for(Entry<Integer, Position> entry: a.getShips().entrySet()) {
				ships.put(entry.getKey(), entry.getValue());
			}
			for(Entry<Integer, Position> entry: b.getShips().entrySet()) {
				ships.put(entry.getKey(), entry.getValue());
			}
			Group group = new Group(ships);
			groups.remove(a);
			groups.remove(b);
			groups.add(group);
			return group;
		}else {
			//Get them closer together
			double averageX = (a.getCircle().getPosition().getX()+b.getCircle().getPosition().getX())/2;
			double averageY = (a.getCircle().getPosition().getY()+b.getCircle().getPosition().getY())/2;
			Position midpoint = new Position(averageX, averageY);
			Map<Position, PathfindPlan> possibilitiesA = 
					bruteforce(a.getCircle().getPosition(), a.getCircle().getRadius(), midpoint, target, MAGNITUDES);
			Map<Position, PathfindPlan> possibilitiesB = 
					bruteforce(b.getCircle().getPosition(), b.getCircle().getRadius(), midpoint, target, MAGNITUDES);
			PathfindPlan bestPathfindPlanA = null;
			PathfindPlan bestPathfindPlanB = null;
			double bestDistanceSquared = Double.MAX_VALUE;
			for(Position positionA: possibilitiesA.keySet()) {
				for(Position positionB: possibilitiesB.keySet()) {
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
	private Map<Position, PathfindPlan> bruteforce(Position position, double buffer, Position start, Position end, int[] magnitudes){
		Map<Position, PathfindPlan> possibilities = new HashMap<Position, PathfindPlan>();
		Position projection = Geometry.projectPointToLine(start, end, position);
		double projectionDistance = projection.getDistanceTo(position)-buffer;
		double projectionDirection = position.getDirectionTowards(projection);
		double targetDirection = position.getDirectionTowards(end);
		RoundPolicy roundPolicy;
		int sign = 0;
		if(MathUtil.angleBetween(RoundPolicy.FLOOR.apply(projectionDirection), targetDirection)<
				MathUtil.angleBetween(RoundPolicy.CEIL.apply(projectionDirection), targetDirection)) {
			sign = -1;
			roundPolicy = RoundPolicy.FLOOR;
		}else {
			sign = 1;
			roundPolicy = RoundPolicy.CEIL;
		}
		for(int magnitude: magnitudes) {
			if(magnitude<projectionDistance) {
				double direction = roundPolicy.apply(projectionDirection);
				possibilities.put(position.addPolar(magnitude, direction), new PathfindPlan(magnitude, direction, RoundPolicy.NONE));
			}else {
				double theta = Math.acos(projectionDistance/magnitude);
				double direction = roundPolicy.apply(projectionDirection+theta*sign);
				possibilities.put(position.addPolar(magnitude, direction), new PathfindPlan(magnitude, direction, RoundPolicy.NONE));
			}
		}
		return possibilities;
	}
	public void moveGroup(Group group, Position target, double targetBuffer, MoveQueue moveQueue) {
		/*
		PathfindPlan plan = Pathfinder.pathfind(ship, start, target, buffer, targetBuffer);
		for(int shipId: groups.get(group)) {
			Ship ship = gameMap.getMyPlayer().getShip(shipId);
			moveQueue.addMove(new ThrustMove(ship, plan));
		}
		*/
	}
	public void update() {
		for(Group group: groups) {
			group.update(gameMap);
		}
		//do stuff
		
	}
}
