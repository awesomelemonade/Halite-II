import hlt.DebugLog;
import hlt.GameMap;
import hlt.Position;
import hlt.Ship;
import hlt.ThrustMove;

public class Pathfinder {
	private static GameMap gameMap;

	public static void setGameMap(GameMap gameMap) {
		Pathfinder.gameMap = gameMap;
	}
	public static ThrustMove pathfind(Position start, Position end, double startBuffer, double endBuffer) {
		return pathfind(start, end.addPolar(endBuffer, end.getDirectionTowards(start)), startBuffer);
	}
	public static ThrustMove pathfind(Position start, Position end, double buffer) {
		return null;
	}
	public static double segmentLineDistance(Position start, Position end, Position point) {
		
	}
	//Segment - Circle
	public static double segmentCircleIntersect(Position start, Position end, Position center, double radius) {
		// Parameterize the segment as start + t * (end-start),
		// and substitute into the equation of a circle
		// Solve for t
		double dx = end.getX() - start.getX();
		double dy = end.getY() - start.getY();

		double a = dx * dx + dy * dy; // Distance squared between start and end

		double b = -2 * (start.getX() * start.getX() - start.getX() * end.getX() - start.getX() * center.getX()
				+ end.getX() * center.getX() + start.getY() * start.getY() - start.getY() * end.getY()
				- start.getY() * center.getY() + end.getY() * center.getY());
		
		// y=ax^2+bx+c
		
		return -b / (2*a); // Time along segment when closest to the circle (vertex of the quadratic
	}
	//Arc - Circle
}
