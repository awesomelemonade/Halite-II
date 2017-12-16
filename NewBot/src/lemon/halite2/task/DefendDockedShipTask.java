package lemon.halite2.task;

import hlt.GameConstants;
import hlt.Move;
import hlt.RoundPolicy;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustPlan;
import hlt.Vector;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.task.projection.Projection;
import lemon.halite2.task.projection.ProjectionManager;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.MathUtil;

public class DefendDockedShipTask implements Task {
	private Ship ship;
	private Projection projection;
	private Vector intersection;
	private boolean accepted;
	public DefendDockedShipTask(Ship ship) {
		this.ship = ship;
		this.intersection = null;
		this.accepted = false;
	}
	@Override
	public void accept(Ship ship) {
		accepted = true;
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		double direction = ship.getPosition().getDirectionTowards(intersection);
		double directionDegrees = Math.toDegrees(direction);
		int roundedDegrees = RoundPolicy.ROUND.applyDegrees(direction);
		int preferredSign = directionDegrees-((int)directionDegrees)<0.5?1:-1;
		//try greediest
		if(ship.getPosition().getDistanceSquared(intersection)<=(2*GameConstants.SHIP_RADIUS+GameConstants.MAX_SPEED)*
				(2*GameConstants.SHIP_RADIUS+GameConstants.MAX_SPEED)){
			int bestCandidateThrust = 0;
			int bestCandidateAngle = 0;
			double bestCandidateDistanceSquared = ship.getPosition().getDistanceSquared(intersection);
			for(int i=1;i<=7;++i) {
				for(int j=0;j<MathUtil.TAU_DEGREES;++j) {
					if(pathfinder.getCandidate(i, j, ObstacleType.PERMANENT)==null) {
						double distanceSquared = ship.getPosition().add(Pathfinder.velocityVector[i-1][j]).getDistanceSquared(intersection);
						if(distanceSquared<bestCandidateDistanceSquared) {
							bestCandidateDistanceSquared = distanceSquared;
							bestCandidateThrust = i;
							bestCandidateAngle = j;
						}
					}
				}
			}
			Obstacle obstacle = pathfinder.getCandidate(bestCandidateThrust, bestCandidateAngle, ObstacleType.UNCERTAIN);
			if(obstacle==null) {
				return new ThrustMove(ship.getId(), new ThrustPlan(bestCandidateThrust, bestCandidateAngle));
			}else {
				blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
				return null;
			}
		}
		//try candidates
		for(int i=7;i>0;--i) {
			for(int j=0;j<=MathUtil.PI_DEGREES;++j) {
				int candidateA = MathUtil.normalizeDegrees(roundedDegrees+j*preferredSign);
				int candidateB = MathUtil.normalizeDegrees(roundedDegrees-j*preferredSign);
				if(pathfinder.getCandidate(i, candidateA, ObstacleType.PERMANENT)==null) {
					Obstacle obstacle = pathfinder.getCandidate(i, candidateA, ObstacleType.UNCERTAIN);
					if(obstacle==null) {
						return new ThrustMove(ship.getId(), new ThrustPlan(i, candidateA));
					}else {
						blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
						return null;
					}
				}
				if(pathfinder.getCandidate(i, candidateB, ObstacleType.PERMANENT)==null) {
					Obstacle obstacle = pathfinder.getCandidate(i, candidateB, ObstacleType.UNCERTAIN);
					if(obstacle==null) {
						return new ThrustMove(ship.getId(), new ThrustPlan(i, candidateB));
					}else {
						blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
						return null;
					}
				}
			}
		}
		Obstacle obstacle = pathfinder.getStillCandidate(ObstacleType.UNCERTAIN);
		if(obstacle==null) {
			return new ThrustMove(ship.getId(), new ThrustPlan(0, 0));
		}else {
			blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
			return null;
		}
	}
	public void updateProjection() {
		projection = ProjectionManager.INSTANCE.calculate(this.ship.getPosition(), s->false); 
	}
	@Override
	public double getScore(Ship ship) {
		if(accepted) {
			return -Double.MAX_VALUE;
		}
		if(projection.getFriendlySourceShipId()!=ship.getId()) {
			return -Double.MAX_VALUE;
		}
		if(projection.getClosestEnemyDistanceSquared()-256.0<
				projection.getClosestFriendlyDistanceSquared()) { //estimation
			if(projection.getClosestFriendlyDistanceSquared()<projection.getClosestEnemyDistanceSquared()) {
				double angle = MathUtil.angleBetweenRadians(projection.getEnemySource().getDirectionTowards(projection.getFriendlySource()),
						projection.getEnemySource().getDirectionTowards(projection.getTarget()));
				double magnitude = Math.sin(angle)/(Math.sin(Math.PI-2*angle)/
						projection.getFriendlySource().getDistanceTo(projection.getEnemySource())); //Law of Sines
				this.intersection = projection.getFriendlySource().addPolar(magnitude,
						projection.getFriendlySource().getDirectionTowards(projection.getEnemySource())+angle);
				return 1.0/(magnitude*magnitude);
			}else {
				this.intersection = projection.getTarget();
				return 1.0/intersection.getDistanceSquared(ship.getPosition());
			}
		}else {
			return -Double.MAX_VALUE;
		}
	}
}
