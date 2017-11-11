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
	@Override
	public boolean equals(Object o) {
		if(this==o) {
			return true;
		}
		if(o==null||this.getClass()!=o.getClass()) {
			return false;
		}
		Circle circle = (Circle)o;
		return circle.getPosition().equals(position)&&Double.compare(circle.getRadius(), radius)==0;
	}
	@Override
	public int hashCode() {
		long temp = Double.doubleToLongBits(radius);
		return 31 * position.hashCode() + (int)(temp^(temp>>>32));
	}
	@Override
	public String toString() {
		return String.format("Circle[position=%s, radius=%f]", position.toString(), radius);
	}
}