package hlt;

public class ThrustMove extends Move {
	private double angle;
	private int thrust;
	public ThrustMove(Ship ship, double angle, int thrust) {
		super(MoveType.Thrust, ship);
		this.thrust = thrust;
		this.angle = angle;
	}
	public double getAngle() {
		return angle;
	}
	public int getThrust() {
		return thrust;
	}
}
