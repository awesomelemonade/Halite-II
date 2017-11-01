package lemon.halite2.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
	public static ThrustMove pathfind(Ship ship, Position start, Position end) {
		return pathfind(ship, start, end, 0);
	}
	//Offset Policy: -1 = OFFSET NEGATIVE DIR; 1 = OFFSET POSITIVE DIR; 0 = NO PREFERENCE
	public static ThrustMove pathfind(Ship ship, Position start, Position end, double buffer) {
		if(start.getDistanceSquared(end)<buffer*buffer){
			return new ThrustMove(ship, 0, 0, RoundPolicy.NONE);
		}
		double realDirection = start.getDirectionTowards(end);
		double targetDirection = RoundPolicy.ROUND.apply(realDirection);
		//Solves Law of Sines
		double offsetDirection = Math.abs(targetDirection-realDirection);
		double realDistance = start.getDistanceTo(end);
		double targetDistance = 0;
		if(buffer>realDistance*Math.sin(offsetDirection)){ //Ensures triangle to be solvable
			double lawOfSinesValue = Math.sin(offsetDirection)/buffer; //Divide by 0 error if buffer = 0
			double sineInverse = Math.PI-Math.asin(realDistance*lawOfSinesValue); //It's the one that is greater than Math.PI/2 radians
			targetDistance = Math.sin(Math.PI-sineInverse-offsetDirection)/lawOfSinesValue;
			//targetDistance = Math.sin(Math.asin(realDistance*lawOfSinesValue)-offsetDirection)/lawOfSinesValue;
		}else{
			targetDistance = realDistance*Math.cos(offsetDirection);
		}
		Position targetPosition = start.addPolar(targetDistance, targetDirection);
		//Target Vector to travel: (targetDistance, targetDirection) @ targetPosition
		Map<Position, Double> obstacles = new HashMap<Position, Double>();
		for(Planet planet: gameMap.getPlanets()){
			obstacles.put(planet.getPosition(), planet.getRadius());
		}
		for(Ship otherShip: gameMap.getMyPlayer().getShips()){
			if(otherShip.getId()==ship.getId()){
				continue;
			}
			obstacles.put(otherShip.getPosition(), GameConstants.SHIP_RADIUS);
		}
		double leftMargin = 0; //Deviating from targetDirection
		double rightMargin = 0; //Deviating from targetDirection
		for(Entry<Position, Double> entry: obstacles.entrySet()){
			Position position = entry.getKey();
			double radius = entry.getValue()+buffer;
			if(Geometry.segmentCircleIntersection(start, targetPosition, position, radius)){
				double distance = start.getDistanceTo(position);
				if(distance<=radius){
					//You're in the obstacle? whaaaaa
					continue; //Skip the obstacle
				}
				double tangentValue = Math.asin(radius/distance);
				double direction = start.getDirectionTowards(position);
				double directionA = RoundPolicy.CEIL.apply(MathUtil.angleBetween(targetDirection, direction-tangentValue));
				double directionB = RoundPolicy.CEIL.apply(MathUtil.angleBetween(targetDirection, direction+tangentValue));
				leftMargin = Math.min(Math.PI, directionA);
				rightMargin = Math.min(Math.PI, directionB);
			}
		}
		if(leftMargin==0&&rightMargin==0){
			return new ThrustMove(ship, (int)Math.min(targetDistance, 7), targetDirection, RoundPolicy.NONE);
		}
		if(leftMargin<rightMargin){
			return new ThrustMove(ship, 7, targetDirection-leftMargin, RoundPolicy.NONE);
		}else{
			return new ThrustMove(ship, 7, targetDirection+rightMargin, RoundPolicy.NONE);
		}
	}
}
