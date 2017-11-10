package lemon.halite2.micro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Position;
import hlt.Ship;
import lemon.halite2.util.Circle;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.PathfindPlan;

public class Group implements Comparable<Group> {
	private Map<Integer, Position> ships; //Maps shipId to Position
	private Circle circle;
	public Group(Ship ship) {
		ships = new HashMap<Integer, Position>();
		ships.put(ship.getId(), ship.getPosition());
		Circle circle = EncloseCircle.create(ships.values());
		circle.setRadius(circle.getRadius()+GameConstants.SHIP_RADIUS);
		this.circle = circle;
	}
	public Group(Map<Integer, Position> ships) {
		this.ships = ships;
		Circle circle = EncloseCircle.create(ships.values());
		circle.setRadius(circle.getRadius()+GameConstants.SHIP_RADIUS);
		this.circle = circle;
	}
	public boolean update(GameMap gameMap) {
		List<Integer> deadShips = new ArrayList<Integer>();
		//Remove all dead ships
		for(int shipId: ships.keySet()) {
			if(gameMap.getMyPlayer().getShip(shipId)==null) {
				deadShips.add(shipId);
			}
		}
		for(int shipId: deadShips) {
			ships.remove(shipId);
		}
		if(ships.isEmpty()) {
			return false;
		}
		Circle circle = EncloseCircle.create(ships.values());
		circle.setRadius(circle.getRadius()+GameConstants.SHIP_RADIUS);
		this.circle = circle;
		return true;
	}
	public void move(GameMap gameMap, MoveQueue moveQueue, PathfindPlan plan) {
		//pathfind using circle
		for(int shipId: ships.keySet()) {
			moveQueue.forceMove(plan.apply(gameMap.getMyPlayer().getShip(shipId)));
		}
	}
	public Circle getCircle() {
		return circle;
	}
	public Map<Integer, Position> getShips(){
		return ships;
	}
	public int getSize(){
		return ships.size();
	}
	@Override
	public int compareTo(Group group) {
		return group.getSize()-this.getSize(); //Sort from biggest to smallest
	}
}
