package hlt;

public class Ship extends Entity {
	public enum DockingStatus {
		Undocked, Docking, Docked, Undocking
	}
	private DockingStatus dockingStatus;
	private int dockedPlanet;
	private int dockingProgress;
	private int weaponCooldown;
	public Ship(int owner, int id, Position position, int health, DockingStatus dockingStatus, int dockedPlanet,
			int dockingProgress, int weaponCooldown) {
		super(owner, id, position, health, GameConstants.SHIP_RADIUS);

		this.dockingStatus = dockingStatus;
		this.dockedPlanet = dockedPlanet;
		this.dockingProgress = dockingProgress;
		this.weaponCooldown = weaponCooldown;
	}
	public int getWeaponCooldown() {
		return weaponCooldown;
	}
	public DockingStatus getDockingStatus() {
		return dockingStatus;
	}
	public int getDockingProgress() {
		return dockingProgress;
	}
	public int getDockedPlanet() {
		return dockedPlanet;
	}
	public boolean canDock(Planet planet) {
		return this.getPosition().getDistanceTo(planet.getPosition()) <= GameConstants.DOCK_RADIUS + planet.getRadius();
	}
	@Override
	public String toString() {
		return "Ship[" + super.toString() + ", dockingStatus=" + dockingStatus + ", dockedPlanet=" + dockedPlanet
				+ ", dockingProgress=" + dockingProgress + ", weaponCooldown=" + weaponCooldown + "]";
	}
}
