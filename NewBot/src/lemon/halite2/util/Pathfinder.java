package lemon.halite2.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import hlt.DebugLog;
import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Position;
import hlt.Ship;
import hlt.Ship.DockingStatus;
import hlt.ThrustMove.RoundPolicy;

public class Pathfinder {
	private static final double ONE_DEGREE = Math.toRadians(1);
	private static GameMap gameMap;
	private static List<Circle> staticObstacles;
	private static Map<Circle, Position> dynamicObstacles;
	
	public static void init(GameMap gameMap) {
		Pathfinder.gameMap = gameMap;
		staticObstacles = new ArrayList<Circle>();
		dynamicObstacles = new HashMap<Circle, Position>();
	}
	public static void addStaticObstacle(Circle circle) {
		staticObstacles.add(circle);
	}
	public static void addDynamicObstacle(Circle circle, Position velocity) {
		dynamicObstacles.put(circle, velocity);
	}
	public static void update() {
		staticObstacles.clear();
		for(Planet planet: gameMap.getPlanets()){
			staticObstacles.add(new Circle(planet.getPosition(), planet.getRadius()));
		}
		for(Ship ship: gameMap.getMyPlayer().getShips()){
			if(ship.getDockingStatus()!=DockingStatus.UNDOCKED){
				staticObstacles.add(new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS));
			}
		}
		dynamicObstacles.clear();
	}
	public static PathfindPlan pathfind(Position start, Position end, double buffer, double endBuffer) {
		DebugLog.log("\tPathfinding: "+start+" - "+end+" - "+buffer+" - "+endBuffer);
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
		targetDistance = Math.max(0, targetDistance-CORRECTION);
		double left = calcPlan(start, targetDistance, targetDirection, buffer, -1, Math.PI);
		double right = calcPlan(start, targetDistance, targetDirection, buffer, 1, Math.PI);
		DebugLog.log("\t\t"+left+" - "+right+" - "+targetDistance+" - "+targetDirection);
		if(left==0&&right==0) {
			return new PathfindPlan((int)Math.min(7, targetDistance), targetDirection, RoundPolicy.NONE);
		}
		if(left==-1&&right==-1) { //No Valid Directions
			return null;
		}
		if(left==-1) {
			return new PathfindPlan(7, targetDirection+right, RoundPolicy.NONE);
		}else if(right==-1){
			return new PathfindPlan(7, targetDirection-left, RoundPolicy.NONE);
		}else if(left<right) {
			return new PathfindPlan(7, targetDirection-left, RoundPolicy.NONE);
		}else {
			return new PathfindPlan(7, targetDirection+right, RoundPolicy.NONE);
		}
	}
	private static final double DETECT = 0.001;
	private static final double CORRECTION = 0.002;
	//Feed in degree-workable start and end
	private static double calcPlan(Position start, double magnitude, double direction, double buffer, int sign, double amount) {
		DebugLog.log("\t\tCalcPlan: "+start+" - "+magnitude+" - "+direction+" - "+buffer+" - "+sign+" - "+amount);
		Position end = start.addPolar(magnitude, direction);
		for(Circle circle: staticObstacles) {
			Position position = circle.getPosition();
			double radius = circle.getRadius()+buffer+DETECT;
			//DebugLog.log("Checking Parameters: "+start+" - "+end+" - "+position+" - "+radius);
			//DebugLog.log("Checking Output: "+Geometry.segmentPointDistance(start, end, position)+" - "+Geometry.segmentCircleIntersection(start, end, position, radius));
			if(Geometry.segmentCircleIntersection(start, end, position, radius)){
				double distance = start.getDistanceTo(position);
				if(distance<=radius){
					continue; //You're in the obstacle? - Skip the obstacle
				}
				DebugLog.log("\t\t\tIntersected with: "+position+" - "+radius);
				double tangentValue = Math.asin(Math.min((radius+CORRECTION)/distance, 1));
				double refDirection = start.getDirectionTowards(position);
				double theta = RoundPolicy.CEIL.apply(MathUtil.angleBetween(direction, refDirection+sign*tangentValue));
				if(theta<=amount) {
					double plan = calcPlan(start, 7, direction+sign*theta, buffer, sign, amount-theta);
					if(plan==-1) {
						return -1; //OH NOES
					}
					return theta+plan;
				}else {
					return -1; //OH NOES
				}
			}
		}
		Position velocity = new Position(magnitude*Math.cos(direction), magnitude*Math.sin(direction));
		for(Entry<Circle, Position> entry: dynamicObstacles.entrySet()) {
			if(checkCollisions(start, entry.getKey().getPosition(), velocity, entry.getValue(), buffer+entry.getKey().getRadius())) {
				if(ONE_DEGREE<amount) {
					double plan = calcPlan(start, 7, direction+sign*ONE_DEGREE, buffer, sign, amount-ONE_DEGREE);
					if(plan==-1) {
						return -1;
					}
					return ONE_DEGREE+plan;
				}else {
					return -1;
				}
			}
		}
		return 0;
	}
	public static boolean checkCollisions(Position a, Position b, Position velocityA, Position velocityB, double buffer) {
		double time = MathUtil.getMinTime(a, b, velocityA, velocityB);
		time = Math.max(0, Math.min(1, time)); //Clamp between 0 and 1
		return buffer*buffer>=MathUtil.getDistanceSquared(a, b, velocityA, velocityB, time);
	}
}
