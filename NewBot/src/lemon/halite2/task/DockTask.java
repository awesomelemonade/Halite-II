package lemon.halite2.task;

import java.util.Map;
import java.util.Set;

import hlt.DockMove;
import hlt.GameConstants;
import hlt.Move;
import hlt.Planet;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustPlan;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.MathUtil;

public class DockTask implements Task {
	private Planet planet;
	private double innerBufferSquared;
	private double outerBufferSquared;
	public DockTask(Planet planet){
		this.planet = planet;
		this.innerBufferSquared = (planet.getRadius()+GameConstants.DOCK_RADIUS);
		this.outerBufferSquared = (innerBufferSquared+GameConstants.MAX_SPEED);
		innerBufferSquared = innerBufferSquared*innerBufferSquared;
		outerBufferSquared = outerBufferSquared*outerBufferSquared;
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
	public Move execute(Ship ship, Pathfinder pathfinder, Map<Integer, Set<Integer>> blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		double distanceSquared = ship.getPosition().getDistanceSquared(planet.getPosition());
		if(distanceSquared<=innerBufferSquared) {
			return new DockMove(ship.getId(), planet.getId());
		}
		if(distanceSquared<=outerBufferSquared) {
			//try greediest
			for(int i=1;i<=7;++i) {
				for(int j=0;j<MathUtil.TAU_DEGREES;++j) {
					if(ship.getPosition().add(Pathfinder.velocityVector[i-1][j]).getDistanceSquared(planet.getPosition())<=innerBufferSquared) {
						if(pathfinder.getCandidate(i, j, ObstacleType.PERMANENT)==null) {
							if(pathfinder.getCandidate(i, j, ObstacleType.UNCERTAIN)==null) {
								return new ThrustMove(ship.getId(), new ThrustPlan(i, j));
							}
						}
					}
				}
			}
		}
		return null;
	}
	@Override
	public double getScore(Ship ship) {
		return -ship.getPosition().getDistanceSquared(planet.getPosition());
	}
}
