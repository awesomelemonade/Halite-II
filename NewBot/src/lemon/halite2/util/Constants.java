package lemon.halite2.util;

public class Constants {
	// Accounts for the fact that you cannot have fractional degrees
	// You should leave BUFFER_CONSTANT error margin to mitigate risk in crashing into planets
	// BUFFER_CONSTANT = 7 * tan(0.5 degrees)
	// Used in Pathfinding
	public static final float BUFFER_CONSTANT = 0.06109f;
}
