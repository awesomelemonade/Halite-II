package lemon.halite2.util;

import hlt.Position;

public class Circle {
	private Position position;
	private double radius;
	
	public Circle(Position position, double radius) {
		this.position = position;
		this.radius = radius;
	}
	public boolean contains(Position position) {
		return radius * radius >= position.getDistanceSquared(this.position);
	}
	public void setPosition(Position position) {
		this.position = position;
	}
	public void setRadius(double radius) {
		this.radius = radius;
	}
	public Position getPosition() {
		return position;
	}
	public double getRadius() {
		return radius;
	}
}