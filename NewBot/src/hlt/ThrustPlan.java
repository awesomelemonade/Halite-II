package hlt;

import lemon.halite2.util.MathUtil;

public class ThrustPlan {
	private int thrust;
	private int angle;
	public ThrustPlan(int thrust, double angle, RoundPolicy roundPolicy) {
		this.thrust = thrust;
		this.angle = roundPolicy.applyDegrees(angle);
	}
	public ThrustPlan(int thrust, int angle) {
		this.thrust = thrust;
		this.angle = MathUtil.normalizeDegrees(angle);
	}
	public int getThrust() {
		return thrust;
	}
	public int getAngle() {
		return angle;
	}
}
