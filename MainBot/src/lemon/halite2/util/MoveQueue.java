package lemon.halite2.util;
import java.util.ArrayList;
import java.util.List;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Move;
import hlt.Networking;
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
	//returns blame?
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
			if(Pathfinder.segmentCircleIntersection(start, end, ship.getPosition(), 2*GameConstants.SHIP_RADIUS)){
				return ship.getId();
			}
		}
		totalMoves.add(thrustMove);
		thrustMoves.add(thrustMove);
		return -1;
	}
	private boolean intersects(ThrustMove moveA, ThrustMove moveB) {
		Position endA = moveA.getShip().getPosition().addPolar(moveA.getThrust(), moveA.getRoundedAngle());
		Position endB = moveB.getShip().getPosition().addPolar(moveB.getThrust(), moveB.getRoundedAngle());
		double a = Pathfinder.segmentPointDistance(moveA.getShip().getPosition(), endA, moveB.getShip().getPosition());
		double b = Pathfinder.segmentPointDistance(moveA.getShip().getPosition(), endA, endB);
		double c = Pathfinder.segmentPointDistance(moveB.getShip().getPosition(), endB, moveA.getShip().getPosition());
		double d = Pathfinder.segmentPointDistance(moveB.getShip().getPosition(), endB, endA);
		return Math.min(Math.min(a, b), Math.min(c, d))<=2*GameConstants.SHIP_RADIUS;
	}
	public void flush(){
		Networking.sendMoves(totalMoves);
		totalMoves.clear();
		thrustMoves.clear();
	}
}
