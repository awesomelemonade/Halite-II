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
		return endX<=min.getX()||endX>=max.getX()||endY<=min.getY()||endY>=max.getY();
	}
}
