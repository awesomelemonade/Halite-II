package lemon.halite2.pathfinding;

import hlt.Vector;

public class MapBorderObstacle implements Obstacle {
	private Vector min;
	private Vector max;
	public MapBorderObstacle(Vector min, Vector max) {
		this.min = min;
		this.max = max;
	}
	@Override
	public boolean willCollide(Vector position, Vector velocity, double buffer) {
		double endX = position.getX()+velocity.getX();
		double endY = position.getY()+velocity.getY();
		return endX-buffer<=min.getX()||endX+buffer>=max.getX()||endY-buffer<=min.getY()||endY+buffer>=max.getY();
	}
}
