package lemon.halite2.strats;

import java.util.List;

import hlt.DebugLog;
import hlt.DockMove;
import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Position;
import hlt.Ship;
import hlt.ThrustMove;
import lemon.halite2.util.MoveQueue;
import lemon.halite2.util.Pathfinder;

public class BasicStrategy {
	private GameMap gameMap;
	private Position basePosition;
	private int currentPlanetId;
	public BasicStrategy(GameMap gameMap){
		this.gameMap = gameMap;
		this.currentPlanetId = -1;
		init();
	}
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
		
		for(Planet planet: gameMap.getPlanets()) {
			if(planet.getPosition().getDistanceSquared(averageStart)>4900) {
				continue;
			}
			if(planet.getRadius()>biggestRadius) {
				biggestRadius = planet.getRadius();
				currentPlanetId = planet.getId();
			}
		}
		if(currentPlanetId==-1) {
			currentPlanetId = getClosestPlanet(averageStart).getId();
		}
		basePosition = gameMap.getPlanet(currentPlanetId).getPosition();
	}
	public int handleShip(List<Integer> handledShips, int shipId, MoveQueue moveQueue){
		Ship ship = gameMap.getShip(gameMap.getMyPlayerId(), shipId);
		Planet currentPlanet = gameMap.getPlanet(currentPlanetId);
		if(currentPlanet==null||currentPlanet.getOwner()==gameMap.getMyPlayerId()&&currentPlanet.isFull()) {
			Planet closestPlanet = null;
			double closestDistance = Double.MAX_VALUE;
			for(Planet planet: gameMap.getPlanets()) {
				if(planet.getOwner()==gameMap.getMyPlayerId()&&planet.isFull()) {
					continue;
				}
				double distance = basePosition.getDistanceSquared(planet.getPosition());
				if(closestDistance>distance) {
					closestDistance = distance;
					closestPlanet = planet;
				}
			}
			currentPlanetId = closestPlanet.getId();
			currentPlanet = gameMap.getPlanet(currentPlanetId);
		}
		Planet closestPlanet = getClosestPlanet(ship.getPosition());
		
		if(ship.canDock(closestPlanet)) {
			DebugLog.log("Docking Ship: "+ship.getId());
			return moveQueue.addMove(new DockMove(ship, closestPlanet));
		}else if(ship.getPosition().getDistanceSquared(currentPlanet.getPosition())<GameConstants.DOCK_RADIUS*GameConstants.DOCK_RADIUS){
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
