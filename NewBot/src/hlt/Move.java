package hlt;

public class Move {
	public enum MoveType {
		NOOP, THRUST, DOCK, UNDOCK;
	}
	private MoveType type;
	private Ship ship;
	public Move(MoveType type, Ship ship) {
		this.type = type;
		this.ship = ship;
	}
	public MoveType getType() {
		return type;
	}
	public Ship getShip() {
		return ship;
	}
}
