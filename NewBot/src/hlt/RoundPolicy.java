package hlt;

public enum RoundPolicy {
	FLOOR, CEIL, ROUND;
	public static int applyDegrees(double radians, RoundPolicy policy) {
		double degrees = Math.toDegrees(radians);
		switch(policy) {
		case FLOOR:
			return (int)Math.floor(degrees);
		case CEIL:
			return (int)Math.ceil(degrees);
		case ROUND:
			return (int)Math.round(degrees);
		default:
			throw new NullPointerException(String.format("RoundPolicy is null: %f", radians));
		}
	}
	public static double applyRadians(double radians, RoundPolicy policy) {
		return Math.toRadians(applyDegrees(radians, policy));
	}
	public double applyRadians(double radians){
		return RoundPolicy.applyRadians(radians, this);
	}
	public int applyDegrees(double radians) {
		return RoundPolicy.applyDegrees(radians, this);
	}
}
