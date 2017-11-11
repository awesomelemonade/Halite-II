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
	public void move(Move move) {
		if(move instanceof ThrustMove) {
			thrustMoves.add((ThrustMove)move);
		}
		totalMoves.add(move);
	}
	public void flush(){
		Networking.sendMoves(totalMoves);
		totalMoves.clear();
		thrustMoves.clear();
	}
}
