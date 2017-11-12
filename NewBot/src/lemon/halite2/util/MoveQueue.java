package lemon.halite2.util;

import java.util.ArrayList;
import java.util.List;

import hlt.Move;
import hlt.Networking;
import hlt.ThrustMove;

public class MoveQueue {
	private List<Move> totalMoves;
	private List<ThrustMove> thrustMoves;
	public MoveQueue(){
		totalMoves = new ArrayList<Move>();
		thrustMoves = new ArrayList<ThrustMove>();
	}
	public void add(Move move) {
		totalMoves.add(move);
	}
	public void flush(){
		Networking.sendMoves(totalMoves);
		totalMoves.clear();
		thrustMoves.clear();
	}
}
