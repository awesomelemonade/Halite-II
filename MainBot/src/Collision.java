

import hlt.Entity;
import hlt.Position;

public class Collision {
	/**
	 * Test whether a given line segment intersects a circular area.
	 *
	 * @param start
	 *            The start of the segment.
	 * @param end
	 *            The end of the segment.
	 * @param circle:
	 *            The circle to test against.
	 * @param fudge
	 *            An additional safety zone to leave when looking for collisions.
	 *            (Probably set it to ship radius 0.5)
	 * @return true if the segment intersects, false otherwise
	 */
	public static boolean segmentCircleIntersect(Position start, Position end, Entity circle, double fudge) {
		// Parameterize the segment as start + t * (end - start),
		// and substitute into the equation of a circle
		// Solve for t
		double circleRadius = circle.getRadius();
		double startX = start.getXPos();
		double startY = start.getYPos();
		double endX = end.getXPos();
		double endY = end.getYPos();
		double centerX = circle.getPosition().getXPos();
		double centerY = circle.getPosition().getYPos();
		double dx = endX - startX;
		double dy = endY - startY;

		double a = dx*dx + dy*dy;

		double b = -2 * (startX*startX - (startX * endX) - (startX * centerX) + (endX * centerX) + startY*startY
				- (startY * endY) - (startY * centerY) + (endY * centerY));

		if (a == 0.0) {
			// Start and end are the same point
			return start.getDistanceTo(circle.getPosition()) <= circleRadius + fudge;
		}

		// Time along segment when closest to the circle (vertex of the quadratic)
		double t = Math.min(-b / (2 * a), 1.0);
		if (t < 0) {
			return false;
		}

		double closestX = startX + dx * t;
		double closestY = startY + dy * t;
		double closestDistance = new Position(closestX, closestY).getDistanceTo(circle.getPosition());

		return closestDistance <= circleRadius + fudge;
	}
}
