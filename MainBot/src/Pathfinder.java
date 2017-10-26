import hlt.DebugLog;
import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Position;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustMove.RoundPolicy;

public class Pathfinder {
	private static GameMap gameMap;

	public static void setGameMap(GameMap gameMap) {
		Pathfinder.gameMap = gameMap;
	}
	public static double calculateTangent(Position position, Position center, double radius) {
		double distance = position.getDistanceTo(center);
		if (distance < radius) { // No Tangent, you're in the circle
			//Return direction away from center; scales based off distance
			return RoundPolicy.FLOOR.apply(center.getDirectionTowards(position) - (Math.PI / 2) * (distance / radius));
		}
		return RoundPolicy.CEIL.apply(position.getDirectionTowards(center) + Math.asin(radius / distance));
	}
	// Ignores Ships, but takes into account planets
	public static ThrustMove pathfind(Ship ship, Position start, Position end, double startBuffer, double endBuffer) {
		return pathfind(ship, start, end.addPolar(endBuffer, end.getDirectionTowards(start)), startBuffer);
	}
	public static ThrustMove pathfind(Ship ship, Position start, Position end, double buffer) {
		double realDirection = start.getDirectionTowards(end);
		double targetDirection = RoundPolicy.ROUND.apply(realDirection);
		//Solves Law of Sines
		double offsetDirection = Math.abs(targetDirection-realDirection);
		double realDistance = start.getDistanceTo(end);
		//Round Buffer
		buffer = Math.max(buffer, realDistance*Math.sin(offsetDirection));
		//Continue Law of Sines if buffer not equal to 0
		double targetDistance = 0;
		if(buffer>0){
			double lawOfSinesValue = Math.sin(offsetDirection)/buffer; //Divide by 0 error if buffer = 0
			targetDistance = Math.sin(Math.PI-Math.asin(realDistance*lawOfSinesValue)-offsetDirection)/lawOfSinesValue;
		}else{
			targetDistance = realDistance*Math.cos(offsetDirection);
		}
		Position targetPosition = start.addPolar(targetDistance, targetDirection);
		//Target Vector to travel: (targetDistance, targetDirection) @ targetPosition
		for (Planet planet : gameMap.getPlanets()) {
			if(segmentCircleIntersection(start, targetPosition, planet.getPosition(), planet.getRadius()+GameConstants.SHIP_RADIUS)) {
				return pathfind(ship, start,
						start.addPolar(Math.sqrt(start.getDistanceSquared(planet.getPosition())-planet.getRadius()*planet.getRadius()),
								calculateTangent(start, planet.getPosition(), planet.getRadius()+GameConstants.SHIP_RADIUS)), 0);
			}
		}
		return new ThrustMove(ship, (int)Math.min(targetDistance, 7), targetDirection, RoundPolicy.NONE);
	}
	// Arc - Circle
	// Segment - Point Distance
	public static double segmentPointDistance(Position start, Position end, Position point) {
		// Standard form of the line ax+by+c = 0
		double a = start.getY() - end.getY();
		double b = end.getX() - start.getX();
		double c = (start.getX() - end.getX()) * start.getY() + (end.getY() - start.getY()) * start.getX();
		// Calculate closest to point on the line above
		double x = (b * (b * point.getX() - a * point.getY()) - a * c) / (a * a + b * b);
		double y = (a * (-b * point.getX() + a * point.getY()) - b * c) / (a * a + b * b);
		// Check if (x, y) is between start and end
		if (((start.getX() <= x && x <= end.getX()) || (start.getX() >= x && x >= end.getX()))
				&& ((start.getY() <= y && y <= end.getY()) || (start.getY() >= y && y >= end.getY()))) {
			return Math.sqrt((point.getX() - x) * (point.getX() - x) + (point.getY() - y) * (point.getY() - y));
		} else {
			double i = point.getDistanceSquared(start);
			double j = point.getDistanceSquared(end);
			return i < j ? Math.sqrt(i) : Math.sqrt(j);
		}
	}
	public static boolean segmentCircleIntersection(Position a, Position b, Position center, double buffer) {
		return segmentPointDistance(a, b, center)<=buffer;
	}
}
