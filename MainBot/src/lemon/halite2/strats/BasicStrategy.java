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

public class BasicStrategy {
	private GameMap gameMap;
	public BasicStrategy(GameMap gameMap){
		this.gameMap = gameMap;
		init();
	}
	private List<Integer> closestPlanetIds;
	private Map<Integer, Integer> shipToPlanet;
	public void init() {
		double averageX = 0;
		double averageY = 0;
		for(Ship ship: gameMap.getMyPlayer().getShips()) {
			averageX+=ship.getPosition().getX();
			averageY+=ship.getPosition().getY();
		}
		averageX/=gameMap.getMyPlayer().getShips().size();
		averageY/=gameMap.getMyPlayer().getShips().size();
		Position averageStart = new Position(averageX, averageY);
		
		double biggestRadius = 0;
		int basePlanetId = -1;
		
		for(Planet planet: gameMap.getPlanets()) {
			if(planet.getPosition().getDistanceSquared(averageStart)>4900) {
				continue;
			}
			if(planet.getRadius()>biggestRadius) {
				biggestRadius = planet.getRadius();
				basePlanetId = planet.getId();
			}
		}
		if(basePlanetId==-1) {
			basePlanetId = getClosestPlanet(averageStart).getId();
		}
		Position basePosition = gameMap.getPlanet(basePlanetId).getPosition();
		closestPlanetIds = new ArrayList<Integer>();
		List<Planet> planets = new ArrayList<Planet>();
		planets.addAll(gameMap.getPlanets());
		Collections.sort(planets, new Comparator<Planet>() {
			@Override
			public int compare(Planet a, Planet b) {
				return Double.compare(a.getPosition().getDistanceSquared(basePosition),
						b.getPosition().getDistanceSquared(basePosition));
			}
		});
		for(Planet planet: planets) {
			closestPlanetIds.add(planet.getId());
		}
		shipToPlanet = new HashMap<Integer, Integer>();
		shipDraft = new ArrayList<Ship>();
	}
	private static List<Ship> shipDraft;
	private static final double FACTOR = 1.2f;
	public void newTurn() {
		shipDraft.clear();
		for(Ship ship: gameMap.getMyPlayer().getShips()) {
			if(ship.getDockingStatus()==DockingStatus.UNDOCKED) { //Only add ships that are undocked
				shipDraft.add(ship);
			}
		}
		//check if any planets exploded
		List<Integer> toRemove = new ArrayList<Integer>(); //Prevents ConcurrentModificationException
		for(int planetId: closestPlanetIds) {
			if(gameMap.getPlanet(planetId)==null) { //Planet exploded :(
				toRemove.add(planetId);
			}
		}
		for(int i: toRemove) {
			closestPlanetIds.remove((Object)i);
		}
		//assign ships; TODO: DEFENSE - detect enemy ships nearby our planets and target
		for(int planetId: closestPlanetIds) {
			Planet planet = gameMap.getPlanet(planetId);
			int spacesLeft = planet.getDockingSpots();
			if(planet.getOwner()==gameMap.getMyPlayerId()) {
				spacesLeft-=planet.getDockedShips().size();
			}
			assignShips(planet, (int)(spacesLeft*FACTOR));
		}
		//assign leftover ships
		if(shipDraft.size()>0) {
			for(int planetId: closestPlanetIds) {
				Planet planet = gameMap.getPlanet(planetId);
				if(planet.getOwner()!=gameMap.getMyPlayerId()) {
					assignShips(planet, shipDraft.size());
					break;
				}
			}
		}
		//We've taken all of the planets! distribute leftover ships
		if(shipDraft.size()>0) {
			int totalShips = shipDraft.size();
			for(int planetId: closestPlanetIds) {
				Planet planet = gameMap.getPlanet(planetId);
				assignShips(planet, (int)Math.ceil(((double)totalShips)/((double)closestPlanetIds.size())));
			}
		}
	}
	public void assignShips(Planet planet, int spaces) {
		int[] draft = new int[spaces];
		double[] distSquared = new double[spaces];
		for(int i=0;i<spaces;++i) {
			draft[i] = -1;
			distSquared[i] = Double.MAX_VALUE;
		}
		int count = 0; // To ensure filling out all unfilled draft spaces before replacing
		for(Ship ship: shipDraft) {
			for(int i=0;i<spaces;++i) {
				double distanceSquared = planet.getPosition().getDistanceSquared(ship.getPosition());
				if(draft[i]==-1) {
					draft[i] = ship.getId();
					distSquared[i] = distanceSquared;
					count++;
					break;
				}
				if(count==spaces&&distSquared[i]>distanceSquared) {
					draft[i] = ship.getId();
					distSquared[i] = distanceSquared;
					break;
				}
			}
		}
		for(int i: draft) {
			if(i!=-1) {
				shipToPlanet.put(i, planet.getId());
				shipDraft.remove(gameMap.getShip(gameMap.getMyPlayerId(), i));
			}
		}
	}
	public int handleShip(List<Integer> handledShips, int shipId, MoveQueue moveQueue){
		Ship ship = gameMap.getShip(gameMap.getMyPlayerId(), shipId);
		
		if(ship.getDockingStatus()!=DockingStatus.UNDOCKED) { //Checks if ship is undocked
			return -1;
		}
		
		Planet currentPlanet = gameMap.getPlanet(shipToPlanet.get(shipId));
		Planet closestPlanet = getClosestPlanet(ship.getPosition());
		
		if(isSafeToDock(ship.getPosition(), closestPlanet)&&ship.canDock(closestPlanet)) {
			DebugLog.log("Docking Ship: "+ship.getId());
			return moveQueue.addMove(new DockMove(ship, closestPlanet));
		}else if(ship.getPosition().getDistanceSquared(currentPlanet.getPosition())<(GameConstants.DOCK_RADIUS+currentPlanet.getRadius())*(GameConstants.DOCK_RADIUS+currentPlanet.getRadius())){
			Ship enemyShip = findEnemyShip(currentPlanet.getPosition());
			ThrustMove move = Pathfinder.pathfind(ship, ship.getPosition(), enemyShip.getPosition(), 2*GameConstants.SHIP_RADIUS+0.01f);
			int request = moveQueue.addMove(move);
			while(request!=-1&&handledShips.contains(request)) {
				move.setThrust(Math.min(move.getThrust()-1, 0));
				request = moveQueue.addMove(move);
			}
			return request;
		}else{
			ThrustMove move = Pathfinder.pathfind(ship, ship.getPosition(), currentPlanet.getPosition(), GameConstants.SHIP_RADIUS+currentPlanet.getRadius()+0.01f);
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
			if(ship.getPosition().getDistanceSquared(planet.getPosition())<(planet.getRadius()+GameConstants.DOCK_RADIUS)*(planet.getRadius()+GameConstants.DOCK_RADIUS)) {
				return false;
			}
		}
		return true;
	}
	public Ship findEnemyShip(Position position) {
		Ship closestShip = null;
		double closestDistance = Double.MAX_VALUE;
		for(Ship ship: gameMap.getShips()) {
			if(ship.getOwner()==gameMap.getMyPlayerId()) {
				continue;
			}
			double distance = position.getDistanceSquared(ship.getPosition());
			if(closestDistance>distance) {
				closestDistance = distance;
				closestShip = ship;
			}
		}
		return closestShip;
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
}
