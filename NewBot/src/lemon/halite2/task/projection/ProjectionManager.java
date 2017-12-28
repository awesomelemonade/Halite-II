package lemon.halite2.task.projection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Ship;
import hlt.Vector;
import hlt.Ship.DockingStatus;
import lemon.halite2.task.TaskManager;

public enum ProjectionManager {
	INSTANCE(GameMap.INSTANCE); // Singleton
	private GameMap gameMap;
	private Map<Integer, Vector> spawnPositions;
	private Map<Integer, Integer> spawnCount;
	private List<Integer> ships;
	private List<Integer> undockingShips;
	private ProjectionManager(GameMap gameMap) {
		this.gameMap = gameMap;
		this.spawnPositions = new HashMap<Integer, Vector>();
		this.spawnCount = new HashMap<Integer, Integer>();
		this.ships = new ArrayList<Integer>();
		this.undockingShips = new ArrayList<Integer>();
	}
	public void init(){
		for(Planet planet: gameMap.getPlanets()){
			Vector projectedSpawn = planet.getPosition().addPolar(planet.getRadius()+GameConstants.SPAWN_RADIUS,
					planet.getPosition().getDirectionTowards(gameMap.getCenterPosition()));
			spawnPositions.put(planet.getId(), projectedSpawn);
		}
	}
	public void update() {
		ships.clear();
		spawnCount.clear();
		undockingShips.clear();
		for(Ship ship: gameMap.getMyPlayer().getShips()){
			ships.add(ship.getId());
		}
	}
	public void addUndockingShip(int shipId) {
		undockingShips.add(shipId);
	}
	public Projection calculate(Vector target, int size, Predicate<Ship> shipExceptions) {
		Projection projection = new Projection(target, size);
		// Friendly Ships
		for(int shipId: ships){
			Ship ship = gameMap.getMyPlayer().getShip(shipId);
			if(shipExceptions.test(ship)) {
				continue;
			}
			if(ship.getDockingStatus()==DockingStatus.UNDOCKED) {
				double distanceSquared = target.getDistanceSquared(ship.getPosition());
				projection.compareFriendlyShip(distanceSquared, ship.getId(), ship.getPosition());
			}
			if(ship.getDockingStatus()==DockingStatus.UNDOCKING) {
				double distance = ship.getDockingProgress()*GameConstants.MAX_SPEED+
						ship.getPosition().getDistanceTo(target);
				projection.compareFriendlyShip(distance*distance, ship.getId(), ship.getPosition());
			}
			if(ship.getDockingStatus()==DockingStatus.DOCKED) {
				if(undockingShips.contains(ship.getId())) {
					double distance = GameConstants.UNDOCK_TURNS*GameConstants.MAX_SPEED+
							ship.getPosition().getDistanceTo(target);
					projection.compareFriendlyShip(distance*distance, ship.getId(), ship.getPosition());
				}
			}
		}
		// Enemy Ships
		for(Ship ship: gameMap.getShips()) {
			if(ship.getOwner()==gameMap.getMyPlayerId()) {
				continue;
			}
			if(ship.getDockingStatus()==DockingStatus.UNDOCKED){
				double distanceSquared = target.getDistanceSquared(ship.getPosition());
				projection.compareEnemyShip(distanceSquared, ship.getId(), ship.getPosition());
			}
			if(ship.getDockingStatus()==DockingStatus.UNDOCKING) {
				double distance = ship.getDockingProgress()*GameConstants.MAX_SPEED+
						ship.getPosition().getDistanceTo(target);
				projection.compareEnemyShip(distance*distance, ship.getId(), ship.getPosition());
			}
			if(ship.getDockingStatus()==DockingStatus.DOCKED) {
				double distance = GameConstants.UNDOCK_TURNS*GameConstants.MAX_SPEED+
						ship.getPosition().getDistanceTo(target);
				projection.compareEnemyShip(distance*distance, ship.getId(), ship.getPosition());
			}
		}
		//Project undocking of accepted ships
		for(Planet planet: gameMap.getPlanets()) {
			List<Integer> acceptedShips = TaskManager.INSTANCE.getDockTask(planet.getId()).getAcceptedShips();
			for(int shipId: acceptedShips) {
				Ship ship = GameMap.INSTANCE.getMyPlayer().getShip(shipId);
				Vector projectedLanding = planet.getPosition().addPolar(
						Math.min(planet.getRadius()+GameConstants.DOCK_RADIUS, ship.getPosition().getDistanceTo(planet.getPosition())),
						planet.getPosition().getDirectionTowards(ship.getPosition()));
				double distance = GameConstants.MAX_SPEED*(GameConstants.DOCK_TURNS+GameConstants.UNDOCK_TURNS)+
						projectedLanding.getDistanceTo(target);
				projection.compareFriendlyShip(distance*distance, shipId, projectedLanding);
			}
		}
		// Project ships that would be created in the future
		for(Planet planet: gameMap.getPlanets()){
			// Calculate number of turns it would take to create a new ship
			int remainingProduction = (spawnCount.getOrDefault(planet.getId(), 0)+1)*
					GameConstants.TOTAL_PRODUCTION-planet.getCurrentProduction();
			List<Integer> acceptedShips = TaskManager.INSTANCE.getDockTask(planet.getId()).getAcceptedShips();
			List<Integer> dockedProgress = new ArrayList<Integer>();
			int turns = 0;
			for(int i=0;i<planet.getDockedShips().size();++i) {
				Ship s = gameMap.getShip(planet.getOwner(), planet.getDockedShips().get(i));
				if(s.getDockingStatus()==DockingStatus.DOCKED) {
					if(!undockingShips.contains(s.getId())) {
						dockedProgress.add(0);
					}
				}else if(s.getDockingStatus()==DockingStatus.DOCKING) {
					dockedProgress.add(s.getDockingProgress());
				}
			}
			for(int i=0;i<acceptedShips.size();++i) {
				Ship s = gameMap.getMyPlayer().getShip(acceptedShips.get(i));
				dockedProgress.add(GameConstants.DOCK_TURNS+
						(int)Math.ceil(((double)Math.max(s.getPosition().getDistanceTo(planet.getPosition())-planet.getRadius()-GameConstants.DOCK_RADIUS, 0))/GameConstants.MAX_SPEED));
			}
			if(dockedProgress.isEmpty()) {
				continue;
			}
			while(remainingProduction>0) {
				while(remainingProduction>0) {
					for(int i=0;i<dockedProgress.size();++i) {
						if(dockedProgress.get(i)>0) {
							dockedProgress.set(i, dockedProgress.get(i)-1);
						}else {
							remainingProduction-=GameConstants.BASE_PRODUCTION;
						}
					}
					turns++;
				}
				Vector projectedSpawn = spawnPositions.get(planet.getId());
				double distanceSquared = target.getDistanceTo(projectedSpawn)+turns*GameConstants.MAX_SPEED;
				distanceSquared = distanceSquared*distanceSquared;
				if((!planet.isOwned())||planet.getOwner()==gameMap.getMyPlayerId()){
					if(projection.compareFriendlyPlanet(distanceSquared, planet.getId(), projectedSpawn)) {
						remainingProduction+=GameConstants.TOTAL_PRODUCTION;
					}
				}else{
					if(projection.compareEnemyPlanet(distanceSquared, planet.getId(), projectedSpawn)){
						remainingProduction+=GameConstants.TOTAL_PRODUCTION;
					}
				}
			}
		}
		return projection;
	}
}
