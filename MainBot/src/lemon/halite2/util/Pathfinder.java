package lemon.halite2.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
	private static Map<Position, Double> obstacles;
	
	public static void init(GameMap gameMap) {
		Pathfinder.gameMap = gameMap;
		obstacles = new HashMap<Position, Double>();
	}
	public static void update() {
		obstacles.clear();
		for(Planet planet: gameMap.getPlanets()){
			obstacles.put(planet.getPosition(), planet.getRadius());
		}
		for(Ship otherShip: gameMap.getMyPlayer().getShips()){
			obstacles.put(otherShip.getPosition(), GameConstants.SHIP_RADIUS);
		}
	}
	public static ThrustMove pathfind(Ship ship, Position start, Position end) {
		return pathfind(ship, start, end, 0, 0);
	}
	public static ThrustMove pathfind(Ship ship, Position start, Position end, double buffer, double endBuffer) {
		if(start.getDistanceSquared(end)<buffer*buffer){
			return new ThrustMove(ship, 0, 0, RoundPolicy.NONE);
		}
		double realDirection = start.getDirectionTowards(end);
		double targetDirection = RoundPolicy.ROUND.apply(realDirection);
		//Solves Law of Sines
		double offsetDirection = Math.abs(targetDirection-realDirection);
		double realDistance = start.getDistanceTo(end);
		double targetDistance = 0;
		if(offsetDirection>0&&(buffer+endBuffer)<realDistance&&(buffer+endBuffer)>=realDistance*Math.sin(offsetDirection)){ //Ensures triangle to be solvable
			double lawOfSinesValue = Math.sin(offsetDirection)/(buffer+endBuffer); //Divide by 0 error if buffer = 0
			double sineInverse = Math.PI-Math.asin(realDistance*lawOfSinesValue); //It's the one that is greater than Math.PI/2 radians
			targetDistance = Math.sin(Math.PI-sineInverse-offsetDirection)/lawOfSinesValue;
			//targetDistance = Math.sin(Math.asin(realDistance*lawOfSinesValue)-offsetDirection)/lawOfSinesValue;
		}else{
			if(offsetDirection==0||buffer+endBuffer>realDistance) {
				targetDistance = Math.max(0, realDistance-(buffer+endBuffer));
			}else {
				targetDistance = realDistance*Math.cos(offsetDirection);
			}
		}
		Position targetPosition = start.addPolar(targetDistance, targetDirection);
		double left = calcPlan(start, targetPosition, buffer, -1, Math.PI, obstacles);
		double right = calcPlan(start, targetPosition, buffer, 1, Math.PI, obstacles);
		if(left==0&&right==0) {
			return new ThrustMove(ship, (int)Math.min(7, targetDistance), targetDirection, RoundPolicy.NONE);
		}
		if(left==-1&&right==-1) { //No Valid Directions
			return new ThrustMove(ship, 0, 0, RoundPolicy.NONE);
		}
		if(left==-1) {
			return new ThrustMove(ship, 7, targetDirection+right, RoundPolicy.NONE);
		}else if(right==-1){
			return new ThrustMove(ship, 7, targetDirection-left, RoundPolicy.NONE);
		}else if(left<right) {
			return new ThrustMove(ship, 7, targetDirection-left, RoundPolicy.NONE);
		}else {
			return new ThrustMove(ship, 7, targetDirection+right, RoundPolicy.NONE);
		}
	}
	//Feed in degree-workable start and end
	private static double calcPlan(Position start, Position end, double buffer, int sign, double amount, Map<Position, Double> obstacles) {
		DebugLog.log("\t\t"+start+" - "+end+" - "+buffer+" - "+sign+" - "+amount);
		double realDirection = start.getDirectionTowards(end);
		for(Entry<Position, Double> entry: obstacles.entrySet()) {
			Position position = entry.getKey();
			double radius = entry.getValue()+buffer;
			if(Geometry.segmentCircleIntersection(start, end, position, radius)){
				DebugLog.log("\t\t\tIntersected with: "+position+" - "+radius);
				double distance = start.getDistanceTo(position);
				if(distance<=radius){
					continue; //You're in the obstacle? - Skip the obstacle
				}
				double tangentValue = Math.asin((radius+0.001)/distance);
				double direction = start.getDirectionTowards(position);
				double theta = RoundPolicy.CEIL.apply(MathUtil.angleBetween(realDirection, direction+sign*tangentValue));
				if(theta<=amount) {
					Position newPosition = start.addPolar(7, realDirection+sign*theta);
					double plan = calcPlan(start, newPosition, buffer, sign, amount-theta, obstacles);
					if(plan==-1) {
						return -1; //OH NOES
					}
					return theta+plan;
				}else {
					return -1; //OH NOES
				}
			}
		}
		return 0;
	}
}
