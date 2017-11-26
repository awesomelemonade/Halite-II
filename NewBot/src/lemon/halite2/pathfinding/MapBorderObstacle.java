package lemon.halite2.pathfinding;

import hlt.Position;

public class MapBorderObstacle implements Obstacle {
	private Position min;
	private Position max;
	public MapBorderObstacle(Position min, Position max) {
		this.min = min;
		this.max = max;
	}
	@Override
	public boolean willCollide(Position position, Position velocity, double buffer) {
		double endX = position.getX()+velocity.getX();
		double endY = position.getY()+velocity.getY();
		return endX<=min.getX()||endX>=max.getX()||endY<=min.getY()||endY>=max.getY();
	}
	@Override
	public int getPriority() {
		return Integer.MAX_VALUE;
	}
}
