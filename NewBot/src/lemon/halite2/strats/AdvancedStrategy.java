package lemon.halite2.strats;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import hlt.DebugLog;
import hlt.DockMove;
import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Vector;
import hlt.Ship;
import hlt.ThrustPlan;
import hlt.Ship.DockingStatus;
import lemon.halite2.benchmark.Benchmark;
import lemon.halite2.micro.Group;
import lemon.halite2.micro.MicroGame;
import lemon.halite2.pathfinding.DynamicObstacle;
import lemon.halite2.pathfinding.EnemyShipObstacle;
import lemon.halite2.pathfinding.MapBorderObstacle;
import lemon.halite2.pathfinding.Obstacle;
import lemon.halite2.pathfinding.ObstacleType;
import lemon.halite2.pathfinding.Obstacles;
import lemon.halite2.pathfinding.Pathfinder;
import lemon.halite2.pathfinding.StaticObstacle;
import lemon.halite2.task.AttackDockedEnemyTask;
import lemon.halite2.task.AttackEnemyTask;
import lemon.halite2.task.DefendDockedShipTask;
import lemon.halite2.task.DockTask;
import lemon.halite2.task.Task;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.Circle;
import lemon.halite2.util.MathUtil;
import lemon.halite2.util.MoveQueue;

public class AdvancedStrategy implements Strategy {
	private GameMap gameMap;
	public AdvancedStrategy(GameMap gameMap) {
		this.gameMap = gameMap;
	}
	@Override
	public void init() {
		//Initialize Pathfinder
		Pathfinder.init();
	}
	public List<Integer> getClosestPlanets(Vector position){
		List<Integer> planets = new ArrayList<Integer>();
		final Map<Integer, Double> distances = new HashMap<Integer, Double>();
		for(Planet planet: gameMap.getPlanets()) {
			planets.add(planet.getId());
			distances.put(planet.getId(), position.getDistanceTo(planet.getPosition())-planet.getRadius());
		}
		Collections.sort(planets, new Comparator<Integer>() {
			@Override
			public int compare(Integer a, Integer b) {
				return Double.compare(distances.get(a), distances.get(b));
			}
		});
		return planets;
	}
	@Override
	public void newTurn(MoveQueue moveQueue) {
		//Calculate Tasks
		Map<Integer, Task> taskMap = new HashMap<Integer, Task>();
		List<Task> taskRequests = new ArrayList<Task>();
		for(Planet planet: gameMap.getPlanets()){
			if(planet.isOwned()){
				if(planet.getOwner()==gameMap.getMyPlayerId()){
					int dockingSpotsLeft = planet.getDockingSpots()-planet.getDockedShips().size();
					if(dockingSpotsLeft>0){
						taskRequests.add(new DockTask(planet));
					}
				}
			}else{
				taskRequests.add(new DockTask(planet));
			}
		}
		for(Ship ship: gameMap.getShips()){
			if(ship.getOwner()==gameMap.getMyPlayerId()){
				if(ship.getDockingStatus()!=DockingStatus.UNDOCKED){
					taskRequests.add(new DefendDockedShipTask(ship));
				}
			}else{
				if(ship.getDockingStatus()==DockingStatus.UNDOCKED){
					taskRequests.add(new AttackEnemyTask(ship));
				}else{
					taskRequests.add(new AttackDockedEnemyTask(ship));
				}
			}
		}
		for(Ship ship: gameMap.getMyPlayer().getShips()){
			Task bestTask = null;
			double bestScore = Double.MIN_VALUE;
			for(Task task: taskRequests){
				double score = task.getScore(ship);
				if(bestScore<=score){
					bestScore = score;
					bestTask = task;
				}
			}
			bestTask.accept(ship);
			taskMap.put(ship.getId(), bestTask);
		}
		//Define Obstacles
		Obstacles<ObstacleType> obstacles = new Obstacles<ObstacleType>();
		//Define queue
		Deque<Integer> queue = new ArrayDeque<Integer>();
		//Add Map Border Obstacle
		obstacles.addObstacle(ObstacleType.PERMANENT, new MapBorderObstacle(new Vector(0, 0), new Vector(gameMap.getWidth(), gameMap.getHeight())));
		//Add Planets to Obstacles
		for(Planet planet: gameMap.getPlanets()) {
			obstacles.addObstacle(ObstacleType.PERMANENT, new StaticObstacle(new Circle(planet.getPosition(), planet.getRadius())));
		}
		//Add Docked Ships to Obstacles
		for(Ship ship: gameMap.getMyPlayer().getShips()) {
			if(ship.getDockingStatus()!=DockingStatus.UNDOCKED) {
				obstacles.addObstacle(ObstacleType.PERMANENT, new StaticObstacle(new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS)));
			}else {
				queue.add(ship.getId());
			}
		}
		//Add Enemy Ship Movements to Obstacles
		for(Ship ship: gameMap.getMyPlayer().getShips()) {
			if(ship.getOwner()==gameMap.getMyPlayerId()) {
				continue;
			}
			Planet closestPlanet = getClosestOwnedPlanet(ship.getPosition());
			if(closestPlanet==null) {
				continue;
			}else {
				if(closestPlanet.getOwner()!=gameMap.getMyPlayerId()) {
					obstacles.addObstacle(ObstacleType.ENEMY, new EnemyShipObstacle(ship.getPosition()));
				}
			}
		}
		//Add Uncertain Obstacles
		BiMap<Integer, Obstacle> uncertainObstacles = new BiMap<Integer, Obstacle>();
		Map<Integer, Pathfinder> pathfinders = new HashMap<Integer, Pathfinder>();
		for(int shipId: queue) {
			Circle circle = new Circle(gameMap.getMyPlayer().getShip(shipId).getPosition(), GameConstants.SHIP_RADIUS);
			Obstacle obstacle = new StaticObstacle(circle);
			uncertainObstacles.put(shipId, obstacle);
			obstacles.addObstacle(ObstacleType.UNCERTAIN, obstacle);
			Pathfinder pathfinder = new Pathfinder(circle.getPosition(), circle.getRadius(), obstacles,
					o->o.equals(uncertainObstacles.getValue(shipId))); //Weird use of Lambdas :)
			pathfinders.put(shipId, pathfinder);
		}
		Map<Integer, Set<Integer>> blameMap = new HashMap<Integer, Set<Integer>>();
		do {
			int biggestSet = 0;
			int biggestSize = 0;
			for(int id: blameMap.keySet()) {
				if(blameMap.get(id).size()>biggestSize) {
					biggestSet = id;
					biggestSize = blameMap.get(id).size();
				}
			}
			if(biggestSize>0) {
				Group group = Group.getGroup(biggestSet);
				Pathfinder pathfinder = pathfinders.get(group.getId());
				pathfinder.clearObstacles(UNCERTAIN_SHIP_PRIORITY);
				
			}
			while(!groupQueue.isEmpty()&&checkInterruption()) {
				Group group = Group.getGroup(groupQueue.poll());
				Pathfinder pathfinder = pathfinders.get(group.getId());
				pathfinder.clearObstacles(UNCERTAIN_SHIP_PRIORITY);
				Planet targetPlanet = gameMap.getPlanet(targetPlanets.get(group.getId()));
				Ship enemyShip = null;
				if(targetPlanet.getOwner()==gameMap.getMyPlayerId()) {
					enemyShip = findEnemyShip(targetPlanet, group.getCircle().getPosition());
				}else {
					enemyShip = findEnemyDockedShip(targetPlanet, group.getCircle().getPosition());
				}
				ThrustPlan plan;
				if(enemyShip==null) {
					plan = pathfinder.getGreedyPlan(targetPlanet.getPosition(), targetPlanet.getRadius(), targetPlanet.getRadius()+GameConstants.DOCK_RADIUS);
				}else {
					plan = pathfinder.getGreedyPlan(enemyShip.getPosition(), GameConstants.SHIP_RADIUS, GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS);
				}
				if(plan==null) {
					DebugLog.log(String.format("\tCan't Move: %d", group.getId()));
				}else {
					Obstacle candidate = pathfinder.getCandidate(plan);
					DebugLog.log("Evaluating Candidate for "+group.getId()+": "+candidate.getPriority()+" - "+uncertainObstacles.getKey(candidate));
					if(candidate==null||candidate==Pathfinder.NO_CONFLICT) {
						group.move(gameMap, moveQueue, plan);
						obstacles.addObstacle(ObstacleType.PERMANENT, new DynamicObstacle(group.getCircle(), plan));
						obstacles.removeObstacle(ObstacleType.UNCERTAIN, uncertainObstacles.getValue(group.getId()));
						if(blameMap.containsKey(group.getId())) {
							for(int groupId: blameMap.get(group.getId())) {
								groupQueue.push(groupId);
							}
							blameMap.get(group.getId()).clear();
						}
					}else if(candidate.getPriority()==UNCERTAIN_SHIP_PRIORITY) {
						int groupId = uncertainObstacles.getKey(candidate);
						if(!blameMap.containsKey(groupId)) {
							blameMap.put(groupId, new HashSet<Integer>());
						}
						blameMap.get(groupId).add(group.getId());
					}else if(candidate.getPriority()==ENEMY_SHIP_PRIORITY){
						ThrustPlan newPlan;
						if(enemyShip==null) {
							newPlan = pathfinder.getGreedyPlan(targetPlanet.getPosition(), targetPlanet.getRadius(), targetPlanet.getRadius()+GameConstants.DOCK_RADIUS, ENEMY_SHIP_PRIORITY);
						}else {
							newPlan = pathfinder.getGreedyPlan(enemyShip.getPosition(), GameConstants.SHIP_RADIUS, GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS, ENEMY_SHIP_PRIORITY);
						}
						if(newPlan==null) {
							group.move(gameMap, moveQueue, plan);
							obstacles.addObstacle(new DynamicObstacle(group.getCircle(), plan));
						}else {
							group.move(gameMap, moveQueue, newPlan);
							obstacles.addObstacle(new DynamicObstacle(group.getCircle(), newPlan));
						}
						obstacles.removeObstacle(uncertainObstacles.getValue(group.getId()));
						if(blameMap.containsKey(group.getId())) {
							for(int groupId: blameMap.get(group.getId())) {
								groupQueue.push(groupId);
							}
							blameMap.remove(group.getId());
						}
					}else {
						DebugLog.log(String.format("\tConflict? %d %d", group.getId()));
					}
				}
			}
		}while((!blameMap.isEmpty())&&checkInterruption());
		if(Thread.currentThread().isInterrupted()){
			DebugLog.log("Exiting Function: "+(benchmark.peek()/1000000.0));
		}
	}
	public static Benchmark benchmark;
	public boolean checkInterruption() {
		if(Thread.currentThread().isInterrupted()){
			DebugLog.log("Received Interruption: "+(benchmark.peek()/1000000.0));
		}
		return !Thread.currentThread().isInterrupted();
	}
	public int countEnemyShips(Vector position, double buffer) {
		buffer = buffer*buffer; //compares against distanceSquared
		int count = 0;
		for(Ship ship: gameMap.getShips()) {
			if(ship.getOwner()==gameMap.getMyPlayerId()) {
				continue;
			}
			double distanceSquared = ship.getPosition().getDistanceSquared(position);
			if(distanceSquared<buffer) {
				count++;
			}
		}
		return count;
	}
	public Planet getClosestOwnedPlanet(Vector position) {
		Planet closestPlanet = null;
		double closestDistance = Double.MAX_VALUE;
		for(Planet planet: gameMap.getPlanets()) {
			if(!planet.isOwned()) {
				continue;
			}
			double distance = position.getDistanceTo(planet.getPosition())-planet.getRadius();
			if(closestDistance>distance) {
				closestDistance = distance;
				closestPlanet = planet;
			}
		}
		return closestPlanet;
	}
	public boolean isSafeToDock(Planet planet) {
		for(Ship ship: gameMap.getShips()) {
			if(ship.getOwner()==gameMap.getMyPlayerId()) {
				continue;
			}
			//Check if there are any enemy ships nearby planet
			if(ship.getPosition().getDistanceSquared(planet.getPosition())<(planet.getRadius()+GameConstants.DOCK_RADIUS*2)*(planet.getRadius()+GameConstants.DOCK_RADIUS*2)) {
				return false;
			}
		}
		return true;
	}
	public Ship findEnemyShip(Planet planet, Vector position) {
		double bufferSquared = (planet.getRadius()+GameConstants.DOCK_RADIUS+GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS);
		bufferSquared = bufferSquared*bufferSquared;
		
		double startDirection = planet.getPosition().getDirectionTowards(position);
		double shortestDirection = Double.MAX_VALUE;
		Ship shortestShip = null;
		
		for(Ship ship: gameMap.getShips()) {
			if(ship.getOwner()==gameMap.getMyPlayerId()) {
				continue;
			}
			if(planet.getPosition().getDistanceSquared(ship.getPosition())<bufferSquared) {
				double targetDirection = planet.getPosition().getDirectionTowards(ship.getPosition());
				double deltaDirection = MathUtil.angleBetweenRadians(startDirection, targetDirection);
				if(shortestDirection>deltaDirection) {
					shortestDirection = deltaDirection;
					shortestShip = ship;
				}
			}
		}
		return shortestShip;
	}
	public Ship findEnemyDockedShip(Planet planet, Vector position) {
		double bufferSquared = (planet.getRadius()+GameConstants.DOCK_RADIUS+GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS);
		bufferSquared = bufferSquared*bufferSquared;
		
		double startDirection = planet.getPosition().getDirectionTowards(position);
		double shortestDirection = Double.MAX_VALUE;
		Ship shortestShip = null;
		
		for(Ship ship: gameMap.getShips()) {
			if(ship.getOwner()==gameMap.getMyPlayerId()||ship.getDockingStatus()==DockingStatus.UNDOCKED) {
				continue;
			}
			if(planet.getPosition().getDistanceSquared(ship.getPosition())<bufferSquared) {
				double targetDirection = planet.getPosition().getDirectionTowards(ship.getPosition());
				double deltaDirection = MathUtil.angleBetweenRadians(startDirection, targetDirection);
				if(shortestDirection>deltaDirection) {
					shortestDirection = deltaDirection;
					shortestShip = ship;
				}
			}
		}
		return shortestShip;
	}
}
