package lemon.halite2.strats;

import lemon.halite2.util.MoveQueue;

public interface Strategy {
	public void init();
	public void newTurn(MoveQueue moveQueue);
}
