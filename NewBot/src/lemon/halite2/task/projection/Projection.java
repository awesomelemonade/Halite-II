package lemon.halite2.task.projection;

import hlt.Vector;

public class Projection {
	private Vector target;
	private double closestFriendlyDistanceSquared;
	private double closestEnemyDistanceSquared;
	private int friendlySourceShipId;
	private int enemySourceShipId;
	private int friendlySourcePlanetId;
	private int enemySourcePlanetId;
	private Vector friendlySource;
	private Vector enemySource;
	public Projection(Vector target){
		this.target = target;
		this.closestFriendlyDistanceSquared = Double.MAX_VALUE;
		this.closestEnemyDistanceSquared = Double.MAX_VALUE;
		this.friendlySourceShipId = -1;
		this.enemySourceShipId = -1;
		this.friendlySourcePlanetId = -1;
		this.enemySourcePlanetId = -1;
		this.friendlySource = null;
		this.enemySource = null;
	}
	public void compareFriendlyShip(double distanceSquared, int shipId, Vector position) {
		if(distanceSquared<closestFriendlyDistanceSquared) {
			closestFriendlyDistanceSquared = distanceSquared;
			friendlySourceShipId = shipId;
			friendlySourcePlanetId = -1;
			friendlySource = position;
		}
	}
	public void compareEnemyShip(double distanceSquared, int shipId, Vector position) {
		if(distanceSquared<closestEnemyDistanceSquared) {
			closestEnemyDistanceSquared = distanceSquared;
			enemySourceShipId = shipId;
			enemySourcePlanetId = -1;
			enemySource = position;
		}
	}
	public void compareFriendlyPlanet(double distanceSquared, int planetId, Vector position) {
		if(distanceSquared<closestFriendlyDistanceSquared) {
			closestFriendlyDistanceSquared = distanceSquared;
			friendlySourceShipId = -1;
			friendlySourcePlanetId = planetId;
			friendlySource = position;
		}
	}
	public void compareEnemyPlanet(double distanceSquared, int planetId, Vector position) {
		if(distanceSquared<closestEnemyDistanceSquared) {
			closestEnemyDistanceSquared = distanceSquared;
			enemySourceShipId = -1;
			enemySourcePlanetId = planetId;
			enemySource = position;
		}
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
	public int getFriendlySourceShipId(){
		return friendlySourceShipId;
	}
	public int getEnemySourceShipId(){
		return enemySourceShipId;
	}
	public int getFriendlySourcePlanetId() {
		return friendlySourcePlanetId;
	}
	public int getEnemySourcePlanetId() {
		return enemySourcePlanetId;
	}
}
