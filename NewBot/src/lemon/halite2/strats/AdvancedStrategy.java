package lemon.halite2.strats;

import java.util.ArrayList;
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
import hlt.UndockMove;
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
import lemon.halite2.task.projection.Projection;
import lemon.halite2.task.projection.ProjectionManager;
import lemon.halite2.util.BiMap;
import lemon.halite2.util.Circle;
import lemon.halite2.util.MoveQueue;

public class AdvancedStrategy implements Strategy {
	private Benchmark benchmark;
	private Map<Class<? extends Task>, Integer> classId;
	private Map<Class<? extends Task>, Long> scoreTime;
	private Map<Class<? extends Task>, Long> executeTime;
	private Map<Class<? extends Task>, Integer> executeCount;
	@Override
	public void init() {
		//Initialize Benchmark
		benchmark = new Benchmark();
		//Initialize benchmark maps
		scoreTime = new HashMap<Class<? extends Task>, Long>();
		executeTime = new HashMap<Class<? extends Task>, Long>();
		executeCount = new HashMap<Class<? extends Task>, Integer>();
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
	@Override
	public void newTurn(MoveQueue moveQueue) {
		benchmark.push();
		//Clear benchmark maps
		scoreTime.clear();
		executeTime.clear();
		executeCount.clear();
		//Update Managers
		ProjectionManager.INSTANCE.update();
		TaskManager.INSTANCE.update();
		//Preprocess Tasks
		benchmark.push();
		AttackEnemyTask.newTurn();
		DebugLog.log("AttackEnemyTask Preprocess: "+Benchmark.format(benchmark.pop())+"s");
		//Define ships to be processed
		List<Integer> undockedShips = new ArrayList<Integer>();
		//Process undockedShips and undocking of docked ships
		for(Ship ship: GameMap.INSTANCE.getMyPlayer().getShips()) {
			if(ship.getDockingStatus()==DockingStatus.UNDOCKED) {
				undockedShips.add(ship.getId());
			}
			if(ship.getDockingStatus()==DockingStatus.DOCKED) {
				//Check if we should undock
				Projection projection = ProjectionManager.INSTANCE.calculate(ship.getPosition(), 3, s->s.getId()==ship.getId());
				if(projection.getEnemyProjectionItems().first().getDistanceSquared()>
						GameConstants.MAX_SPEED*GameConstants.UNDOCK_TURNS*GameConstants.MAX_SPEED*GameConstants.UNDOCK_TURNS&&
						(!projection.isSafe((enemy, friendly)->enemy<friendly))) {
					ProjectionManager.INSTANCE.addUndockingShip(ship.getId());
					moveQueue.add(new UndockMove(ship.getId()));
					DebugLog.log("Undocking Ship: "+ship.getId());
				}
			}
		}
		//Define Obstacles
		Obstacles<ObstacleType> obstacles = new Obstacles<ObstacleType>();
		//Add Map Border Obstacle
		obstacles.addObstacle(ObstacleType.PERMANENT, new MapBorderObstacle(new Vector(0, 0), new Vector(GameMap.INSTANCE.getWidth(), GameMap.INSTANCE.getHeight())));
		//Add Ship Obstacles
		for(Ship ship: GameMap.INSTANCE.getShips()){
			if(ship.getOwner()==GameMap.INSTANCE.getMyPlayerId()){
				if(ship.getDockingStatus()!=DockingStatus.UNDOCKED){
					obstacles.addObstacle(ObstacleType.PERMANENT, new StaticObstacle(new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS)));
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
		mainLoop: while(!undockedShips.isEmpty()&&(!isInterrupted())) {
			double bestScore = -Double.MAX_VALUE;
			Ship bestShip = null;
			Task bestTask = null;
			for(int shipId: undockedShips) {
				for(Task task: TaskManager.INSTANCE.getTasks()) {
					if(isInterrupted()) {
						break mainLoop;
					}
					Ship ship = GameMap.INSTANCE.getMyPlayer().getShip(shipId);
					benchmark.push();
					double score = task.getScore(ship, bestScore);
					scoreTime.put(task.getClass(), scoreTime.getOrDefault(task.getClass(), 0L)+benchmark.pop());
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
				while(!queue.isEmpty()&&(!isInterrupted())) {
					Ship ship = GameMap.INSTANCE.getMyPlayer().getShip(queue.poll());
					Task task = TaskManager.INSTANCE.getTask(ship.getId());
					benchmark.push();
					Move move = task.execute(ship, pathfinders.get(ship.getId()), blameMap, uncertainObstacles);
					executeTime.put(task.getClass(), executeTime.getOrDefault(task.getClass(), 0L)+benchmark.pop());
					executeCount.put(task.getClass(), executeCount.getOrDefault(task.getClass(), 0)+1);
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
			}while((!blameMap.isEmpty())&&(!isInterrupted()));
		}
		for(Class<? extends Task> clazz: classId.keySet()) {
			DebugLog.log(String.format("%s time: scoring=%ss, execution=%ss (count=%d, average=%s)", clazz.getSimpleName(),
					Benchmark.format(scoreTime.getOrDefault(clazz, 0L)), Benchmark.format(executeTime.getOrDefault(clazz, 0L)),
					executeCount.getOrDefault(clazz, 0), executeCount.getOrDefault(clazz, 0)==0?"Infinity":
						(Benchmark.format(executeTime.getOrDefault(clazz, 0L)/executeCount.getOrDefault(clazz, 0)))));
		}
		DebugLog.log("Final Turn Time: "+Benchmark.format(benchmark.pop())+"s");
	}
	public boolean isInterrupted() {
		return Thread.currentThread().isInterrupted();
	}
}
