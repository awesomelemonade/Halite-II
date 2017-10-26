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
		if(gameMap.getPlanet(currentPlanetId).isFull()) {
			Planet closestPlanet = null;
			double closestDistance = Double.MAX_VALUE;
			for(Planet planet: gameMap.getPlanets()) {
				if(planet.isFull()) {
					continue;
				}
				double distance = basePosition.getDistanceSquared(planet.getPosition());
				if(closestDistance>distance) {
					closestDistance = distance;
					closestPlanet = planet;
				}
			}
			currentPlanetId = closestPlanet.getId();
		}
		Planet currentPlanet = gameMap.getPlanet(currentPlanetId);
		
		if(ship.canDock(currentPlanet)) {
			return moveQueue.addMove(new DockMove(ship, currentPlanet));
		} else {
			ThrustMove move = Pathfinder.pathfind(ship, ship.getPosition(), currentPlanet.getPosition(), GameConstants.SHIP_RADIUS, currentPlanet.getRadius());
			int request = moveQueue.addMove(move);
			while(request!=-1&&handledShips.contains(request)) {
				if(move.getThrust()==0) {
					DebugLog.log("Uhh.. loop?");
				}
				move.setThrust(Math.min(move.getThrust()-1, 0));
				request = moveQueue.addMove(move);
			}
			if(move.getThrust()==0) {
				DebugLog.log("Not Moving: "+ship.getId());
			}
			return request;
		}
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
