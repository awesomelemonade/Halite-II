package lemon.halite2.util;

import java.util.HashSet;
import java.util.Set;

import hlt.Move;
import hlt.Networking;

public class MoveQueue {
	private Set<Integer> moved;
	public MoveQueue() {
		moved = new HashSet<Integer>();
	}
	public void add(Move move) {
		moved.add(move.getShipId());
		StringBuilder builder = new StringBuilder();
		Networking.writeMove(builder, move);
		Networking.send(builder.toString());
	}
	public boolean hasMoved(int shipId) {
		return moved.contains(shipId);
	}
	public void flush(){
		moved.clear();
		Networking.flush();
	}
}
