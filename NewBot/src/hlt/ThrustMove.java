package hlt;

public class ThrustMove extends Move {
	private ThrustPlan thrustPlan;
	public ThrustMove(int shipId, ThrustPlan thrustPlan) {
		super(MoveType.THRUST, shipId);
		this.thrustPlan = thrustPlan;
	}
	public ThrustPlan getThrustPlan() {
		return thrustPlan;
	}
}
