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
import hlt.Position;
import hlt.Ship;
import hlt.ThrustPlan;
import hlt.Ship.DockingStatus;
import lemon.halite2.micro.Group;
import lemon.halite2.micro.MicroGame;
import lemon.halite2.util.Circle;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.Obstacles;
import lemon.halite2.util.Pathfinder;

public class AdvancedStrategy implements Strategy {
	private GameMap gameMap;
	private Map<Integer, Integer> targetPlanets;
	private MicroGame micro;
	public AdvancedStrategy(GameMap gameMap) {
		this.gameMap = gameMap;
	}
	@Override
	public void init() {
		//Initialize Data Structures
		targetPlanets = new HashMap<Integer, Integer>();
		//Initialize MicroGame
		micro = new MicroGame(gameMap);
		//Initialize Pathfinder
		Pathfinder.init();
		//Initialize Obstacles
		Obstacles.init();
	}
	public int calcBasePlanetId(Position averageStart){
		double mostDockingSpots = 0;
		double closestDistance = Double.MAX_VALUE;
		int basePlanetId = -1;
		DebugLog.log("Calculating Base Planet");
		for(Planet planet: gameMap.getPlanets()) {
			double distance = planet.getPosition().getDistanceTo(averageStart)-planet.getRadius();
			double actualDistance = distance;
			if(distance>50) {
				continue;
			}
			distance-=0.25*(planet.getPosition().getDistanceTo(gameMap.getCenterPosition())-planet.getRadius()); //Prefer to be not near center
			DebugLog.log("Evaluating Planet "+planet.getId()+": "+planet.getDockingSpots()+" - "+actualDistance+" - "+distance);
			if(planet.getDockingSpots()>mostDockingSpots||(planet.getDockingSpots()==mostDockingSpots&&distance<closestDistance)) {
				mostDockingSpots = planet.getDockingSpots();
				basePlanetId = planet.getId();
				closestDistance = distance;
			}
		}
		if(basePlanetId==-1) {
			basePlanetId = getClosestPlanet(averageStart).getId();
			DebugLog.log("Using Closest Planet: "+basePlanetId);
		}
		DebugLog.log("Base Planet Picked: "+basePlanetId);
		return basePlanetId;
	}
	public List<Integer> getClosestPlanets(Position position){
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
		DebugLog.log("ID Counter: "+Group.getIdCounter());
		micro.update();
		targetPlanets.clear();
		//Calculate Planet Requests
		Map<Integer, Integer> planetRequests = new HashMap<Integer, Integer>();
		for(Planet planet: gameMap.getPlanets()){
			if(planet.isOwned()){
				int enemyShips = countEnemyShips(planet.getPosition(), planet.getRadius()+GameConstants.DOCK_RADIUS+GameConstants.WEAPON_RADIUS);
				if(planet.getOwner()==gameMap.getMyPlayerId()){
					int dockingSpotsLeft = planet.getDockingSpots()-planet.getDockedShips().size();
					planetRequests.put(planet.getId(), Math.max(enemyShips, dockingSpotsLeft));
				}else{
					planetRequests.put(planet.getId(), Math.max(enemyShips, planet.getDockingSpots()));
				}
			}else{
				planetRequests.put(planet.getId(), planet.getDockingSpots());
			}
		}
		//Free Agents
		Deque<Group> freeAgents = new ArrayDeque<Group>();
		//Initialize groupToDistance map
		final Map<Group, Double> groupToDistance = new HashMap<Group, Double>(); //Maps Group to distance of target planet
		//Initialize PriorityQueue with custom comparator
		PriorityQueue<Group> queue = new PriorityQueue<Group>(new Comparator<Group>() {
			@Override
			public int compare(Group a, Group b) {
				return Double.compare(groupToDistance.get(a), groupToDistance.get(b));
			}
		});
		Map<Group, List<Integer>> groupToPlanetOrder = new HashMap<Group, List<Integer>>(); //Maps Group to priority of planets
		Map<Group, Integer> groupToPlanetOrderIndex = new HashMap<Group, Integer>();
		for(Group group: micro.getGroups()){
			groupToPlanetOrder.put(group, getClosestPlanets(group.getCircle().getPosition()));
			groupToPlanetOrderIndex.put(group, 0);
			Planet targetPlanet = gameMap.getPlanet(groupToPlanetOrder.get(group).get(groupToPlanetOrderIndex.get(group)));
			double distance = targetPlanet.getPosition().getDistanceTo(group.getCircle().getPosition())
					-group.getCircle().getRadius()-targetPlanet.getRadius();
			groupToDistance.put(group, distance);
			queue.add(group);
		}
		DebugLog.log("Processing Queue");
		//Assign Groups to Planets
		while(!queue.isEmpty()) {
			Group popped = queue.poll();
			int targetPlanetId = groupToPlanetOrder.get(popped).get(groupToPlanetOrderIndex.get(popped));
			if(planetRequests.get(targetPlanetId)>0){
				DebugLog.log("\tAssigned "+popped.getSize()+" ship(s) with "+planetRequests.get(targetPlanetId)+" requests to planet "+targetPlanetId);
				targetPlanets.put(popped.getId(), targetPlanetId);
				planetRequests.put(targetPlanetId, planetRequests.get(targetPlanetId)-popped.getSize());
			}else{
				int index = groupToPlanetOrderIndex.get(popped);
				if(index+1>=groupToPlanetOrder.get(popped).size()){
					freeAgents.add(popped);
				}else{
					Planet newTargetPlanet = gameMap.getPlanet(groupToPlanetOrder.get(popped).get(index+1));
					double distance = newTargetPlanet.getPosition().getDistanceTo(popped.getCircle().getPosition())
							-popped.getCircle().getRadius()-newTargetPlanet.getRadius();
					groupToDistance.put(popped, distance);
					groupToPlanetOrderIndex.put(popped, index+1);
					queue.add(popped);
				}
			}
		}
		//Assign Free Agents - Target planet that is not ours
		if(!freeAgents.isEmpty()) {
			for(Planet planet: gameMap.getPlanets()) {
				if(planet.getOwner()!=gameMap.getMyPlayerId()) {
					for(Group group: freeAgents){
						targetPlanets.put(group.getId(), planet.getId());
					}
					freeAgents.clear();
					break;
				}
			}
		}
		//We've conquered all planets!
		if(!freeAgents.isEmpty()) {
			int totalShips = freeAgents.size();
			for(Planet planet: gameMap.getPlanets()){
				int split = (int)Math.ceil(((double)totalShips)/((double)gameMap.getPlanets().size()));
				if(freeAgents.size()>=split){
					for(int i=0;i<split;++i){
						targetPlanets.put(freeAgents.pop().getId(), planet.getId());
					}
				}else{
					for(Group group: freeAgents){
						targetPlanets.put(group.getId(), planet.getId());
					}
					freeAgents.clear();
				}
			}
		}
		if(!freeAgents.isEmpty()){
			throw new IllegalStateException("How do we still have unassigned ships: "+freeAgents.size()); //Fail Fast
		}
		Obstacles.clear();
		//Add Planets to Obstacles
		for(Planet planet: gameMap.getPlanets()) {
			Obstacles.addStaticObstacle(new Circle(planet.getPosition(), planet.getRadius()));
		}
		//Add Docked Ships to Obstacles
		for(Ship ship: gameMap.getMyPlayer().getShips()) {
			if(ship.getDockingStatus()!=DockingStatus.UNDOCKED) {
				Obstacles.addStaticObstacle(new Circle(ship.getPosition(), GameConstants.SHIP_RADIUS));
			}
		}
		Deque<Integer> groupQueue = new ArrayDeque<Integer>(); //Queue of Group IDs
		Deque<Integer> resolved = new ArrayDeque<Integer>();
		//Add all to queue
		for(int groupId: targetPlanets.keySet()) {
			groupQueue.push(groupId);
		}
		DebugLog.log(String.format("Resolving %d Groups", groupQueue.size()));
		//Resolve Docking; Adds docking ship to static obstacles
		while(!groupQueue.isEmpty()) {
			Group group = Group.getGroup(groupQueue.peek());
			Planet targetPlanet = gameMap.getPlanet(targetPlanets.get(groupQueue.poll()));
			if(isSafeToDock(targetPlanet)&&!targetPlanet.isFull()) {
				if(group.getSize()>1) {
					micro.getGroups().remove(group);
					for(int shipId: group.getShips().keySet()) {
						Group newGroup = new Group(gameMap.getMyPlayer().getShip(shipId));
						groupQueue.push(newGroup.getId());
						micro.getGroups().add(newGroup);
					}
				}else {
					for(int shipId: group.getShips().keySet()) {
						if(gameMap.getMyPlayer().getShip(shipId).canDock(targetPlanet)) {
							DebugLog.log("\tDocking Ship "+shipId+" to Planet "+targetPlanet.getId());
							moveQueue.add(new DockMove(shipId, targetPlanet.getId()));
							Obstacles.addStaticObstacle(group.getCircle());
						}else {
							resolved.push(group.getId());
						}
					}
				}
			}else {
				resolved.push(group.getId());
			}
		}
		while(!resolved.isEmpty()) {
			groupQueue.push(resolved.poll());
		}
		//Add Uncertain Obstacles
		for(int groupId: groupQueue) {
			Obstacles.addUncertainObstacle(Group.getGroup(groupId).getCircle(), groupId);
		}
		//Resolve Micro
		//	Merging
		Map<Integer, Pathfinder> pathfinders = new HashMap<Integer, Pathfinder>();
		for(int groupId: groupQueue) { //Attach a pathfinder to all groups that are left
			Group group = Group.getGroup(groupId);
			Pathfinder pathfinder = new Pathfinder(group.getCircle().getPosition(), group.getCircle().getRadius());
			pathfinder.resolveStaticObstacles();
			pathfinders.put(groupId, pathfinder);
		}
		Map<Integer, Set<Integer>> blameMap = new HashMap<Integer, Set<Integer>>();
		while(!groupQueue.isEmpty()) {
			Group group = Group.getGroup(groupQueue.poll());
			Pathfinder pathfinder = pathfinders.get(group.getId());
			pathfinder.clearUncertainObstacles();
			pathfinder.resolveUncertainObstacles(group.getId());
			Planet targetPlanet = gameMap.getPlanet(targetPlanets.get(group.getId()));
			ThrustPlan plan = pathfinder.getGreedyPlan(targetPlanet.getPosition(), targetPlanet.getRadius());
			if(plan==null) {
				DebugLog.log(String.format("\tCan't Move: %d", group.getId()));
			}else {
				int candidate = pathfinder.getCandidate(plan);
				if(candidate==Pathfinder.NO_CONFLICT) {
					group.move(gameMap, moveQueue, plan);
					pathfinders.remove(group.getId());
					Obstacles.addDynamicObstacle(group.getCircle(), plan);
					for(Pathfinder p: pathfinders.values()) {
						p.resolveDynamicObstacle(group.getCircle(), plan);
					}
					Obstacles.removeUncertainObstacle(group.getId());
					if(blameMap.containsKey(group.getId())) {
						for(int groupId: blameMap.get(group.getId())) {
							groupQueue.push(groupId);
						}
						blameMap.get(group.getId()).clear();
					}
				}else if(candidate!=Pathfinder.CONFLICT) {
					if(!blameMap.containsKey(candidate)) {
						blameMap.put(candidate, new HashSet<Integer>());
					}
					blameMap.get(candidate).add(group.getId());
				}else {
					DebugLog.log(String.format("\tConflict? %d", group.getId()));
				}
			}
		}
	}
	public int countEnemyShips(Position position, double buffer) {
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
	public Planet getClosestPlanet(Position position) {
		Planet closestPlanet = null;
		double closestDistance = Double.MAX_VALUE;
		for(Planet planet: gameMap.getPlanets()) {
			double distance = position.getDistanceSquared(planet.getPosition());
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
	public Ship findEnemyShip(Planet planet, Position position) {
		double bufferSquared = (planet.getRadius()+GameConstants.DOCK_RADIUS+GameConstants.SHIP_RADIUS+GameConstants.WEAPON_RADIUS);
		bufferSquared = bufferSquared*bufferSquared;
		
		double startDirection = planet.getPosition().getDirectionTowards(position);
		double angleBuffer = 2*Math.asin(4/Math.sqrt(bufferSquared)); //8 degrees of buffer; you can calculate this in init time to make it faster
		double shortestDirection = Double.MAX_VALUE;
		Ship shortestShip = null;
		
		for(Ship ship: gameMap.getShips()) {
			if(ship.getOwner()==gameMap.getMyPlayerId()) {
				continue;
			}
			if(planet.getPosition().getDistanceSquared(ship.getPosition())<bufferSquared) {
				double targetDirection = planet.getPosition().getDirectionTowards(ship.getPosition());
				if(Math.abs(targetDirection-startDirection)<angleBuffer) {
					return ship;
				}
				double deltaDirection = (targetDirection-startDirection)%(2*Math.PI);
				if(deltaDirection<0) {
					deltaDirection+=(2*Math.PI);
				}
				if(shortestDirection>deltaDirection) {
					shortestDirection = deltaDirection;
					shortestShip = ship;
				}
			}
		}
		return shortestShip;
	}
}
