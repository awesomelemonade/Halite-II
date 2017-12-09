package lemon.halite2.task;

import java.util.HashSet;
import java.util.Set;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Move;
import hlt.RoundPolicy;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustPlan;
import hlt.Vector;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.Geometry;
import lemon.halite2.util.MathUtil;

public class AttackEnemyTask implements Task {
	private static final double DETECT_RADIUS_SQUARED = (GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS+GameConstants.MAX_SPEED*1.4)*
			(GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS+GameConstants.MAX_SPEED*1.4);
	private static final double MAX_RATIO = 2.0;
	private Ship enemyShip;
	private boolean activate;
	private int counter;
	private Set<Integer> allowedShips;
	private int enemyCount;
	public AttackEnemyTask(Ship enemyShip) {
		this.enemyShip = enemyShip;
		this.allowedShips = new HashSet<Integer>();
		for(Ship ship: GameMap.INSTANCE.getShips()) {
			if(ship.getPosition().getDistanceSquared(enemyShip.getPosition())<DETECT_RADIUS_SQUARED) {
				if(ship.getOwner()==GameMap.INSTANCE.getMyPlayerId()) {
					allowedShips.add(ship.getId());
				}else {
					enemyCount++;
				}
			}
		}
		this.activate = allowedShips.size()>enemyCount;
		this.counter = 0;
	}
	@Override
	public void accept(Ship ship) {
		counter++;
	}
	@Override
	public Move execute(Ship ship, Pathfinder pathfinder, BlameMap blameMap,
			BiMap<Integer, Obstacle> uncertainObstacles) {
		double direction = ship.getPosition().getDirectionTowards(enemyShip.getPosition());
		double directionDegrees = Math.toDegrees(direction);
		int roundedDegrees = RoundPolicy.ROUND.applyDegrees(direction);
		int preferredSign = directionDegrees-((int)directionDegrees)<0.5?1:-1;
		//try candidates
		for(int i=7;i>0;--i) {
			for(int j=0;j<=MathUtil.PI_DEGREES;++j) {
				int candidateA = MathUtil.normalizeDegrees(roundedDegrees+j*preferredSign);
				int candidateB = MathUtil.normalizeDegrees(roundedDegrees-j*preferredSign);
				if(!willCollide(ship.getPosition(), i, candidateA)) {
					if(pathfinder.getCandidate(i, candidateA, ObstacleType.PERMANENT)==null) {
						Obstacle obstacle = pathfinder.getCandidate(i, candidateA, ObstacleType.UNCERTAIN);
						if(obstacle==null) {
							return new ThrustMove(ship.getId(), new ThrustPlan(i, candidateA));
						}else {
							blameMap.add(uncertainObstacles.getKey(obstacle), ship.getId());
							return null;
						}
					}
				}
				if(!willCollide(ship.getPosition(), i, candidateB)) {
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
		//Only activate if density of ship of friendly is greater than enemy
		if(activate) {
			if(allowedShips.contains(ship.getId())) {
				if(((double)(counter+1))/((double)enemyCount)<=MAX_RATIO) {
					return -ship.getPosition().getDistanceSquared(enemyShip.getPosition())*0.9;
				}
			}
		}
		return -Double.MAX_VALUE;
	}
	public boolean willCollide(Vector position, int thrust, int angle) {
		return Geometry.segmentCircleIntersection(position, position.add(Pathfinder.velocityVector[thrust-1][angle]), enemyShip.getPosition(), 2*GameConstants.SHIP_RADIUS);
	}
}
