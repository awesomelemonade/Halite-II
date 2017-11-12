package lemon.halite2.benchmark;

import java.util.ArrayDeque;
import java.util.Deque;

public class Benchmark {
	private Deque<Long> deque;
	public Benchmark() {
		deque = new ArrayDeque<Long>();
	}
	public void push() {
		deque.push(System.nanoTime());
	}
	public long peek() {
		return System.nanoTime()-deque.peek();
	}
	public long pop() {
		return System.nanoTime()-deque.pop();
	}
	public static String format(long elapsedTime) {
		return Double.toString(((double)elapsedTime)/1000000000.0);
	}
}
