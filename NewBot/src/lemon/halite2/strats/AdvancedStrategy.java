package lemon.halite2.strats;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hlt.DebugLog;
import hlt.GameConstants;
import hlt.GameMap;
import hlt.Move;
import hlt.Planet;
import hlt.Vector;
import hlt.Ship;
import hlt.Ship.DockingStatus;
import hlt.ThrustMove;
import lemon.halite2.benchmark.Benchmark;
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
import lemon.halite2.task.BlameMap;
import lemon.halite2.task.DefendDockedShipTask;
import lemon.halite2.task.DockTask;
import lemon.halite2.task.Task;
import lemon.halite2.task.WanderTask;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.Circle;
import lemon.halite2.util.MathUtil;
import lemon.halite2.util.MoveQueue;

public class AdvancedStrategy implements Strategy {
	@Override
	public void init() {
		//Initialize Pathfinder
		Pathfinder.init();
	}
	public List<Integer> getClosestPlanets(Vector position){
		List<Integer> planets = new ArrayList<Integer>();
		final Map<Integer, Double> distances = new HashMap<Integer, Double>();
		for(Planet planet: GameMap.INSTANCE.getPlanets()) {
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
		//Define Obstacles
		Obstacles<ObstacleType> obstacles = new Obstacles<ObstacleType>();
		//Define processList
		List<Integer> undockedShips = new ArrayList<Integer>();
		//Add Map Border Obstacle
		obstacles.addObstacle(ObstacleType.PERMANENT, new MapBorderObstacle(new Vector(0, 0), new Vector(GameMap.INSTANCE.getWidth(), GameMap.INSTANCE.getHeight())));
		//Calculate Tasks
		List<Task> taskRequests = new ArrayList<Task>();
		//Add Wander Task
		taskRequests.add(new WanderTask());
		for(Planet planet: GameMap.INSTANCE.getPlanets()){
			if(planet.isOwned()){
				if(planet.getOwner()==GameMap.INSTANCE.getMyPlayerId()){
					int dockingSpotsLeft = planet.getDockingSpots()-planet.getDockedShips().size();
					if(dockingSpotsLeft>0){
						taskRequests.add(new DockTask(planet, dockingSpotsLeft));
					}
				}
			}else{
				taskRequests.add(new DockTask(planet, planet.getDockingSpots()));
			}
		}
		for(Ship ship: GameMap.INSTANCE.getShips()){
			if(ship.getOwner()==GameMap.INSTANCE.getMyPlayerId()){
				if(ship.getDockingStatus()!=DockingStatus.UNDOCKED){
					taskRequests.add(new DefendDockedShipTask());
					obstacles.addObstacle(ObstacleType.PERMANENT, new StaticObstacle(new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS)));
				}else {
					undockedShips.add(ship.getId());
				}
			}else{
				if(ship.getDockingStatus()==DockingStatus.UNDOCKED){
					taskRequests.add(new AttackEnemyTask(ship));
				}else{
					taskRequests.add(new AttackDockedEnemyTask(ship));
					obstacles.addObstacle(ObstacleType.PERMANENT, new StaticObstacle(new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS)));
				}
			}
		}
		//Add Planets to Obstacles
		for(Planet planet: GameMap.INSTANCE.getPlanets()) {
			obstacles.addObstacle(ObstacleType.PERMANENT, new StaticObstacle(new Circle(planet.getPosition(), planet.getRadius())));
		}
		//Add Enemy Ship Movements to Obstacles
		for(Ship ship: GameMap.INSTANCE.getShips()) {
			if(ship.getOwner()==GameMap.INSTANCE.getMyPlayerId()) {
				continue;
			}
			if(ship.getDockingStatus()==DockingStatus.UNDOCKED) {
				obstacles.addObstacle(ObstacleType.ENEMY, new EnemyShipObstacle(ship.getPosition()));
			}
		}
		//Add Uncertain Obstacles
		BiMap<Integer, Obstacle> uncertainObstacles = new BiMap<Integer, Obstacle>();
		Map<Integer, Pathfinder> pathfinders = new HashMap<Integer, Pathfinder>();
		for(int shipId: undockedShips) {
			Circle circle = new Circle(GameMap.INSTANCE.getMyPlayer().getShip(shipId).getPosition(), GameConstants.SHIP_RADIUS);
			Obstacle obstacle = new StaticObstacle(circle);
			uncertainObstacles.put(shipId, obstacle);
			obstacles.addObstacle(ObstacleType.UNCERTAIN, obstacle);
			Pathfinder pathfinder = new Pathfinder(circle.getPosition(), circle.getRadius(), obstacles,
					o->o.equals(uncertainObstacles.getValue(shipId))); //Weird use of Lambdas :)
			pathfinders.put(shipId, pathfinder);
		}
		Map<Integer, Task> taskMap = new HashMap<Integer, Task>();
		ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
		DebugLog.log("Assigning "+undockedShips.size()+" ships to "+taskRequests.size()+" tasks");
		while(!undockedShips.isEmpty()) {
			double bestScore = -Double.MAX_VALUE;
			Ship bestShip = null;
			Task bestTask = null;
			for(int shipId: undockedShips) {
				for(Task task: taskRequests) {
					Ship ship = GameMap.INSTANCE.getMyPlayer().getShip(shipId);
					double score = task.getScore(ship);
					if(score>bestScore) {
						bestScore = score;
						bestTask = task;
						bestShip = ship;
					}
				}
			}
			bestTask.accept(bestShip);
			taskMap.put(bestShip.getId(), bestTask);
			undockedShips.remove((Object)bestShip.getId());
			queue.add(bestShip.getId());
		}
		BlameMap blameMap = new BlameMap();
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
				Ship ship = GameMap.INSTANCE.getMyPlayer().getShip(biggestSet);
				DebugLog.log("Forcing Move: "+ship.getId());
				obstacles.addObstacle(ObstacleType.PERMANENT, new StaticObstacle(new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS)));
				obstacles.removeObstacle(ObstacleType.UNCERTAIN, uncertainObstacles.getValue(ship.getId()));
				for(int shipId: blameMap.get(ship.getId())) {
					queue.add(shipId);
				}
				blameMap.clear(ship.getId());
			}
			while(!queue.isEmpty()&&checkInterruption()) {
				Ship ship = GameMap.INSTANCE.getMyPlayer().getShip(queue.poll());
				Task task = taskMap.get(ship.getId());
				Move move = task.execute(ship, pathfinders.get(ship.getId()), blameMap, uncertainObstacles);
				if(move!=null) {
					moveQueue.add(move);
					if(blameMap.containsKey(ship.getId())) {
						for(int shipId: blameMap.get(ship.getId())) {
							queue.add(shipId);
						}
						blameMap.clear(ship.getId());
					}
					if(move instanceof ThrustMove) {
						ThrustMove thrustMove = (ThrustMove)move;
						obstacles.addObstacle(ObstacleType.PERMANENT, new DynamicObstacle(
								new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS), thrustMove.getThrustPlan()));
					}else {
						obstacles.addObstacle(ObstacleType.PERMANENT, new StaticObstacle(
								new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS)));
					}
					obstacles.removeObstacle(ObstacleType.UNCERTAIN, uncertainObstacles.getValue(ship.getId()));
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
		for(Ship ship: GameMap.INSTANCE.getShips()) {
			if(ship.getOwner()==GameMap.INSTANCE.getMyPlayerId()) {
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
		for(Planet planet: GameMap.INSTANCE.getPlanets()) {
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
	public Ship findEnemyShip(Planet planet, Vector position) {
		double bufferSquared = (planet.getRadius()+GameConstants.DOCK_RADIUS+GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS);
		bufferSquared = bufferSquared*bufferSquared;
		
		double startDirection = planet.getPosition().getDirectionTowards(position);
		double shortestDirection = Double.MAX_VALUE;
		Ship shortestShip = null;
		
		for(Ship ship: GameMap.INSTANCE.getShips()) {
			if(ship.getOwner()==GameMap.INSTANCE.getMyPlayerId()) {
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
		
		for(Ship ship: GameMap.INSTANCE.getShips()) {
			if(ship.getOwner()==GameMap.INSTANCE.getMyPlayerId()||ship.getDockingStatus()==DockingStatus.UNDOCKED) {
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
