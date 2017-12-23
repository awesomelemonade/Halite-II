package lemon.halite2.task.projection;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.function.BiPredicate;

import hlt.Vector;

public class Projection {
	private Vector target;
	private int size;
	private TreeSet<ProjectionItem> friendlyProjectionItems; //Cannot use "Set" because we need "set.last()"
	private TreeSet<ProjectionItem> enemyProjectionItems;
	public Projection(Vector target, int size){
		this.target = target;
		this.size = size;
		friendlyProjectionItems = new TreeSet<ProjectionItem>();
		enemyProjectionItems = new TreeSet<ProjectionItem>();
	}
	public boolean isSafe(BiPredicate<Double, Double> comparator) {
		Iterator<ProjectionItem> friendlyIterator = friendlyProjectionItems.iterator();
		Iterator<ProjectionItem> enemyIterator = enemyProjectionItems.iterator();
		while(friendlyIterator.hasNext()&&enemyIterator.hasNext()) {
			ProjectionItem friendly = friendlyIterator.next();
			ProjectionItem enemy = enemyIterator.next();
			if(comparator.test(enemy.getDistanceSquared(), friendly.getDistanceSquared())) {
				return false;
			}
		}
		return !enemyIterator.hasNext();
	}
	private boolean add(TreeSet<ProjectionItem> items, ProjectionItem item) {
		if(items.size()>=size&&item.compareTo(items.last())>0) {
			return false;
		}
		items.add(item);
		if(items.size()>size) {
			items.remove(items.last());
		}
		return true;
	}
	public boolean compareFriendlyShip(double distanceSquared, int shipId, Vector position) {
		return add(friendlyProjectionItems, new ProjectionItem(distanceSquared, shipId, -1, position));
	}
	public boolean compareEnemyShip(double distanceSquared, int shipId, Vector position) {
		return add(enemyProjectionItems, new ProjectionItem(distanceSquared, shipId, -1, position));
	}
	public boolean compareFriendlyPlanet(double distanceSquared, int planetId, Vector position) {
		return add(friendlyProjectionItems, new ProjectionItem(distanceSquared, -1, planetId, position));
	}
	public boolean compareEnemyPlanet(double distanceSquared, int planetId, Vector position) {
		return add(enemyProjectionItems, new ProjectionItem(distanceSquared, -1, planetId, position));
	}
	public Vector getTarget(){
		return target;
	}
	public int getSize() {
		return size;
	}
	public TreeSet<ProjectionItem> getFriendlyProjectionItems(){
		return friendlyProjectionItems;
	}
	public TreeSet<ProjectionItem> getEnemyProjectionItems(){
		return enemyProjectionItems;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("Projection - Target=%s\n", target.toString()));
		Iterator<ProjectionItem> friendlyIterator = friendlyProjectionItems.iterator();
		Iterator<ProjectionItem> enemyIterator = enemyProjectionItems.iterator();
		for(int i=0;i<size;++i) {
			builder.append(String.format("\tFriendly=%s - Enemy=%s",
					friendlyIterator.hasNext()?friendlyIterator.next().toString():"null",
					enemyIterator.hasNext()?enemyIterator.next().toString():"null"));
			if(i!=size-1) {
				builder.append('\n');
			}
		}
		return builder.toString();
	}
}
