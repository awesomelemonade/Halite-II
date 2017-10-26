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
	
	private static final double FUDGE_FACTOR = 0.01f;
	
	public static void setGameMap(GameMap gameMap) {
		Pathfinder.gameMap = gameMap;
	}
	// Ignores Ships, but takes into account planets
	public static ThrustMove pathfind(Ship ship, Position start, Position end, double buffer) {
		return pathfind(ship, start, end, buffer, RoundPolicy.ROUND);
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
		
		//Continue Law of Sines if buffer not equal to 0
		double targetDistance = 0;
		if(buffer>realDistance*Math.sin(offsetDirection)){ //Ensures triangle to be solvable
			double lawOfSinesValue = Math.sin(offsetDirection)/buffer; //Divide by 0 error if buffer = 0
			double sineInverse = Math.PI-Math.asin(realDistance*lawOfSinesValue); //It's the one that is greater than Math.PI/2 radians
			targetDistance = Math.sin(Math.PI-sineInverse-offsetDirection)/lawOfSinesValue;
			//targetDistance = Math.sin(Math.asin(realDistance*lawOfSinesValue)-offsetDirection)/lawOfSinesValue;
		}else{
			targetDistance = realDistance*Math.cos(offsetDirection); //Not even sure if this ever happens
		}
		Position targetPosition = start.addPolar(targetDistance, targetDirection);
		//Target Vector to travel: (targetDistance, targetDirection) @ targetPosition
		for (Planet planet : gameMap.getPlanets()) {
			double calculatedBuffer = planet.getRadius()+GameConstants.SHIP_RADIUS+FUDGE_FACTOR;
			if(Geometry.segmentCircleIntersection(start, targetPosition, planet.getPosition(), calculatedBuffer)) {
				double distance = start.getDistanceTo(planet.getPosition());
				if(distance<=calculatedBuffer){
					//you're in the planet :(
					return new ThrustMove(ship, (int)Math.min(calculatedBuffer-distance, 7),
							planet.getPosition().getDirectionTowards(start), RoundPolicy.ROUND);
				}
				double tangentValue = Math.asin(calculatedBuffer/distance);
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
}
