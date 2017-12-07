package lemon.halite2.util;

import hlt.Move;
import hlt.Networking;

public class MoveQueue {
	public void add(Move move) {
		StringBuilder builder = new StringBuilder();
		Networking.writeMove(builder, move);
		Networking.send(builder.toString());
	}
	public void flush(){
		Networking.flush();
	}
}
