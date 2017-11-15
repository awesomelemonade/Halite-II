package lemon.halite2.util;

import hlt.Move;
import hlt.Networking;

public class MoveQueue {
	private StringBuilder builder;
	public MoveQueue(){
		builder = new StringBuilder();
	}
	public void add(Move move) {
		Networking.writeMove(builder, move);
	}
	public void flush(){
		Networking.send(builder.toString());
		builder = new StringBuilder();
	}
}
