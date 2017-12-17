package lemon.halite2.task.projection;

import hlt.Vector;

public class Projection {
	private Vector target;
	private int size;
	private ProjectionItem[] friendlyProjectionItems;
	private ProjectionItem[] enemyProjectionItems;
	public Projection(Vector target, int size){
		this.target = target;
		this.size = size;
		this.friendlyProjectionItems = new ProjectionItem[size];
		this.enemyProjectionItems = new ProjectionItem[size];
		for(int i=0;i<size;++i) {
			friendlyProjectionItems[i] = new ProjectionItem();
			enemyProjectionItems[i] = new ProjectionItem();
		}
	}
	public boolean isSafe(int margin) {
		for(int i=0;i<size;++i) {
			if(enemyProjectionItems[i].getDistanceSquared()==Double.MAX_VALUE) {
				continue;
			}
			if(enemyProjectionItems[i].getDistanceSquared()-margin<friendlyProjectionItems[i].getDistanceSquared()) {
				return false;
			}
		}
		return true;
	}
	public boolean compareFriendlyShip(double distanceSquared, int shipId, Vector position) {
		for(ProjectionItem item: friendlyProjectionItems) {
			if(item.compareShip(distanceSquared, shipId, position)) {
				return true;
			}
		}
		return false;
	}
	public boolean compareEnemyShip(double distanceSquared, int shipId, Vector position) {
		for(ProjectionItem item: enemyProjectionItems) {
			if(item.compareShip(distanceSquared, shipId, position)) {
				return true;
			}
		}
		return false;
	}
	public boolean compareFriendlyPlanet(double distanceSquared, int planetId, Vector position) {
		for(ProjectionItem item: friendlyProjectionItems) {
			if(item.comparePlanet(distanceSquared, planetId, position)) {
				return true;
			}
		}
		return false;
	}
	public boolean compareEnemyPlanet(double distanceSquared, int planetId, Vector position) {
		for(ProjectionItem item: enemyProjectionItems) {
			if(item.comparePlanet(distanceSquared, planetId, position)) {
				return true;
			}
		}
		return false;
	}
	public ProjectionItem getFriendlyProjectionItem(int index) {
		return friendlyProjectionItems[index];
	}
	public ProjectionItem getEnemyProjectionItem(int index) {
		return enemyProjectionItems[index];
	}
	public Vector getTarget(){
		return target;
	}
	public int getSize() {
		return size;
	}
}
