package lemon.halite2.pathfinding;

import hlt.Position;

public interface Obstacle {
	public boolean willCollide(Position position, Position velocity, double buffer);
	public int getPriority();
}
