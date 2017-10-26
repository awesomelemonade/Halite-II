package lemon.halite2.util;
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
	// Ignores Ships, but takes into account planets
	public static ThrustMove pathfind(Ship ship, Position start, Position end, double startBuffer, double endBuffer) {
		return pathfind(ship, start, end.addPolar(endBuffer, end.getDirectionTowards(start)), startBuffer, RoundPolicy.ROUND);
	}
	//Offset Policy: -1 = OFFSET NEGATIVE DIR; 1 = OFFSET POSITIVE DIR; 0 = NO PREFERENCE
	public static ThrustMove pathfind(Ship ship, Position start, Position end, double buffer, RoundPolicy offsetPolicy) {
		if(offsetPolicy==RoundPolicy.NONE){
			throw new IllegalArgumentException("OffsetPolicy cannot be NONE");
		}
		double realDirection = start.getDirectionTowards(end);
		double targetDirection = offsetPolicy.apply(realDirection);
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
				double distance = start.getDistanceTo(planet.getPosition());
				if(distance<=planet.getRadius()+GameConstants.SHIP_RADIUS){
					//you're in the planet :(
					return new ThrustMove(ship, (int)Math.min(planet.getRadius()+GameConstants.SHIP_RADIUS-distance, 7),
							planet.getPosition().getDirectionTowards(start), RoundPolicy.ROUND);
				}
				double tangentValue = Math.asin((planet.getRadius()+GameConstants.SHIP_RADIUS)/distance);
				double magnitude = Math.sqrt(start.getDistanceSquared(planet.getPosition())-planet.getRadius()*planet.getRadius());
				double direction = start.getDirectionTowards(planet.getPosition());
				RoundPolicy newOffsetPolicy = RoundPolicy.NONE;
				if(Math.abs((direction-tangentValue)-targetDirection)<Math.abs((direction+tangentValue)-targetDirection)){
					direction+=tangentValue;
					newOffsetPolicy = RoundPolicy.CEIL;
				}else{
					direction-=tangentValue;
					newOffsetPolicy = RoundPolicy.FLOOR;
				}
				Position endPoint = start.addPolar(magnitude, direction);
				return pathfind(ship, start, endPoint, 0, newOffsetPolicy);
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
