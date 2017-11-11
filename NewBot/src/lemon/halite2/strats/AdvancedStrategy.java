package lemon.halite2.strats;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import hlt.DebugLog;
import hlt.DockMove;
import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Position;
import hlt.Ship;
import lemon.halite2.micro.Group;
import lemon.halite2.micro.MicroGame;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.PathfindPlan;
import lemon.halite2.util.Pathfinder;

public class AdvancedStrategy implements Strategy {
	private GameMap gameMap;
	private Map<Integer, Map<Group, Boolean>> targetPlanets;
	private MicroGame micro;
	public AdvancedStrategy(GameMap gameMap) {
		this.gameMap = gameMap;
	}
	@Override
	public void init() {
		//Initialize Data Structures
		targetPlanets = new HashMap<Integer, Map<Group, Boolean>>();
		//Initialize MicroGame
		micro = new MicroGame(gameMap);
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
		micro.update();
		targetPlanets.clear();
		for(Planet planet: gameMap.getPlanets()){
			targetPlanets.put(planet.getId(), new TreeMap<Group, Boolean>());
		}
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
				targetPlanets.get(targetPlanetId).put(popped, false);
				planetRequests.put(targetPlanetId, planetRequests.get(targetPlanetId)-popped.getSize());
			}else{
				DebugLog.log("\tFufilled Requests! Recalculating");
				int index = groupToPlanetOrderIndex.get(popped);
				if(index+1>=groupToPlanetOrder.get(popped).size()){
					DebugLog.log("\tNo more planets! Adding to Free Agents");
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
		DebugLog.log("Free Agents: "+Arrays.toString(freeAgents.toArray()));
		//Assign Free Agents - Target planet that is not ours
		if(!freeAgents.isEmpty()) {
			for(Planet planet: gameMap.getPlanets()) {
				if(planet.getOwner()!=gameMap.getMyPlayerId()) {
					for(Group group: freeAgents){
						targetPlanets.get(planet.getId()).put(group, false);
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
						targetPlanets.get(planet.getId()).put(freeAgents.pop(), false);
					}
				}else{
					for(Group group: freeAgents){
						targetPlanets.get(planet.getId()).put(group, false);
					}
					freeAgents.clear();
				}
			}
		}
		if(!freeAgents.isEmpty()){
			throw new IllegalStateException("How do we still have unassigned ships: "+freeAgents.size()); //Fail Fast
		}
		for(int planetId: targetPlanets.keySet()) { //Resolve Docking
			Planet targetPlanet = gameMap.getPlanet(planetId);
			if(isSafeToDock(targetPlanet)&&!targetPlanet.isFull()) {
				Set<Group> oldGroups = new HashSet<Group>();
				Set<Group> newGroups = new HashSet<Group>();
				for(Group group: targetPlanets.get(planetId).keySet()){
					if(group.getSize()>1) {
						oldGroups.add(group);
						for(int shipId: group.getShips().keySet()) {
							newGroups.add(new Group(gameMap.getMyPlayer().getShip(shipId)));
						}
					}
				}
				for(Group group: oldGroups) {
					targetPlanets.get(planetId).remove(group);
					micro.getGroups().remove(group);
				}
				for(Group group: newGroups) {
					targetPlanets.get(planetId).put(group, false);
					micro.getGroups().add(group);
				}
				for(Group group: targetPlanets.get(planetId).keySet()) {
					if(!targetPlanets.get(planetId).get(group)) {
						for(int shipId: group.getShips().keySet()) {
							if(gameMap.getMyPlayer().getShip(shipId).canDock(targetPlanet)) {
								DebugLog.log("\tDocking Ship "+shipId+" to Planet "+targetPlanet.getId());
								moveQueue.forceMove(new DockMove(gameMap.getMyPlayer().getShip(shipId), targetPlanet));
								targetPlanets.get(planetId).put(group, true);
								Pathfinder.addStaticObstacle(group.getCircle());
							}
							break; //There's only supposed to be one ship per group
						}
					}
				}
			}
		}
		for(int planetId: targetPlanets.keySet()){ //Resolve by planet
			Planet targetPlanet = gameMap.getPlanet(planetId);
			if(targetPlanets.get(planetId).size()>0) {
				DebugLog.log("Handling Planet "+planetId+" with "+targetPlanets.get(planetId).size()+" ship(s)");
			}
			//Handle Merging - merge only if enemy planet
			
			//Pathfind remaining groups
			for(Group group: targetPlanets.get(planetId).keySet()){
				if(!targetPlanets.get(planetId).get(group)){
					PathfindPlan plan = Pathfinder.pathfind(group.getCircle().getPosition(), targetPlanet.getPosition(),
							group.getCircle().getRadius(), targetPlanet.getRadius());
					if(plan==null) {
						DebugLog.log("\tCan't move");
						Pathfinder.addStaticObstacle(group.getCircle());
					}else {
						group.move(gameMap, moveQueue, plan);
						Pathfinder.addDynamicObstacle(group.getCircle(), plan.toVelocity());
					}
				}else {
					DebugLog.log("\tAlready Processed Group: "+group.getCircle().getPosition());
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
