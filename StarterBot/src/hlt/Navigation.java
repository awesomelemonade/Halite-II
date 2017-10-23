package hlt;

public class Navigation {
	public static ThrustMove navigateShipToDock(GameMap gameMap, Ship ship, Entity dockTarget, int maxThrust) {
		int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
		boolean avoidObstacles = true;
		double angularStepRad = Math.PI / 180.0;
		Position targetPos = ship.getPosition().getClosestPoint(dockTarget);
		return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust, avoidObstacles, maxCorrections,
				angularStepRad);
	}
	public static ThrustMove navigateShipTowardsTarget(GameMap gameMap, Ship ship, Position targetPos, int maxThrust,
			boolean avoidObstacles, int maxCorrections, double angularStepRad) {
		if (maxCorrections <= 0) {
			return null;
		}
		double distance = ship.getPosition().getDistanceTo(targetPos);
		double angleRad = ship.getPosition().orientTowardsInRad(targetPos);
		if (avoidObstacles && !gameMap.objectsBetween(ship.getPosition(), targetPos).isEmpty()) {
			double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
			double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
			Position newTarget = new Position(ship.getPosition().getXPos() + newTargetDx,
					ship.getPosition().getYPos() + newTargetDy);
			return navigateShipTowardsTarget(gameMap, ship, newTarget, maxThrust, true, (maxCorrections - 1),
					angularStepRad);
		}
		int thrust;
		if (distance < maxThrust) {
			// Do not round up, since overshooting might cause collision.
			thrust = (int) distance;
		} else {
			thrust = maxThrust;
		}
		int angleDeg = Util.angleRadToDegClipped(angleRad);
		return new ThrustMove(ship, angleDeg, thrust);
	}
}
