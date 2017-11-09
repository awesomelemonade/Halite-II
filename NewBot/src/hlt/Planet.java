package hlt;

import java.util.Collections;
import java.util.List;

public class Planet extends Entity {
	private int remainingProduction;
	private int currentProduction;
	private int dockingSpots;
	private List<Integer> dockedShips;
	
	public Planet(int owner, int id, Position position, int health, double radius, int dockingSpots,
			int currentProduction, int remainingProduction, List<Integer> dockedShips) {
		super(owner, id, position, health, radius);
		this.dockingSpots = dockingSpots;
		this.currentProduction = currentProduction;
		this.remainingProduction = remainingProduction;
		this.dockedShips = Collections.unmodifiableList(dockedShips);
	}
	public int getRemainingProduction() {
		return remainingProduction;
	}
	public int getCurrentProduction() {
		return currentProduction;
	}
	public int getDockingSpots() {
		return dockingSpots;
	}
	public List<Integer> getDockedShips() {
		return dockedShips;
	}
	public boolean isFull() {
		return dockedShips.size() == dockingSpots;
	}
	public boolean isOwned() {
		return getOwner() != -1;
	}
	@Override
	public String toString() {
		return "Planet[" + super.toString() + ", remainingProduction=" + remainingProduction + ", currentProduction="
				+ currentProduction + ", dockingSpots=" + dockingSpots + ", dockedShips=" + dockedShips + "]";
	}
}
