package lemon.halite2.pathfinding;

import hlt.Vector;

public interface Obstacle {
	public boolean willCollide(Vector position, Vector velocity, double buffer);
	public int getPriority();
}
