package lemon.halite2.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Move;
import hlt.RoundPolicy;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.ThrustPlan;
import hlt.Vector;
import hlt.Ship.DockingStatus;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.Geometry;
import lemon.halite2.util.MathUtil;

public class AttackEnemyTask implements Task {
	private static final double IGNORE_RADIUS_SQUARED = 50*GameConstants.MAX_SPEED*GameConstants.MAX_SPEED;
	private static final double DETECT_RADIUS_SQUARED = (GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS+GameConstants.MAX_SPEED)*
			(GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS+GameConstants.MAX_SPEED);
	private static final double MAX_RATIO = 2.0;
	private Ship enemyShip;
	private int counter;
	private int enemyCount;
	public AttackEnemyTask(Ship enemyShip) {
		this.enemyShip = enemyShip;
		for(Ship ship: GameMap.INSTANCE.getShips()) {
			if(ship.getDockingStatus()!=DockingStatus.UNDOCKED) {
				continue;
			}
			if(ship.getPosition().getDistanceSquared(enemyShip.getPosition())<DETECT_RADIUS_SQUARED) {
				if(ship.getOwner()!=GameMap.INSTANCE.getMyPlayerId()) {
					enemyCount++;
				}
			}
		}
		this.counter = 0;
	}
	private static Map<Integer, List<Integer>> friendlies;
	static {
		friendlies = new HashMap<Integer, List<Integer>>();
	}
	public static void newTurn() {
		for(Ship ship: GameMap.INSTANCE.getMyPlayer().getShips()) {
			List<Integer> ships = new ArrayList<Integer>();
			for(Ship s: GameMap.INSTANCE.getMyPlayer().getShips()) {
				if(s.getDockingStatus()!=DockingStatus.UNDOCKED) {
					continue;
				}
				if(s.getPosition().getDistanceSquared(ship.getPosition())<DETECT_RADIUS_SQUARED) {
					ships.add(s.getId());
				}
			}
			friendlies.put(ship.getId(), ships);
		}
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
		if(((double)(counter+1))/((double)enemyCount)>MAX_RATIO) {
			return -Double.MAX_VALUE;
		}
		int friendlyCount = 0;
		for(int shipId: friendlies.get(ship.getId())) {
			Task task = TaskManager.INSTANCE.getTask(shipId);
			if(task==null||task.getClass().equals(AttackEnemyTask.class)) {
				friendlyCount++;
			}
		}
		if(friendlyCount>enemyCount) {
			return -Math.max(ship.getPosition().getDistanceSquared(enemyShip.getPosition()), 0);
		}else {
			return -Double.MAX_VALUE;
		}
	}
	public boolean willCollide(Vector position, int thrust, int angle) {
		return Geometry.segmentCircleIntersection(position, position.add(Pathfinder.velocityVector[thrust-1][angle]), enemyShip.getPosition(), 2*GameConstants.SHIP_RADIUS);
	}
}
