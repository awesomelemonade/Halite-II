package lemon.halite2.pathfinding;

import hlt.GameConstants;
import hlt.Position;
import lemon.halite2.util.Geometry;
import lemon.halite2.util.MathUtil;

public class EnemyShipObstacle implements Obstacle {
	private static final int MAGNITUDE = 7;
	private Position position;
	private int priority;
	public EnemyShipObstacle(Position position, int priority) {
		this.position = position;
		this.priority = priority;
	}
	@Override
	public boolean willCollide(Position position, Position velocity, double buffer) {
		if(Geometry.segmentCircleIntersection(position, position.add(velocity), this.position, buffer+GameConstants.SHIP_RADIUS+MAGNITUDE)) {
			for(int i=0;i<MathUtil.TAU_DEGREES;++i) {
				if(MathUtil.getMinDistanceSquared(this.position, Pathfinder.velocityVector[MAGNITUDE-1][i], position, velocity)<=(GameConstants.SHIP_RADIUS+buffer)*(GameConstants.SHIP_RADIUS*buffer)){
					return true;
				}
			}
		}
		return false;
	}
	@Override
	public int getPriority() {
		return priority;
	}
}
