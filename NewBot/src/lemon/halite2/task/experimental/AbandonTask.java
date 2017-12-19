package lemon.halite2.task.experimental;

import hlt.GameMap;
import hlt.Move;
import hlt.Planet;
import hlt.Ship;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.task.BlameMap;
import lemon.halite2.task.Task;
import lemon.halite2.util.BiMap;

public class AbandonTask implements Task {
	private static final double FACTOR = 0.75;
	private static boolean abandon = false;
	public static void checkAbandon() {
		if(GameMap.INSTANCE.getPlayers().size()==4) {
			abandon = false;
			int[] count = new int[4];
			for(Planet planet: GameMap.INSTANCE.getPlanets()) {
				if(planet.isOwned()) {
					count[planet.getOwner()]++;
				}
			}
			for(int i=0;i<count.length;++i) {
				if(i==GameMap.INSTANCE.getMyPlayerId()) { //Don't abandon if you're winning!
					continue;
				}
				if(((double)count[i])/((double)GameMap.INSTANCE.getPlanets().size())>FACTOR) {
					abandon = true;
				}
			}
		}
	}
	@Override
	public void accept(Ship ship) {
		//Assign to ship from team #3 or #4 (using a map)
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		return null;
	}
	@Override
	public double getScore(Ship ship) {
		return abandon?Double.MAX_VALUE:-Double.MAX_VALUE;
	}
}
