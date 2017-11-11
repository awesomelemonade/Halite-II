package hlt;

public class DockMove extends Move {
	private int planetId;
	public DockMove(int shipId, int planetId) {
		super(MoveType.DOCK, shipId);
		this.planetId = planetId;
	}
	public int getPlanetId() {
		return planetId;
	}
}
