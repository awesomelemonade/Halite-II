package hlt;

public class Move {
	public enum MoveType {
		THRUST, DOCK, UNDOCK;
	}
	private MoveType type;
	private int shipId;
	public Move(MoveType type, int shipId) {
		this.type = type;
		this.shipId = shipId;
	}
	public MoveType getType() {
		return type;
	}
	public int getShipId() {
		return shipId;
	}
}
