package hlt;

import lemon.halite2.util.MathUtil;

public enum RoundPolicy {
	CLOCKWISE, COUNTER_CLOCKWISE, ROUND;
	public static int applyDegrees(double radians, RoundPolicy policy) {
		double degrees = MathUtil.normalizeDegrees(Math.toDegrees(radians));
		switch(policy) {
		case CLOCKWISE:
			degrees = Math.floor(degrees);
			break;
		case COUNTER_CLOCKWISE:
			degrees = Math.ceil(degrees);
			break;
		case ROUND:
			degrees = Math.round(degrees);
			break;
		default:
			throw new NullPointerException(String.format("RoundPolicy is null: %f", radians));
		}
		return MathUtil.normalizeDegrees((int)Math.round(degrees));
	}
	public static double applyRadians(double radians, RoundPolicy policy) {
		return MathUtil.normalizeRadians(Math.toRadians(applyDegrees(radians, policy)));
	}
	public double applyRadians(double radians){
		return RoundPolicy.applyRadians(radians, this);
	}
	public int applyDegrees(double radians) {
		return RoundPolicy.applyDegrees(radians, this);
	}
}
