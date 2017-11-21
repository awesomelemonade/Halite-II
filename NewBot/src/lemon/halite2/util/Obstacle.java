package lemon.halite2.util;

import hlt.ThrustPlan;

public class Obstacle {
	private Circle circle;
	private ThrustPlan thrustPlan; // null = static obstacle
	private int id; // responsible id for "uncertain" obstacles; -1 = not uncertain
	public Obstacle(Circle circle){
		this(circle, null, -1);
	}
	public Obstacle(Circle circle, ThrustPlan thrustPlan){
		this(circle, thrustPlan, -1);
	}
	public Obstacle(Circle circle, int id){
		this(circle, null, id);
	}
	public Obstacle(Circle circle, ThrustPlan thrustPlan, int id){
		this.circle = circle;
		this.thrustPlan = thrustPlan;
		this.id = id;
	}
	public Circle getCircle(){
		return circle;
	}
	public ThrustPlan getThrustPlan(){
		return thrustPlan;
	}
	public int getId(){
		return id;
	}
}
