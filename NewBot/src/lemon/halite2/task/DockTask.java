package lemon.halite2.task;

import hlt.GameConstants;
import hlt.Planet;
import hlt.Ship;
import lemon.halite2.pathfinding.Pathfinder;

public class DockTask implements Task {
	private Planet planet;
	public DockTask(Planet planet){
		this.planet = planet;
	}
	@Override
	public void accept(Ship ship) {
		if(ship.getPosition().getDistanceSquared(planet.getPosition())<=
				(planet.getRadius()+GameConstants.DOCK_RADIUS+GameConstants.SHIP_RADIUS+GameConstants.MAX_SPEED)*
				(planet.getRadius()+GameConstants.DOCK_RADIUS+GameConstants.SHIP_RADIUS+GameConstants.MAX_SPEED)){
			//try greediest
			
		}
	}
	@Override
	public void execute(Ship ship, Pathfinder pathfinder) {
		
	}
	@Override
	public double getScore(Ship ship) {
		return 0;
	}
}
