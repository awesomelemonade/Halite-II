package lemon.halite2.task.projection;

import java.util.List;
import java.util.function.Predicate;

import hlt.GameConstants;
import hlt.GameMap;
import hlt.Planet;
import hlt.Ship;
import hlt.Vector;
import hlt.Ship.DockingStatus;
import lemon.halite2.task.TaskManager;

public class Projection {
	private Vector target;
	private double closestFriendlyDistanceSquared;
	private double closestEnemyDistanceSquared;
	private Vector friendlySource;
	private Vector enemySource;
	public Projection(Vector target){
		this.target = target;
		this.closestFriendlyDistanceSquared = Double.MAX_VALUE;
		this.closestEnemyDistanceSquared = Double.MAX_VALUE;
		this.friendlySource = null;
		this.enemySource = null;
	}
	public Vector getTarget(){
		return target;
	}
	public double getClosestFriendlyDistanceSquared(){
		return closestFriendlyDistanceSquared;
	}
	public double getClosestEnemyDistanceSquared(){
		return closestEnemyDistanceSquared;
	}
	public Vector getFriendlySource(){
		return friendlySource;
	}
	public Vector getEnemySource(){
		return enemySource;
	}
	public void calculate(Predicate<Ship> shipExceptions){
		for(Ship s: GameMap.INSTANCE.getShips()) {
			if(shipExceptions.test(s)){
				continue;
			}
			if(s.getOwner()==GameMap.INSTANCE.getMyPlayerId()) {
				if(s.getDockingStatus()==DockingStatus.UNDOCKED) {
					double distanceSquared = target.getDistanceSquared(s.getPosition());
					if(distanceSquared<closestFriendlyDistanceSquared) {
						closestFriendlyDistanceSquared = distanceSquared;
						friendlySource = s.getPosition();
					}
				}
			}else {
				if(s.getDockingStatus()==DockingStatus.UNDOCKED){
					double distanceSquared = target.getDistanceSquared(s.getPosition());
					if(distanceSquared<closestEnemyDistanceSquared) {
						closestEnemyDistanceSquared = distanceSquared;
						enemySource = s.getPosition();
					}
				}else{
					double distanceSquared = target.getDistanceTo(s.getPosition())+
							GameConstants.UNDOCK_TURNS*GameConstants.MAX_SPEED;
					distanceSquared = distanceSquared*distanceSquared;
					if(distanceSquared<closestEnemyDistanceSquared) {
						closestEnemyDistanceSquared = distanceSquared;
						enemySource = s.getPosition();
					}
				}
			}
		}
		//Project ships that would be created in the future
		for(Planet planet: GameMap.INSTANCE.getPlanets()){
			if(!planet.isOwned()){
				continue;
			}
			//calculate number of turns it would take to create a new ship
			int remainingProduction = GameConstants.TOTAL_PRODUCTION-planet.getCurrentProduction();
			List<Integer> acceptedShips = TaskManager.INSTANCE.getDockTask(planet.getId()).getAcceptedShips();
			int[] dockedProgress = new int[planet.getDockedShips().size()+acceptedShips.size()];
			int turns = 0;
			for(int i=0;i<planet.getDockedShips().size();++i) {
				Ship s = GameMap.INSTANCE.getShip(planet.getOwner(), planet.getDockedShips().get(i));
				if(s.getDockingStatus()==DockingStatus.DOCKED) {
					dockedProgress[i] = 0;
				}else if(s.getDockingStatus()==DockingStatus.DOCKING) {
					dockedProgress[i] = s.getDockingProgress();
				}
			}
			for(int i=0;i<acceptedShips.size();++i) {
				Ship s = GameMap.INSTANCE.getMyPlayer().getShip(acceptedShips.get(i));
				dockedProgress[planet.getDockedShips().size()+i] = GameConstants.DOCK_TURNS+
						(int)Math.ceil(((double)Math.max(s.getPosition().getDistanceTo(planet.getPosition())-planet.getRadius()-GameConstants.DOCK_RADIUS, 0))/7.0);
			}
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
			Vector projection = planet.getPosition().addPolar(planet.getRadius()+GameConstants.SPAWN_RADIUS,
					planet.getPosition().getDirectionTowards(GameMap.INSTANCE.getCenterPosition()));
			double distanceSquared = target.getDistanceTo(projection)+turns*GameConstants.MAX_SPEED;
			Vector source = target.addPolar(distanceSquared, target.getDirectionTowards(projection)); //Fake a source
			distanceSquared = distanceSquared*distanceSquared;
			if(planet.getOwner()==GameMap.INSTANCE.getMyPlayerId()){
				if(distanceSquared<closestFriendlyDistanceSquared){
					closestFriendlyDistanceSquared = distanceSquared;
					friendlySource = source;
				}
			}else{
				if(distanceSquared<closestEnemyDistanceSquared){
					closestEnemyDistanceSquared = distanceSquared;
					enemySource = source;
				}
			}
		}
	}
}
