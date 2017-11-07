package lemon.halite2.strats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import hlt.DebugLog;
import hlt.DockMove;
import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Position;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.Ship.DockingStatus;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.Pathfinder;

public class AdvancedStrategy implements Strategy {
	private GameMap gameMap;
	private Map<Integer, Integer> shipToPlanet;
	private Map<Integer, Integer> parentPlanets;
	private Map<Integer, List<Integer>> planetOrder; //Maps Planet to a List of Planets that are sorted by distance
	public AdvancedStrategy(GameMap gameMap) {
		this.gameMap = gameMap;
	}
	@Override
	public void init() {
		//Initialize Data Structures
		shipToPlanet = new HashMap<Integer, Integer>();
		parentPlanets = new HashMap<Integer, Integer>();
		planetOrder = new HashMap<Integer, List<Integer>>();
		//Calculate Starting Plan
		double averageX = 0;
		double averageY = 0;
		for(Ship ship: gameMap.getMyPlayer().getShips()) {
			averageX+=ship.getPosition().getX();
			averageY+=ship.getPosition().getY();
		}
		averageX/=gameMap.getMyPlayer().getShips().size();
		averageY/=gameMap.getMyPlayer().getShips().size();
		Position averageStart = new Position(averageX, averageY);
		
		int basePlanetId = calcBasePlanetId(averageStart);
		
		//Set all spawn ships to be children of basePlanet
		for(Ship ship: gameMap.getMyPlayer().getShips()){
			parentPlanets.put(ship.getId(), basePlanetId);
		}
		//Calculate planetOrder
		for(Planet planet: gameMap.getPlanets()) {
			Position spawnPoint = planet.getPosition().addPolar(planet.getRadius(),
					planet.getPosition().getDirectionTowards(gameMap.getCenterPosition()));
			planetOrder.put(planet.getId(), getClosestPlanets(spawnPoint));
		}
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
	public void newTurn() {
		shipToPlanet.clear();
		//Remove all dead ships
		List<Integer> deadShips = new ArrayList<Integer>(); //Prevents ConcurrentModificationException
		for(int shipId: parentPlanets.keySet()){
			if(gameMap.getMyPlayer().getShip(shipId)==null){
				deadShips.add(shipId);
			}
		}
		for(int shipId: deadShips){
			parentPlanets.remove(shipId);
		}
		//Check if any planets have exploded
		if(planetOrder.size()>gameMap.getPlanets().size()) {
			List<Integer> explodedPlanets = new ArrayList<Integer>();
			for(int planetId: planetOrder.keySet()) {
				if(gameMap.getPlanet(planetId)==null) { //This planet has exploded!
					explodedPlanets.add(planetId);
				}
			}
			for(int explodedPlanet: explodedPlanets) {
				planetOrder.remove(explodedPlanet); //Remove Exploded Planet
				for(int planetId: planetOrder.keySet()) {
					planetOrder.get(planetId).remove((Object)explodedPlanet); //Remove by Object, not index
				}
				//Reassigns children of the exploded planet
				for(int shipId: parentPlanets.keySet()) {
					if(parentPlanets.get(shipId)==explodedPlanet) {
						parentPlanets.put(shipId, getClosestPlanet(gameMap.getMyPlayer().getShip(shipId).getPosition()).getId());
					}
				}
			}
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
		//Initialize planetIndex map
		Map<Integer, Integer> planetIndex = new HashMap<Integer, Integer>(); //Maps planetId to index used in planetOrder
		for(int planetId: planetOrder.keySet()) {
			if(planetOrder.get(planetId).isEmpty()) {
				planetIndex.put(planetId, -1);
			}else {
				int index = 0;
				while(planetRequests.get(planetOrder.get(planetId).get(index))<=0) {
					index++;
					if(index>=planetOrder.get(planetId).size()) {
						index = -1;
						break;
					}
				}
				planetIndex.put(planetId, index);
			}
		}
		//Free Agents
		List<Integer> freeAgents = new ArrayList<Integer>();
		//Initialize shipToDistance map
		final Map<Integer, Double> shipToDistance = new HashMap<Integer, Double>(); //Maps ShipId to Distance to planet specified by planetIndex
		for(Ship ship: gameMap.getMyPlayer().getShips()) {
			if(ship.getDockingStatus()!=DockingStatus.UNDOCKED) {
				continue;
			}
			if(!parentPlanets.containsKey(ship.getId())) { //Ship has just spawned
				parentPlanets.put(ship.getId(), getClosestPlanet(ship.getPosition()).getId()); //Set parent planet
			}
			int parentPlanet = parentPlanets.get(ship.getId());
			int index = planetIndex.get(parentPlanet);
			if(index==-1) {
				freeAgents.add(ship.getId());
				continue;
			}
			int targetPlanetId = planetOrder.get(parentPlanet).get(index);
			Planet targetPlanet = gameMap.getPlanet(targetPlanetId);
			double distance = ship.getPosition().getDistanceTo(targetPlanet.getPosition())-targetPlanet.getRadius();
			shipToDistance.put(ship.getId(), distance);
		}
		//Initialize PriorityQueue with custom comparator
		PriorityQueue<Integer> queue = new PriorityQueue<Integer>(new Comparator<Integer>() {
			@Override
			public int compare(Integer a, Integer b) {
				return Double.compare(shipToDistance.get(a), shipToDistance.get(b));
			}
		});
		for(int shipId: shipToDistance.keySet()) {
			queue.add(shipId);
		}
		DebugLog.log("Processing Queue");
		//Assign Ships
		while(!queue.isEmpty()) {
			int popped = queue.poll();
			int parentPlanet = parentPlanets.get(popped);
			int targetPlanetId = planetOrder.get(parentPlanet).get(planetIndex.get(parentPlanet));
			DebugLog.log("Processing Ship ID="+popped+";target="+targetPlanetId+";parent="+parentPlanet);
			if(planetRequests.get(targetPlanetId)>0) {
				DebugLog.log("\tAssigned with "+planetRequests.get(targetPlanetId)+" requests");
				shipToPlanet.put(popped, targetPlanetId);
				planetRequests.put(targetPlanetId, planetRequests.get(targetPlanetId)-1);
			}else {
				DebugLog.log("\tFufilled Requests! Calculating");
				//Update Planet Index
				int index = planetIndex.get(parentPlanet);
				while(planetRequests.get(planetOrder.get(parentPlanet).get(index))<=0) {
					index++;
					if(index>=planetOrder.get(parentPlanet).size()) {
						index = -1;
						break;
					}
				}
				planetIndex.put(parentPlanet, index);
				List<Integer> sharedParents = new ArrayList<Integer>(); //Prevents ConcurrentModificationException
				for(int shipId: shipToDistance.keySet()) {
					int pPlanet = parentPlanets.get(shipId); //parent planet of shipId
					if(pPlanet==parentPlanet) {
						sharedParents.add(shipId);
					}
				}
				if(index==-1) {
					DebugLog.log("\tNo more planets - adding to FreeAgents");
					for(int shipId: sharedParents) {
						if(queue.contains(shipId)) {
							queue.remove(shipId);
							shipToDistance.remove(shipId);
							freeAgents.add(shipId);
						}
					}
					freeAgents.add(popped);
					shipToDistance.remove(popped);
				}else {
					int newPlanetId = planetOrder.get(parentPlanet).get(index);
					DebugLog.log("\tRecalculated order: "+newPlanetId);
					Planet newPlanet = gameMap.getPlanet(newPlanetId);
					for(int shipId: sharedParents) {
						if(queue.contains(shipId)) {
							double distance = gameMap.getMyPlayer().getShip(shipId).getPosition()
									.getDistanceTo(newPlanet.getPosition())-newPlanet.getRadius();
							shipToDistance.put(shipId, distance); //Recalculate Distance
							queue.remove(shipId);
							queue.add(shipId); //Update PriorityQueue
						}
					}
					queue.add(popped); //Re-add popped ship
				}
			}
		}
		DebugLog.log("Free Agents: "+Arrays.toString(freeAgents.toArray()));
		//Assign Free Agents - Target planet that is not ours
		if(!freeAgents.isEmpty()) {
			for(Planet planet: gameMap.getPlanets()) {
				if(planet.getOwner()!=gameMap.getMyPlayerId()) {
					for(int shipId: freeAgents){
						shipToPlanet.put(shipId, planet.getId());
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
						shipToPlanet.put(freeAgents.get(0), planet.getId());
						freeAgents.remove(0);
					}
				}else{
					for(int shipId: freeAgents){
						shipToPlanet.put(shipId, planet.getId());
					}
					freeAgents.clear();
				}
			}
		}
		if(!freeAgents.isEmpty()){
			throw new IllegalStateException("How do we still have unassigned ships: "+freeAgents.size()); //Fail Fast
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
	@Override
	//Handles each individual ship based off the given target
	public int handleShip(List<Integer> handledShips, int shipId, MoveQueue moveQueue){
		Ship ship = gameMap.getShip(gameMap.getMyPlayerId(), shipId);
		
		if(ship.getDockingStatus()!=DockingStatus.UNDOCKED) { //Checks if ship is undocked
			return -1;
		}
		
		Planet targetPlanet = gameMap.getPlanet(shipToPlanet.get(shipId));
		Planet closestPlanet = getClosestPlanet(ship.getPosition());
		
		if(isSafeToDock(ship.getPosition(), closestPlanet)&&ship.canDock(closestPlanet)) {
			return moveQueue.addMove(new DockMove(ship, closestPlanet));
		}else{
			Ship enemyShip = findEnemyShip(targetPlanet, ship.getPosition());
			ThrustMove move;
			if(enemyShip==null) {
				move = Pathfinder.pathfind(ship.getPosition(), targetPlanet.getPosition(), GameConstants.SHIP_RADIUS, targetPlanet.getRadius()).apply(ship);
			}else {
				if(enemyShip.getDockingStatus()==DockingStatus.UNDOCKED&&enemyShip.getHealth()>ship.getHealth()) {
					//try to crash into enemy ship
					move = Pathfinder.pathfind(ship.getPosition(), enemyShip.getPosition(), GameConstants.SHIP_RADIUS, 0).apply(ship);
				}else {
					move = Pathfinder.pathfind(ship.getPosition(), enemyShip.getPosition(), GameConstants.SHIP_RADIUS, GameConstants.WEAPON_RADIUS*0.5).apply(ship);
				}
			}
			int request = moveQueue.addMove(move);
			while(request!=-1&&handledShips.contains(request)) {
				move.setThrust(Math.min(move.getThrust()-1, 0));
				request = moveQueue.addMove(move);
			}
			return request;
		}
	}
	public boolean isSafeToDock(Position position, Planet planet) {
		for(Ship ship: gameMap.getShips()) {
			if(ship.getOwner()==gameMap.getMyPlayerId()) {
				continue;
			}
			//Check if there are any enemy ships nearby position
			if(ship.getPosition().getDistanceSquared(position)<10*10) {
				return false;
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
