package hlt;

public class ThrustMove extends Move {
	private int thrust;
	private double angle;
	public ThrustMove(Ship ship, int thrust, double angle) {
		super(MoveType.Thrust, ship);
		this.thrust = thrust;
		this.angle = angle;
	}
	public double getAngle() {
		return angle;
	}
	public double getRoundedAngle() {
		return Math.toRadians(Math.round(Math.toDegrees(angle)));
	}
	public int getThrust() {
		return thrust;
	}
}
