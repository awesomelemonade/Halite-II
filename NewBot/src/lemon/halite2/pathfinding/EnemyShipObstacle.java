package lemon.halite2.pathfinding;

import hlt.GameConstants;
import hlt.Vector;
import lemon.halite2.util.Geometry;
import lemon.halite2.util.MathUtil;

public class EnemyShipObstacle implements Obstacle {
	private Vector position;
	public EnemyShipObstacle(Vector position) {
		this.position = position;
	}
	@Override
	public boolean willCollide(Vector position, Vector velocity, double buffer) {
		if(velocity.equals(Vector.ZERO)) {
			return this.position.getDistanceSquared(position)<=
					(buffer+GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS+GameConstants.MAX_SPEED)*
					(buffer+GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS+GameConstants.MAX_SPEED);
		}else {
			if(Geometry.segmentCircleIntersection(position, position.add(velocity), this.position, buffer+GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS+GameConstants.MAX_SPEED)) {
				for(int i=0;i<MathUtil.TAU_DEGREES;++i) {
					if(MathUtil.getMinDistanceSquared(this.position, Pathfinder.velocityVector[GameConstants.MAX_SPEED-1][i], position, velocity)
							<=(GameConstants.WEAPON_RADIUS+GameConstants.SHIP_RADIUS+buffer)*(GameConstants.WEAPON_RADIUS+GameConstants.SHIP_RADIUS+buffer)){
						return true;
					}
				}
			}
			return false;
		}
	}
}
