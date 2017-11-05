package lemon.halite2.micro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hlt.GameMap;
import hlt.Position;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.Pathfinder;

public class MicroGame {
	private GameMap gameMap;
	private List<Set<Integer>> groups;
	private Map<Integer, Position> groupCenters;
	private Map<Integer, Double> groupBuffers;
	public MicroGame(GameMap gameMap) {
		this.gameMap = gameMap;
		groups = new ArrayList<Set<Integer>>();
		groupCenters = new HashMap<Integer, Position>();
		groupBuffers = new HashMap<Integer, Double>();
	}
	public void moveGroup(int group, Position target, double targetBuffer, MoveQueue moveQueue) {
		/*
		PathfindPlan plan = Pathfinder.pathfind(ship, start, target, buffer, targetBuffer);
		for(int shipId: groups.get(group)) {
			Ship ship = gameMap.getMyPlayer().getShip(shipId);
			moveQueue.addMove(new ThrustMove(ship, plan));
		}
		*/
	}
	//https://math.stackexchange.com/questions/1932858/how-to-find-the-smallest-enclosing-circle-over-a-set-of-circles
	public void calcEnclosingCircle(int group) {
		Position center = null;
		double buffer = 0;
		//TODO
		groupCenters.put(group, center);
		groupBuffers.put(group, buffer);
	}
}
