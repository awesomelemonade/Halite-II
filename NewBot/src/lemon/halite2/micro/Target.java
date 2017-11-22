package lemon.halite2.micro;

import hlt.GameConstants;
import hlt.Planet;
import hlt.Position;

public class Target {
	private Position position;
	private double innerRadius;
	private double outerRadius;
	public Target(Position position, double innerRadius, double outerRadius){
		this.position = position;
		this.innerRadius = innerRadius;
		this.outerRadius = outerRadius;
	}
	public Position getPosition(){
		return position;
	}
	public double getInnerRadius(){
		return innerRadius;
	}
	public double getOuterRadius(){
		return outerRadius;
	}
	public static Target createDockingTarget(Planet planet){
		return new Target(planet.getPosition(), planet.getRadius(), planet.getRadius()+GameConstants.DOCK_RADIUS);
	}
	public static Target createCrashTarget(Position position){
		return new Target(position, 0, GameConstants.SHIP_RADIUS);
	}
	public static Target createAttackTarget(Position position){
		return new Target(position, GameConstants.SHIP_RADIUS, GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS);
	}
}
