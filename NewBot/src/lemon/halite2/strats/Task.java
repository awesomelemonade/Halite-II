package lemon.halite2.strats;

public interface Task {
	public void execute(int shipId);
	public double getScore(int shipId);
}
