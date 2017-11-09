package hlt;

public class DockMove extends Move {
	private long destinationId;
	public DockMove(Ship ship, Planet planet) {
		super(MoveType.DOCK, ship);
		destinationId = planet.getId();
	}
	public long getDestinationId() {
		return destinationId;
	}
}
