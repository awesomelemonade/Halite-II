package lemon.halite2.util;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import hlt.GameMap;
import hlt.Ship;

public class ShipPriorities {
	private GameMap gameMap;
	private Map<Priority, Set<Integer>> prioritiesMap; // Maps Priority to List of ID of Ships
	private Map<Integer, Priority> shipMap; // Maps ID of Ship to Priority
	private Queue<Integer> shipIdQueue; // Queue for promoting or demoting ships
	private Queue<Priority> shipPriorityQueue; // Queue for promoting or demoting ships
	public ShipPriorities(GameMap gameMap) {
		this.gameMap = gameMap;
		prioritiesMap = new TreeMap<Priority, Set<Integer>>();
		for(Priority priority: Priority.values()) {
			prioritiesMap.put(priority, new HashSet<Integer>());
		}
		shipMap = new HashMap<Integer, Priority>();
		shipIdQueue = new ArrayDeque<Integer>();
		shipPriorityQueue = new ArrayDeque<Priority>();
	}
	public void update() {
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
	public Map<Priority, Set<Integer>> getPrioritiesMap(){
		return prioritiesMap;
	}
	public enum Priority implements Comparable<Priority> {
		VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW;
	}
}
