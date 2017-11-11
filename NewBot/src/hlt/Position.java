package hlt;

public class Position {
	private double x;
	private double y;
	public Position(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public double getX() {
		return x;
	}
	public double getY() {
		return y;
	}
	public double getDistanceTo(Position target) {
		return Math.sqrt(this.getDistanceSquared(target));
	}
	public double getDistanceSquared(Position target) {
		double dx = target.getX() - x;
		double dy = target.getY() - y;
		return dx*dx+dy*dy;
	}
	public double getDirectionTowards(Position target) {
		return Math.atan2(target.getY()-y, target.getX()-x);
	}
	public Position add(double x, double y) {
		return new Position(this.x+x, this.y+y);
	}
	public Position addPolar(double magnitude, double direction) {
		return new Position(x+Math.cos(direction)*magnitude, y+Math.sin(direction)*magnitude);
	}
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Position position = (Position) o;
		return (Double.compare(position.getX(), x) == 0) && (Double.compare(position.getY(), y) == 0);
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
		return "Position(" + x + ", " + y + ")";
	}
}
