package lemon.halite2.micro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Position;
import hlt.Ship;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.PathfindPlan;

public class Group {
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
	public void update(GameMap gameMap) {
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
		Circle circle = EncloseCircle.create(ships.values());
		circle.setRadius(circle.getRadius()+GameConstants.SHIP_RADIUS);
		this.circle = circle;
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
}
