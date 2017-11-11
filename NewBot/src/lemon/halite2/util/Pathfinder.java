package lemon.halite2.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import hlt.DebugLog;
import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Position;
import hlt.RoundPolicy;
import hlt.Ship;
import hlt.Ship.DockingStatus;
import hlt.ThrustPlan;

public class Pathfinder {
	private static GameMap gameMap;
	
	private Position start;
	private Position end;
	private double buffer;
	private double endBuffer;
	
	private int magnitude;
	private int directionDegrees;
	private int offsetCounterClockwise;
	private int offsetClockwise;
	
	private boolean[] staticDirections;
	
	public Pathfinder(Position start, Position end, double buffer, double endBuffer) {
		this.start = start;
		this.end = end;
		this.buffer = buffer;
		this.endBuffer = endBuffer;
		this.magnitude = (int)Math.min(7, start.getDistanceTo(end)-buffer-endBuffer);
		this.directionDegrees = RoundPolicy.ROUND.applyDegrees(start.getDirectionTowards(end));
		this.offsetClockwise = 0;
		this.offsetCounterClockwise = 0;
		this.staticDirections = new boolean[360];
	}
	public int resolveConflicts() { //Returns conflicting obstacle?
		//Resolve Uncertain Regions
		for(Circle region: Obstacles.getUncertainRegions()) {
			
		}
		return -1;
	}
	public void resolveStaticObstacles() {
		//Resolve Static Obstacles
		for(Circle obstacle: Obstacles.getStaticObstacles()) {
			double distSquared = start.getDistanceSquared(obstacle.getPosition());
			if(distSquared<(obstacle.getRadius()+magnitude)*(obstacle.getRadius()+magnitude)&&
					!obstacle.contains(start)){
				resolveStaticObstacle(obstacle, staticDirections);
			}
		}
	}
	public void resolveStaticObstacle(Circle circle, boolean[] directions) {
		double direction = start.getDirectionTowards(circle.getPosition());
		double theta = Math.asin((circle.getRadius()+buffer)/start.getDistanceTo(circle.getPosition()));
		int start = RoundPolicy.COUNTER_CLOCKWISE.applyDegrees(direction-theta);
		int end = RoundPolicy.CLOCKWISE.applyDegrees(direction+theta);
		for(int i=start;i!=end;i=(i+1)%360) {
			directions[i] = true;
		}
	}
	public void resolveDynamicObstacle(Circle circle, ThrustPlan plan, boolean[] directions) {
		Position velocity = new Position(plan.getThrust()*Math.cos(Math.toRadians(plan.getAngle())),
				plan.getThrust()*Math.sin(Math.toRadians(plan.getAngle())));
		Position endPosition = circle.getPosition().addPolar(plan.getThrust(), Math.toRadians(plan.getAngle()));
		double direction = start.getDirectionTowards(circle.getPosition());
		double direction2 = start.getDirectionTowards(endPosition);
		double theta = Math.asin((circle.getRadius()+buffer)/start.getDistanceTo(circle.getPosition()));
		double theta2 = Math.asin((circle.getRadius()+buffer)/start.getDistanceTo(endPosition));
		int a = RoundPolicy.COUNTER_CLOCKWISE.applyDegrees(direction-theta);
		int b = RoundPolicy.CLOCKWISE.applyDegrees(direction2+theta2);
		int c = RoundPolicy.CLOCKWISE.applyDegrees(direction+theta);
		int d = RoundPolicy.COUNTER_CLOCKWISE.applyDegrees(direction2-theta2);
		int start, end;
		if(MathUtil.angleBetweenDegrees(a, b)>MathUtil.angleBetweenDegrees(c, d)) {
			if(MathUtil.normalizeDegrees(b-a)>MathUtil.PI_DEGREES) {
				start = b;
				end = a;
			}else {
				start = a;
				end = b;
			}
		}else {
			if(MathUtil.normalizeDegrees(d-c)>MathUtil.PI_DEGREES) {
				start = d;
				end = c;
			}else {
				start = c;
				end = d;
			}
		}
		for(int i=start;i!=end;i=(i+1)%360) {
			Position velocity2 = new Position(magnitude*Math.cos(Math.toRadians(i)), magnitude*Math.sin(Math.toRadians(i)));
			if(checkCollisions(circle.getPosition(), this.start, velocity, velocity2, buffer+circle.getRadius())) {
				directions[i] = true;
			}
		}
	}
	public ThrustPlan getThrustPlan() {
		if(offsetCounterClockwise==-1&&offsetClockwise==-1) {
			return null;
		}
		if(offsetCounterClockwise==-1) {
			return new ThrustPlan(magnitude, directionDegrees+offsetClockwise);
		}
		if(offsetClockwise==-1) {
			return new ThrustPlan(magnitude, directionDegrees-offsetCounterClockwise);
		}
		if(offsetClockwise<offsetCounterClockwise) {
			return new ThrustPlan(magnitude, directionDegrees+offsetClockwise);
		}else {
			return new ThrustPlan(magnitude, directionDegrees-offsetCounterClockwise);
		}
	}
	
	public static void init(GameMap gameMap) {
		Pathfinder.gameMap = gameMap;
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
		DebugLog.log("Ideal Case: "+targetDistance+" - "+targetDirection);
		double left = calcPlan(start, targetDistance, targetDirection, buffer, -1, Math.PI);
		double right = calcPlan(start, targetDistance, targetDirection, buffer, 1, Math.PI);
		DebugLog.log("\t\tleft="+left+";right="+right+";targetDistance="+targetDistance+";targetDirection="+targetDirection);
		if(left==0&&right==0) {
			targetDistance = (int)Math.min(7, targetDistance);
			if(targetDistance==0) {
				return null;
			}
			return new PathfindPlan((int)targetDistance, targetDirection, RoundPolicy.NONE);
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
			if(Geometry.segmentCircleIntersection(start, end, position, radius)){
				double distance = start.getDistanceTo(position);
				if(distance<=radius){
					continue; //You're in the obstacle? - Skip the obstacle
				}
				DebugLog.log("\t\t\tIntersected with: "+position+" - "+radius);
				double tangentValue = Math.asin(Math.min((radius+CORRECTION)/distance, 1));
				double refDirection = start.getDirectionTowards(position);
				double theta = Math.ceil(MathUtil.angleBetween(direction, refDirection+sign*tangentValue));
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
		magnitude = (int)Math.min(7, magnitude);
		Position velocity = new Position(magnitude*Math.cos(direction), magnitude*Math.sin(direction));
		for(Entry<Circle, Position> entry: dynamicObstacles.entrySet()) {
			if(checkCollisions(start, entry.getKey().getPosition(), velocity, entry.getValue(), buffer+entry.getKey().getRadius()+DETECT)) {
				if(ONE_DEGREE<=amount) {
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
		DebugLog.log("Solving: "+a+" - "+b+" - "+velocityA+" - "+velocityB+" - "+buffer);
		double time = MathUtil.getMinTime(a, b, velocityA, velocityB);
		time = Math.max(0, Math.min(1, time)); //Clamp between 0 and 1
		DebugLog.log("Checking Collisions: "+MathUtil.getDistanceSquared(a, b, velocityA, velocityB, time)+" - "+(buffer*buffer));
		return buffer*buffer>=MathUtil.getDistanceSquared(a, b, velocityA, velocityB, time);
	}
}
