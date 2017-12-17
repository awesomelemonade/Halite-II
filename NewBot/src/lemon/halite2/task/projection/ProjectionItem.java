package lemon.halite2.task.projection;

import hlt.Vector;

public class ProjectionItem implements Comparable<ProjectionItem> {
	private double distanceSquared;
	private int sourceShipId;
	private int sourcePlanetId;
	private Vector source;
	public ProjectionItem(double distanceSquared, int sourceShipId, int sourcePlanetId, Vector source) {
		this.distanceSquared = distanceSquared;
		this.sourceShipId = sourceShipId;
		this.sourcePlanetId = sourcePlanetId;
		this.source = source;
	}
	public double getDistanceSquared() {
		return distanceSquared;
	}
	public int getSourceShipId() {
		return sourceShipId;
	}
	public int getSourcePlanetId() {
		return sourcePlanetId;
	}
	public Vector getSource() {
		return source;
	}
	@Override
	public String toString() {
		return String.format("ProjectionItem[distSqrd=%s, shipId=%d, planetId=%d, source=%s]", Double.toString(distanceSquared), sourceShipId, sourcePlanetId, source==null?"null":source.toString());
	}
	@Override
	public int compareTo(ProjectionItem item) {
		int compare = Double.compare(distanceSquared, item.getDistanceSquared());
		if(compare==0) {
			return Integer.compare(this.hashCode(), item.hashCode());
		}
		return compare;
	}
}
