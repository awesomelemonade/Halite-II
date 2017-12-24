package lemon.halite2.strats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

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
import lemon.halite2.task.TaskManager;
import lemon.halite2.task.WanderTask;
import lemon.halite2.task.experimental.AbandonTask;
import lemon.halite2.task.experimental.FindEnemyTask;
import lemon.halite2.task.experimental.LureEnemyTask;
import lemon.halite2.task.experimental.RushTask;
import lemon.halite2.task.projection.FindProjectedDockedEnemyTask;
import lemon.halite2.task.projection.ProjectionManager;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.Circle;
import lemon.halite2.util.MathUtil;
import lemon.halite2.util.MoveQueue;

public class AdvancedStrategy implements Strategy {
	private Benchmark benchmark;
	private Map<Class<? extends Task>, Integer> classId;
	private Map<Class<? extends Task>, Long> taskTime;
	@Override
	public void init() {
		//Initialize Benchmark
		benchmark = new Benchmark();
		//Initialize taskTime
		taskTime = new HashMap<Class<? extends Task>, Long>();
		//Initialize Pathfinder
		Pathfinder.init();
		//Initialize TaskManager
		TaskManager.INSTANCE.init();
		//Initialize ProjectionManager
		ProjectionManager.INSTANCE.init();
		//Initialize classId for Chlorine Viewer
		classId = new HashMap<Class<? extends Task>, Integer>();
		classId.put(DockTask.class, 71);
		classId.put(AttackDockedEnemyTask.class, 72);
		classId.put(AttackEnemyTask.class, 73);
		classId.put(DefendDockedShipTask.class, 74);
		classId.put(WanderTask.class, 75);
		//Experimental Tasks
		classId.put(AbandonTask.class, 91);
		classId.put(FindEnemyTask.class, 92);
		classId.put(LureEnemyTask.class, 93);
		classId.put(RushTask.class, 94);
		//Projection
		classId.put(FindProjectedDockedEnemyTask.class, 113);
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
		benchmark.push();
		//Clear taskTime
		taskTime.clear();
		//Update Managers
		ProjectionManager.INSTANCE.update();
		TaskManager.INSTANCE.update();
		//Preprocess Tasks
		benchmark.push();
		AttackEnemyTask.newTurn();
		DebugLog.log("AttackEnemyTask Preprocess: "+Benchmark.format(benchmark.pop())+"s");
		//Define Obstacles
		Obstacles<ObstacleType> obstacles = new Obstacles<ObstacleType>();
		//Define ships to be processed
		List<Integer> undockedShips = new ArrayList<Integer>();
		//Add Map Border Obstacle
		obstacles.addObstacle(ObstacleType.PERMANENT, new MapBorderObstacle(new Vector(0, 0), new Vector(GameMap.INSTANCE.getWidth(), GameMap.INSTANCE.getHeight())));
		//Add Ship Obstacles
		for(Ship ship: GameMap.INSTANCE.getShips()){
			if(ship.getOwner()==GameMap.INSTANCE.getMyPlayerId()){
				if(ship.getDockingStatus()!=DockingStatus.UNDOCKED){
					obstacles.addObstacle(ObstacleType.PERMANENT, new StaticObstacle(new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS)));
				}else {
					undockedShips.add(ship.getId());
				}
			}else{
				if(ship.getDockingStatus()!=DockingStatus.UNDOCKED){
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
			Pathfinder pathfinder = new Pathfinder(circle.getPosition(), circle.getRadius()+0.001, obstacles,
					o->o.equals(uncertainObstacles.getValue(shipId))); //Weird use of Lambdas :)
			pathfinders.put(shipId, pathfinder);
		}
		Map<Integer, Double> scores = new HashMap<Integer, Double>();
		PriorityQueue<Integer> queue = new PriorityQueue<Integer>(Math.max(undockedShips.size(), 1), new Comparator<Integer>() {
			@Override
			public int compare(Integer shipA, Integer shipB) {
				return Double.compare(scores.get(shipA), scores.get(shipB));
			}
		});
		DebugLog.log("Assigning "+undockedShips.size()+" ships to "+TaskManager.INSTANCE.getTasks().size()+" tasks");
		Set<Integer> forcedShips = new HashSet<Integer>();
		BlameMap blameMap = new BlameMap();
		while(!undockedShips.isEmpty()&&checkInterruption()) {
			double bestScore = -Double.MAX_VALUE;
			Ship bestShip = null;
			Task bestTask = null;
			for(int shipId: undockedShips) {
				for(Task task: TaskManager.INSTANCE.getTasks()) {
					Ship ship = GameMap.INSTANCE.getMyPlayer().getShip(shipId);
					benchmark.push();
					double score = task.getScore(ship, bestScore);
					taskTime.put(task.getClass(), taskTime.getOrDefault(task.getClass(), 0L)+benchmark.pop());
					if(score>bestScore) {
						bestScore = score;
						bestTask = task;
						bestShip = ship;
					}
				}
			}
			bestTask.accept(bestShip);
			scores.put(bestShip.getId(), bestScore);
			TaskManager.INSTANCE.assignTask(bestShip.getId(), bestTask);
			undockedShips.remove((Object)bestShip.getId());
			queue.add(bestShip.getId());
			//Execute ships that are possible
			do {
				if(!blameMap.isEmpty()) {
					Ship ship = GameMap.INSTANCE.getMyPlayer().getShip(blameMap.getFirst());
					forcedShips.add(ship.getId());
					Obstacle uncertainObstacle = uncertainObstacles.getValue(ship.getId());
					obstacles.addObstacle(ObstacleType.PERMANENT, uncertainObstacle);
					obstacles.removeObstacle(ObstacleType.UNCERTAIN, uncertainObstacle);
					for(int shipId: blameMap.get(ship.getId())) {
						queue.add(shipId);
						if(forcedShips.contains(shipId)) {
							Obstacle obstacle = uncertainObstacles.getValue(shipId);
							obstacles.addObstacle(ObstacleType.UNCERTAIN, obstacle);
							obstacles.removeObstacle(ObstacleType.PERMANENT, obstacle);
							forcedShips.remove(shipId);
						}
					}
					blameMap.clear(ship.getId());
				}
				while(!queue.isEmpty()&&checkInterruption()) {
					Ship ship = GameMap.INSTANCE.getMyPlayer().getShip(queue.poll());
					Task task = TaskManager.INSTANCE.getTask(ship.getId());
					Move move = task.execute(ship, pathfinders.get(ship.getId()), blameMap, uncertainObstacles);
					if(move!=null) {
						if(blameMap.containsKey(ship.getId())) {
							for(int shipId: blameMap.get(ship.getId())) {
								queue.add(shipId);
								if(forcedShips.contains(shipId)) {
									Obstacle obstacle = uncertainObstacles.getValue(shipId);
									obstacles.addObstacle(ObstacleType.UNCERTAIN, obstacle);
									obstacles.removeObstacle(ObstacleType.PERMANENT, obstacle);
									forcedShips.remove(shipId);
								}
							}
							blameMap.clear(ship.getId());
						}
						if(move instanceof ThrustMove) {
							ThrustMove thrustMove = (ThrustMove)move;
							obstacles.addObstacle(ObstacleType.PERMANENT, new DynamicObstacle(
									new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS), thrustMove.getThrustPlan()));
							moveQueue.add(thrustMove, classId.getOrDefault(task.getClass(), 32));
						}else {
							obstacles.addObstacle(ObstacleType.PERMANENT, new StaticObstacle(
									new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS)));
							moveQueue.add(move);
						}
						obstacles.removeObstacle(ObstacleType.UNCERTAIN, uncertainObstacles.getValue(ship.getId()));
					}
				}
			}while((!blameMap.isEmpty())&&checkInterruption());
		}
		for(Class<? extends Task> clazz: taskTime.keySet()) {
			DebugLog.log("Scoring of "+clazz.getSimpleName()+" time: "+Benchmark.format(taskTime.get(clazz))+"s");
		}
		DebugLog.log("Final Turn Time: "+Benchmark.format(benchmark.pop())+"s");
	}
	public boolean checkInterruption() {
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
