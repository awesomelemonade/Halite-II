package lemon.halite2.strats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hlt.DebugLog;
import hlt.DockMove;
import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Position;
import hlt.Ship;
import hlt.Ship.DockingStatus;
import hlt.ThrustMove;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.Pathfinder;

public class BasicStrategy2 implements Strategy {
	private GameMap gameMap;
	public BasicStrategy2(GameMap gameMap){
		this.gameMap = gameMap;
	}
	private Map<Integer, Integer> shipToPlanet; //maps ship to their target planet
	private Map<Integer, Integer> parentPlanets; //maps ship to planets they have spawned from
	private Map<Integer, List<Integer>> closestPlanetIds; //maps planet to a list of all planets sorted by distance from spawn point
	public void init() {
		//Initialize all data structures
		shipToPlanet = new HashMap<Integer, Integer>();
		parentPlanets = new HashMap<Integer, Integer>();
		closestPlanetIds = new HashMap<Integer, List<Integer>>();
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
		//Calculate closestPlanetIds
		for(Planet planet: gameMap.getPlanets()){
			Position position = planet.getPosition();
			Position spawnPosition = position.addPolar(planet.getRadius(), position.getDirectionTowards(gameMap.getCenterPosition()));
			closestPlanetIds.put(planet.getId(), calcClosestPlanetIds(spawnPosition));
		}
	}
	public int calcBasePlanetId(Position averageStart){
		double mostDockingSpots = 0;
		double closestDistance = Double.MAX_VALUE;
		int basePlanetId = -1;
		
		for(Planet planet: gameMap.getPlanets()) {
			double distanceSquared = planet.getPosition().getDistanceSquared(averageStart);
			if(distanceSquared>4900) {
				continue;
			}
			if(planet.getDockingSpots()>mostDockingSpots||(planet.getDockingSpots()==mostDockingSpots&&distanceSquared<closestDistance)) {
				mostDockingSpots = planet.getDockingSpots();
				basePlanetId = planet.getId();
				closestDistance = distanceSquared;
			}
		}
		if(basePlanetId==-1) {
			basePlanetId = getClosestPlanet(averageStart).getId();
		}
		return basePlanetId;
	}
	public List<Integer> calcClosestPlanetIds(final Position position) {
		List<Integer> planets = new ArrayList<Integer>();
		for(Planet planet: gameMap.getPlanets()){
			planets.add(planet.getId());
		}
		Collections.sort(planets, new Comparator<Integer>() {
			@Override
			public int compare(Integer a, Integer b) {
				return Double.compare(gameMap.getPlanet(a).getPosition().getDistanceSquared(position),
						gameMap.getPlanet(b).getPosition().getDistanceSquared(position));
			}
		});
		return planets;
	}
	public void newTurn() {
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
		//Remove planets that have exploded from closestPlanetIds & Reassign the children of that exploded planet
		if(closestPlanetIds.size()>gameMap.getPlanets().size()){ //Detect whether planets have exploded
			List<Integer> deadPlanets = new ArrayList<Integer>(); //Prevents ConcurrentModificationException
			for(int planetId: closestPlanetIds.keySet()){
				if(gameMap.getPlanet(planetId)==null){
					deadPlanets.add(planetId);
				}
			}
			for(int planetId: deadPlanets){
				for(int shipId: parentPlanets.keySet()){
					if(parentPlanets.get(shipId)==planetId){
						parentPlanets.put(shipId,
								getClosestPlanet(gameMap.getMyPlayer().getShip(shipId).getPosition()).getId()); //reset parent planet
					}
				}
				closestPlanetIds.remove(planetId);
			}
		}
		//calculate # of requests for each planet
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
		//Assign ships to planets
		List<Ship> unassigned = new ArrayList<Ship>();
		for(Ship ship: gameMap.getMyPlayer().getShips()){
			if(ship.getDockingStatus()!=DockingStatus.UNDOCKED){ //Ignore ships that are not undocked
				continue;
			}
			if(!parentPlanets.containsKey(ship.getId())){ //Ship has just spawned
				parentPlanets.put(ship.getId(), getClosestPlanet(ship.getPosition()).getId()); //Set parent planet
			}
			for(int planetId: closestPlanetIds.get(parentPlanets.get(ship.getId()))){
				if(planetRequests.get(planetId)>0){
					shipToPlanet.put(ship.getId(), planetId);
					planetRequests.put(planetId, planetRequests.get(planetId)-1); //Decrease 1
					break;
				}
			}
			if(shipToPlanet.get(ship.getId())==null){
				unassigned.add(ship);
			}
		}
		//We've.. taken all planet requests? Target a random enemy planet
		if(!unassigned.isEmpty()) {
			for(Planet planet: gameMap.getPlanets()) {
				if(planet.getOwner()!=gameMap.getMyPlayerId()) {
					for(Ship ship: unassigned){
						shipToPlanet.put(ship.getId(), planet.getId());
					}
					unassigned.clear();
					break;
				}
			}
		}
		//No more enemy planets? Distribute ship to all planets
		if(!unassigned.isEmpty()){
			int totalShips = unassigned.size();
			for(Planet planet: gameMap.getPlanets()){
				int split = (int)Math.ceil(((double)totalShips)/((double)gameMap.getPlanets().size()));
				if(unassigned.size()>=split){
					for(int i=0;i<split;++i){
						shipToPlanet.put(unassigned.get(0).getId(), planet.getId());
						unassigned.remove(0);
					}
				}else{
					for(Ship ship: unassigned){
						shipToPlanet.put(ship.getId(), planet.getId());
					}
					unassigned.clear();
				}
			}
		}
		if(!unassigned.isEmpty()){
			throw new IllegalStateException("How do we still have unassigned ships: "+unassigned.size()); //Fail Fast
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
	//Handles each individual ship based off the given target
	public int handleShip(List<Integer> handledShips, int shipId, MoveQueue moveQueue){
		Ship ship = gameMap.getShip(gameMap.getMyPlayerId(), shipId);
		
		if(ship.getDockingStatus()!=DockingStatus.UNDOCKED) { //Checks if ship is undocked
			return -1;
		}
		
		Planet targetPlanet = gameMap.getPlanet(shipToPlanet.get(shipId));
		Planet closestPlanet = getClosestPlanet(ship.getPosition());
		
		if(isSafeToDock(ship.getPosition(), closestPlanet)&&ship.canDock(closestPlanet)) {
			DebugLog.log("Docking Ship: "+ship.getId());
			return moveQueue.addMove(new DockMove(ship, closestPlanet));
		}else{
			Ship enemyShip = findEnemyShip(targetPlanet, ship.getPosition());
			ThrustMove move;
			if(enemyShip==null) {
				move = Pathfinder.pathfind(ship, ship.getPosition(), targetPlanet.getPosition(), GameConstants.SHIP_RADIUS, targetPlanet.getRadius());
			}else {
				if(enemyShip.getDockingStatus()==DockingStatus.UNDOCKED&&enemyShip.getHealth()>ship.getHealth()) {
					//try to crash into enemy ship
					move = Pathfinder.pathfind(ship, ship.getPosition(), enemyShip.getPosition(), GameConstants.SHIP_RADIUS, 0);
				}else {
					move = Pathfinder.pathfind(ship, ship.getPosition(), enemyShip.getPosition(), GameConstants.SHIP_RADIUS, GameConstants.SHIP_RADIUS+0.5);
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
}
