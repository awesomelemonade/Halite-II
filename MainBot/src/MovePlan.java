import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Position;
import hlt.Ship;
import hlt.ThrustMove;

public class MovePlan {
	private GameMap gameMap;
	private Map<Priority, Set<Integer>> prioritiesMap; // Maps Priority to List of ID of Ships
	private Map<Integer, Priority> shipMap; // Maps ID of Ship to Priority
	private Queue<Integer> shipIdQueue; // Queue for promoting or demoting ships
	private Queue<Priority> shipPriorityQueue; // Queue for promoting or demoting ships
	private List<ThrustMove> moves;
	public MovePlan(GameMap gameMap) {
		this.gameMap = gameMap;
		prioritiesMap = new TreeMap<Priority, Set<Integer>>();
		for(Priority priority: Priority.values()) {
			prioritiesMap.put(priority, new HashSet<Integer>());
		}
		shipMap = new HashMap<Integer, Priority>();
		shipIdQueue = new ArrayDeque<Integer>();
		shipPriorityQueue = new ArrayDeque<Priority>();
		moves = new ArrayList<ThrustMove>();
	}
	public void update() {
		moves.clear();
		//Update & Empty Queues
		while(!shipIdQueue.isEmpty()) {
			Priority priority = shipPriorityQueue.poll();
			int id = shipIdQueue.poll();
			prioritiesMap.get(shipMap.get(id)).remove(id);
			shipMap.put(id, priority);
			prioritiesMap.get(priority).add(id);
		}
		//Remove Old (Dead) Ships
		for(Priority priority: prioritiesMap.keySet()) {
			for(int id: prioritiesMap.get(priority)) {
				if(gameMap.getMyPlayer().getShip(id)==null) {
					prioritiesMap.get(priority).remove(id);
					shipMap.remove(id);
				}
			}
		}
		//Add New Ships to least priority
		for(Ship ship: gameMap.getMyPlayer().getShips()) {
			if(!shipMap.containsKey(ship.getId())) {
				shipMap.put(ship.getId(), Priority.VERY_LOW);
				prioritiesMap.get(Priority.VERY_LOW).add(ship.getId());
			}
		}
	}
	public void setPriority(int shipId, Priority priority) {
		shipIdQueue.add(shipId);
		shipPriorityQueue.add(priority);
	}
	public int tryMove(ThrustMove move) {
		return tryMove(move, true);
	}
	public int tryMove(ThrustMove move, boolean checkPlanets) {
		if(move.getThrust()==0) {
			moves.add(move);
			return 0;
		}
		if(checkPlanets) {
			for(Planet planet: gameMap.getPlanets()) {
				if(Pathfinder.segmentPointDistance(move.getShip().getPosition(),
						move.getShip().getPosition().addPolar(move.getThrust(), move.getRoundedAngle()),
						planet.getPosition(), planet.getRadius()+GameConstants.SHIP_RADIUS)) {
					
				}
			}
		}
		for(ThrustMove otherMoves: moves) {
			if(Pathfinder.intersect())
		}
		moves.add(move);
		return 0;
	}
	private boolean intersect(ThrustMove moveA, ThrustMove moveB) {
		Position endA = moveA.getShip().getPosition().addPolar(moveA.getThrust(), moveA.getRoundedAngle());
		Position endB = moveB.getShip().getPosition().addPolar(moveB.getThrust(), moveB.getRoundedAngle());
		double a = Pathfinder.segmentPointDistance(moveA.getShip().getPosition(), endA, moveB.getShip().getPosition());
		double b = Pathfinder.segmentPointDistance(moveA.getShip().getPosition(), endA, endB);
		double c = Pathfinder.segmentPointDistance(moveB.getShip().getPosition(), endB, moveA.getShip().getPosition());
		double d = Pathfinder.segmentPointDistance(moveB.getShip().getPosition(), endB, endA);
		return Math.min(Math.min(a, b), Math.min(c, d))<=2*GameConstants.SHIP_RADIUS;
	}
	public List<ThrustMove> getMoves(){
		return moves;
	}
	public Map<Priority, Set<Integer>> getPrioritiesMap(){
		return prioritiesMap;
	}
	public enum Priority implements Comparable<Priority> {
		VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW;
	}
}
