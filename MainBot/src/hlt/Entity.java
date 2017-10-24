package hlt;

public class Entity {
	private int owner;
	private int id;
	private Position position;
	private int health;
	private double radius;

	public Entity(int owner, int id, Position position, int health, double radius) {
		this.owner = owner;
		this.id = id;
		this.position = position;
		this.health = health;
		this.radius = radius;
	}
	public int getOwner() {
		return owner;
	}
	public int getId() {
		return id;
	}
	public Position getPosition() {
		return position;
	}
	public int getHealth() {
		return health;
	}
	public double getRadius() {
		return radius;
	}
	@Override
	public String toString() {
		return "Entity[" + super.toString() + ", owner=" + owner + ", id=" + id + ", health=" + health + ", radius="
				+ radius + "]";
	}
}
