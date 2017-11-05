package lemon.halite2.util;

import java.util.ArrayList;
import java.util.List;

import hlt.DebugLog;
import hlt.GameConstants;
import hlt.GameMap;
import hlt.Move;
import hlt.Networking;
import hlt.Planet;
import hlt.Position;
import hlt.Ship;
import hlt.ThrustMove;

public class MoveQueue {
	private GameMap gameMap;
	private List<Move> totalMoves;
	private List<ThrustMove> thrustMoves;
	public MoveQueue(GameMap gameMap){
		this.gameMap = gameMap;
		totalMoves = new ArrayList<Move>();
		thrustMoves = new ArrayList<ThrustMove>();
	}
	public int addMove(Move move){
		if(!(move instanceof ThrustMove)){
			totalMoves.add(move);
			return -1;
		}
		ThrustMove thrustMove = (ThrustMove)move;
		for(ThrustMove otherMove: thrustMoves){
			if(intersects(thrustMove, otherMove)){
				return otherMove.getShip().getId();
			}
		}
		for(Ship ship: gameMap.getMyPlayer().getShips()){
			if(ship.getId()==thrustMove.getShip().getId()){
				continue;
			}
			Position start = thrustMove.getShip().getPosition();
			Position end = start.addPolar(thrustMove.getThrust(), thrustMove.getRoundedAngle());
			if(Geometry.segmentCircleIntersection(start, end, ship.getPosition(), 2*GameConstants.SHIP_RADIUS)){
				return ship.getId();
			}
		}
		for(Planet planet: gameMap.getPlanets()){
			Position start = thrustMove.getShip().getPosition();
			Position end = start.addPolar(thrustMove.getThrust(), thrustMove.getRoundedAngle());
			if(Geometry.segmentCircleIntersection(start, end, planet.getPosition(), GameConstants.SHIP_RADIUS+planet.getRadius())){
				DebugLog.log("Parameters: "+start+" - "+end+" - "+planet.getPosition()+" - "+(planet.getRadius()+GameConstants.SHIP_RADIUS));
				DebugLog.log("Output: "+Geometry.segmentPointDistance(start, end, planet.getPosition())+" - "+Geometry.segmentCircleIntersection(start, end, planet.getPosition(), GameConstants.SHIP_RADIUS+planet.getRadius()));
				DebugLog.log("Crashing into planet? "+planet.getId()+" - "+thrustMove.getShip().getId());
				//throw new RuntimeException("CRASH: "+planet.getId()+" - "+thrustMove.getShip().getId());
			}
		}
		totalMoves.add(thrustMove);
		thrustMoves.add(thrustMove);
		return -1;
	}
	private boolean intersects(ThrustMove moveA, ThrustMove moveB) {
		Position a = moveA.getShip().getPosition();
		Position b = moveB.getShip().getPosition();
		Position velocityA = new Position(moveA.getThrust()*Math.cos(moveA.getRoundedAngle()), moveA.getThrust()*Math.sin(moveA.getRoundedAngle()));
		Position velocityB = new Position(moveB.getThrust()*Math.cos(moveB.getRoundedAngle()), moveB.getThrust()*Math.sin(moveB.getRoundedAngle()));
		return checkCollisions(a, b, velocityA, velocityB, 2*GameConstants.SHIP_RADIUS);
	}
	public void flush(){
		Networking.sendMoves(totalMoves);
		totalMoves.clear();
		thrustMoves.clear();
	}
	public boolean checkCollisions(Position a, Position b, Position velocityA, Position velocityB, double buffer) {
		double time = getMinTime(a, b, velocityA, velocityB);
		time = Math.max(0, Math.min(1, time)); //Clamp between 0 and 1
		return buffer*buffer>=getDistanceSquared(a, b, velocityA, velocityB, time);
	}
	//https://gamedev.stackexchange.com/questions/97337/detect-if-two-objects-are-going-to-collide
	public double getMinTime(Position a, Position b, Position velocityA, Position velocityB) {
		double deltaVelocityX = velocityA.getX()-velocityB.getX();
		double deltaVelocityY = velocityA.getY()-velocityB.getY();
		return -(deltaVelocityX*(a.getX()-b.getX())+deltaVelocityY*(a.getY()-b.getY()))/
				(deltaVelocityX*deltaVelocityX+deltaVelocityY*deltaVelocityY);
	}
	public double getDistanceSquared(Position a, Position b, Position velocityA, Position velocityB, double time) {
		double deltaX = (a.getX()+velocityA.getX()*time)-(b.getX()+velocityB.getX()*time);
		double deltaY = (a.getY()+velocityA.getY()*time)-(b.getY()+velocityB.getY()*time);
		return deltaX*deltaX+deltaY*deltaY;
	}
}
