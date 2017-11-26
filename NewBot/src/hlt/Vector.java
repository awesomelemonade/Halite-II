package hlt;

public class Vector {
	public static final Vector ZERO = new Vector(0, 0);
	private double x;
	private double y;
	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public double getX() {
		return x;
	}
	public double getY() {
		return y;
	}
	public double getDistanceTo(Vector target) {
		return Math.sqrt(this.getDistanceSquared(target));
	}
	public double getDistanceSquared(Vector target) {
		double dx = target.getX() - x;
		double dy = target.getY() - y;
		return dx*dx+dy*dy;
	}
	public double getDirectionTowards(Vector target) {
		return Math.atan2(target.getY()-y, target.getX()-x);
	}
	public Vector add(Vector vector) {
		return add(vector.getX(), vector.getY());
	}
	public Vector add(double x, double y) {
		return new Vector(this.x+x, this.y+y);
	}
	public Vector addPolar(double magnitude, double direction) {
		return new Vector(x+Math.cos(direction)*magnitude, y+Math.sin(direction)*magnitude);
	}
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Vector vector = (Vector) o;
		return (Double.compare(vector.getX(), x) == 0) && (Double.compare(vector.getY(), y) == 0);
	}
	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	@Override
	public String toString() {
		return "Vector(" + x + ", " + y + ")";
	}
}
