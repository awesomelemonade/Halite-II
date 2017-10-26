package hlt;

public class ThrustMove extends Move {
	private int thrust;
	private double angle;
	private RoundPolicy roundPolicy;
	public ThrustMove(Ship ship, int thrust, double angle, RoundPolicy roundPolicy) {
		super(MoveType.THRUST, ship);
		this.thrust = thrust;
		this.angle = angle;
		this.roundPolicy = roundPolicy;
	}
	public double getRoundedAngle() {
		return Math.toRadians(RoundPolicy.apply(Math.toDegrees(angle), roundPolicy));
	}
	public int getThrust() {
		return thrust;
	}
	public enum RoundPolicy {
		FLOOR, CEIL, ROUND;
		public static double apply(double num, RoundPolicy policy) {
			switch(policy) {
			case FLOOR:
				return Math.floor(num);
			case CEIL:
				return Math.ceil(num);
			case ROUND:
				return Math.round(num);
			default:
				throw new NullPointerException(String.format("RoundPolicy is null: %f", num));
			}
		}
	}
}
