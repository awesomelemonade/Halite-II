import java.util.ArrayList;
import java.util.List;

import hlt.GameConstants;
import hlt.Move;
import hlt.Networking;
import hlt.Position;
import hlt.ThrustMove;

public class MoveQueue {
	private List<Move> totalMoves;
	private List<ThrustMove> thrustMoves;
	public MoveQueue(){
		totalMoves = new ArrayList<Move>();
		thrustMoves = new ArrayList<ThrustMove>();
	}
	//returns blame?
	public void addMove(Move move){
		if(!(move instanceof ThrustMove)){
			totalMoves.add(move);
			return;
		}
	}
	private boolean intersect(ThrustMove moveA, ThrustMove moveB) {
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
