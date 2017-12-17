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
	private ProjectionManager(GameMap gameMap) {
		this.gameMap = gameMap;
		this.spawnPositions = new HashMap<Integer, Vector>();
		this.spawnCount = new HashMap<Integer, Integer>();
		this.ships = new ArrayList<Integer>();
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
		for(Ship ship: gameMap.getMyPlayer().getShips()){
			ships.add(ship.getId());
		}
	}
	public void reserveProjection(Projection projection){
		for(int i=0;i<projection.getSize();++i) {
			if(projection.getEnemyProjectionItem(i).getDistanceSquared()!=Double.MAX_VALUE) {
				ProjectionItem item = projection.getFriendlyProjectionItem(i);
				if(item.getSourceShipId()!=-1) {
					ships.remove((Object)item.getSourceShipId());
				}
				if(item.getSourcePlanetId()!=-1) {
					spawnCount.put(item.getSourcePlanetId(), spawnCount.getOrDefault(item.getSourcePlanetId(), 0)+1);
				}
			}
		}
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
		}
		// Project ships that would be created in the future
		for(Planet planet: gameMap.getPlanets()){
			if(!planet.isOwned()){
				continue;
			}
			// Calculate number of turns it would take to create a new ship
			int remainingProduction = (spawnCount.getOrDefault(planet.getId(), 0)+1)*
					GameConstants.TOTAL_PRODUCTION-planet.getCurrentProduction();
			List<Integer> acceptedShips = TaskManager.INSTANCE.getDockTask(planet.getId()).getAcceptedShips();
			int[] dockedProgress = new int[planet.getDockedShips().size()+acceptedShips.size()];
			int turns = 0;
			for(int i=0;i<planet.getDockedShips().size();++i) {
				Ship s = gameMap.getShip(planet.getOwner(), planet.getDockedShips().get(i));
				if(s.getDockingStatus()==DockingStatus.DOCKED) {
					dockedProgress[i] = 0;
				}else if(s.getDockingStatus()==DockingStatus.DOCKING) {
					dockedProgress[i] = s.getDockingProgress();
				}
			}
			for(int i=0;i<acceptedShips.size();++i) {
				Ship s = gameMap.getMyPlayer().getShip(acceptedShips.get(i));
				dockedProgress[planet.getDockedShips().size()+i] = GameConstants.DOCK_TURNS+
						(int)Math.ceil(((double)Math.max(s.getPosition().getDistanceTo(planet.getPosition())-planet.getRadius()-GameConstants.DOCK_RADIUS, 0))/7.0);
			}
			while(remainingProduction>0) {
				while(remainingProduction>0) {
					for(int i=0;i<dockedProgress.length;++i) {
						if(dockedProgress[i]>0) {
							dockedProgress[i]--;
						}else {
							remainingProduction-=GameConstants.BASE_PRODUCTION;
						}
					}
					turns++;
				}
				Vector projectedSpawn = planet.getPosition().addPolar(planet.getRadius()+GameConstants.SPAWN_RADIUS,
						planet.getPosition().getDirectionTowards(gameMap.getCenterPosition()));
				double distanceSquared = target.getDistanceTo(projectedSpawn)+turns*GameConstants.MAX_SPEED;
				distanceSquared = distanceSquared*distanceSquared;
				if(planet.getOwner()==gameMap.getMyPlayerId()){
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
