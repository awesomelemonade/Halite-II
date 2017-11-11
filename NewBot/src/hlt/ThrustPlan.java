package hlt;

public class ThrustPlan {
	private int thrust;
	private double angleRadians;
	private int angleDegrees;
	public ThrustPlan(int thrust, double angle, RoundPolicy roundPolicy) {
		this.thrust = thrust;
		this.angleRadians = roundPolicy.applyRadians(angle);
		this.angleDegrees = roundPolicy.applyDegrees(angle);
	}
	public int getThrust() {
		return thrust;
	}
	public double getAngleRadians() {
		return angleRadians;
	}
	public int getAngleDegrees() {
		return angleDegrees;
	}
}
