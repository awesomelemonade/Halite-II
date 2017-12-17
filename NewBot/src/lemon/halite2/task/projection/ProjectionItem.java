package lemon.halite2.task.projection;

import hlt.Vector;

public class ProjectionItem {
	private double distanceSquared;
	private int sourceShipId;
	private int sourcePlanetId;
	private Vector source;
	public ProjectionItem() {
		this.distanceSquared = Double.MAX_VALUE;
		this.sourceShipId = -1;
		this.sourcePlanetId = -1;
		this.source = null;
	}
	public boolean compareShip(double distanceSquared, int shipId, Vector position) {
		if(distanceSquared<this.distanceSquared) {
			this.distanceSquared = distanceSquared;
			this.sourceShipId = shipId;
			this.sourcePlanetId = -1;
			this.source = position;
			return true;
		}else {
			return false;
		}
	}
	public boolean comparePlanet(double distanceSquared, int planetId, Vector position) {
		if(distanceSquared<this.distanceSquared) {
			this.distanceSquared = distanceSquared;
			this.sourceShipId = -1;
			this.sourcePlanetId = planetId;
			this.source = position;
			return true;
		}else {
			return false;
		}
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
}
