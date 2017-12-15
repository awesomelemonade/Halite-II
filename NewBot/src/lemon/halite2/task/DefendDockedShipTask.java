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
import lemon.halite2.util.BiMap;
import lemon.halite2.util.MathUtil;

public class DefendDockedShipTask implements Task {
	private Ship ship;
	private int counter;
	private Vector targetPosition;
	private double priority;
	public DefendDockedShipTask(Ship ship) {
		this.ship = ship;
		Projection projection = new Projection(ship.getPosition());
		projection.calculate(s->true);
		if(projection.getClosestEnemyDistanceSquared()-256.0<projection.getClosestFriendlyDistanceSquared()) { //estimation
			counter = 1;
			targetPosition = ship.getPosition().addPolar(1.15,
					ship.getPosition().getDirectionTowards(projection.getEnemySource()));
			priority = projection.getClosestEnemyDistanceSquared();
		}
	}
	@Override
	public void accept(Ship ship) {
		counter--;
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		double direction = ship.getPosition().getDirectionTowards(targetPosition);
		double directionDegrees = Math.toDegrees(direction);
		int roundedDegrees = RoundPolicy.ROUND.applyDegrees(direction);
		int preferredSign = directionDegrees-((int)directionDegrees)<0.5?1:-1;
		//try greediest
		if(ship.getPosition().getDistanceSquared(this.ship.getPosition())<=(2*GameConstants.SHIP_RADIUS+GameConstants.MAX_SPEED)*
				(2*GameConstants.SHIP_RADIUS+GameConstants.MAX_SPEED)){
			int bestCandidateThrust = 0;
			int bestCandidateAngle = 0;
			double bestCandidateDistanceSquared = ship.getPosition().getDistanceSquared(this.ship.getPosition());
			for(int i=1;i<=7;++i) {
				for(int j=0;j<MathUtil.TAU_DEGREES;++j) {
					if(pathfinder.getCandidate(i, j, ObstacleType.PERMANENT)==null) {
						double distanceSquared = ship.getPosition().add(Pathfinder.velocityVector[i-1][j]).getDistanceSquared(this.ship.getPosition());
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
	@Override
	public double getScore(Ship ship) {
		if(counter>0) {
			return -ship.getPosition().getDistanceSquared(targetPosition)*0.9-priority*0.3;
		}else {
			return -Double.MAX_VALUE;
		}
	}
}
