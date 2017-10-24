package hlt;

public class Position {
	private double xPos;
	private double yPos;
	public Position(double xPos, double yPos) {
		this.xPos = xPos;
		this.yPos = yPos;
	}
	public double getXPos() {
		return xPos;
	}
	public double getYPos() {
		return yPos;
	}
	public double getDistanceTo(Position target) {
		double dx = xPos - target.getXPos();
		double dy = yPos - target.getYPos();
		return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
	}
	public int orientTowardsInDeg(Position target) {
		return Util.angleRadToDegClipped(orientTowardsInRad(target));
	}
	public double orientTowardsInRad(Position target) {
		double dx = target.getXPos() - xPos;
		double dy = target.getYPos() - yPos;

		return Math.atan2(dy, dx) + 2 * Math.PI;
	}
	public Position getClosestPoint(Entity target) {
		double radius = target.getRadius() + Constants.MIN_DISTANCE;
		double angleRad = target.getPosition().orientTowardsInRad(this);
		double dx = target.getPosition().getXPos() + radius * Math.cos(angleRad);
		double dy = target.getPosition().getYPos() + radius * Math.sin(angleRad);
		return new Position(dx, dy);
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
		return (Double.compare(position.xPos, xPos) == 0) && (Double.compare(position.yPos, yPos) == 0);
	}
	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(xPos);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(yPos);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	@Override
	public String toString() {
		return "Position(" + xPos + ", " + yPos + ")";
	}
}
