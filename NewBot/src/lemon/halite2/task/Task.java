package lemon.halite2.task;

public interface Task {
	public void accept(int shipId);
	public double getScore(int shipId);
}
