package lemon.halite2.util;

import java.util.ArrayList;
import java.util.List;

import hlt.Entity;
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
	public static ThrustMove pathfind(Ship ship, Position start, Position end) {
		return pathfind(ship, start, end, 0);
	}
	public static ThrustMove pathfind(Ship ship, Position start, Position end, double buffer) {
		if(start.getDistanceSquared(end)<buffer*buffer) { //already there
			return new ThrustMove(ship, 0, 0, RoundPolicy.NONE);
		}
		return pathfind(ship, start, end.addPolar(buffer, end.getDirectionTowards(start)), RoundPolicy.ROUND);
	}
	private static int lastShipId = -1;
	private static List<Entity> entitiesPathfinded = new ArrayList<Entity>();
	//Offset Policy: -1 = OFFSET NEGATIVE DIR; 1 = OFFSET POSITIVE DIR; 0 = NO PREFERENCE
	public static ThrustMove pathfind(Ship ship, Position start, Position end, RoundPolicy offsetPolicy) {
		if(lastShipId!=ship.getId()) {
			lastShipId = ship.getId();
			entitiesPathfinded.clear();
		}
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
		/*if(buffer>realDistance*Math.sin(offsetDirection)){ //Ensures triangle to be solvable
			double lawOfSinesValue = Math.sin(offsetDirection)/buffer; //Divide by 0 error if buffer = 0
			double sineInverse = Math.PI-Math.asin(realDistance*lawOfSinesValue); //It's the one that is greater than Math.PI/2 radians
			targetDistance = Math.sin(Math.PI-sineInverse-offsetDirection)/lawOfSinesValue;
			//targetDistance = Math.sin(Math.asin(realDistance*lawOfSinesValue)-offsetDirection)/lawOfSinesValue;
		}else{
			targetDistance = realDistance*Math.cos(offsetDirection);
		}*/
		targetDistance = 7;
		Position targetPosition = start.addPolar(targetDistance, targetDirection);
		//Target Vector to travel: (targetDistance, targetDirection) @ targetPosition
		for (Planet planet : gameMap.getPlanets()) {
			ThrustMove move = testCollision(ship, start, targetPosition, targetDirection, planet.getPosition(), planet.getRadius(), planet);
			if(move!=null) {
				return move;
			}
		}
		for (Ship otherShip: gameMap.getShips()) {
			if(otherShip.getOwner()==gameMap.getMyPlayerId()&&otherShip.getId()!=ship.getId()) {
				ThrustMove move = testCollision(ship, start, targetPosition, targetDirection, otherShip.getPosition(), otherShip.getRadius(), otherShip);
				if(move!=null) {
					return move;
				}
			}
		}
		return new ThrustMove(ship, (int)Math.min(targetDistance, 7), targetDirection, RoundPolicy.NONE);
	}
	public static ThrustMove testCollision(Ship ship, Position start, Position targetPosition, double targetDirection, Position position, double radius, Entity entity) {
		double calculatedBuffer = radius+GameConstants.SHIP_RADIUS+FUDGE_FACTOR;
		if(Geometry.segmentCircleIntersection(start, targetPosition, position, calculatedBuffer)) {
			if(entitiesPathfinded.contains(entity)) {
				return new ThrustMove(ship, 0, 0, RoundPolicy.NONE);
			}
			double distance = start.getDistanceTo(position);
			if(distance<=calculatedBuffer){
				//you're in the planet :(
				return new ThrustMove(ship, (int)Math.min(calculatedBuffer-distance, 7),
						position.getDirectionTowards(start), RoundPolicy.ROUND);
			}
			double tangentValue = Math.asin(calculatedBuffer/distance);
			double magnitude = Math.sqrt(start.getDistanceSquared(position)-radius*radius);
			double direction = start.getDirectionTowards(position);
			RoundPolicy newOffsetPolicy = RoundPolicy.NONE;
			//if(Math.abs((direction-tangentValue)-targetDirection)<Math.abs((direction+tangentValue)-targetDirection)){
				direction+=tangentValue;
				newOffsetPolicy = RoundPolicy.CEIL;
			//}else{
			//	direction-=tangentValue;
			//	newOffsetPolicy = RoundPolicy.FLOOR;
			//}
			Position endPoint = start.addPolar(magnitude, direction);
			entitiesPathfinded.add(entity);
			return pathfind(ship, start, endPoint, newOffsetPolicy);
		}
		return null;
	}
}
