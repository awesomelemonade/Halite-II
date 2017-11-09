package hlt;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class Player {
	private Map<Integer, Ship> ships;
	private int id;
	public Player(int id, Map<Integer, Ship> ships) {
		this.id = id;
		this.ships = Collections.unmodifiableMap(ships);
	}
	public Collection<Ship> getShips() {
		return ships.values();
	}
	public Ship getShip(int entityId) {
		return ships.get(entityId);
	}
	public int getId() {
		return id;
	}
}
