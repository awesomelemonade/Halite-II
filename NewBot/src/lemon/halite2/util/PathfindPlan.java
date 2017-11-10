package lemon.halite2.util;

import hlt.Position;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustMove.RoundPolicy;

public class PathfindPlan {
	private int thrust;
	private double angle;
	private RoundPolicy roundPolicy;
	public PathfindPlan(int thrust, double angle, RoundPolicy roundPolicy) {
		this.thrust = thrust;
		this.angle = angle;
		this.roundPolicy = roundPolicy;
	}
	public int getThrust() {
		return thrust;
	}
	public double getRoundedAngle() {
		return roundPolicy.apply(angle);
	}
	public Position toVelocity() {
		return new Position(thrust*Math.cos(getRoundedAngle()), thrust*Math.sin(getRoundedAngle()));
	}
	public ThrustMove apply(Ship ship) {
		return new ThrustMove(ship, thrust, angle, roundPolicy);
	}
}
