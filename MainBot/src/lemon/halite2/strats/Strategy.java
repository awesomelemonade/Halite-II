package lemon.halite2.strats;

import java.util.List;

import lemon.halite2.util.MoveQueue;

public interface Strategy {
	public void init();
	public void newTurn();
	public int handleShip(List<Integer> handledShips, int shipId, MoveQueue moveQueue);
}
