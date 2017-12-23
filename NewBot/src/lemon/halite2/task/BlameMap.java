package lemon.halite2.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BlameMap {
	private Map<Integer, Integer> order;
	private Map<Integer, Set<Integer>> blameMap;
	public BlameMap() {
		order = new LinkedHashMap<Integer, Integer>();
		blameMap = new HashMap<Integer, Set<Integer>>();
	}
	public void add(int blameId, int shipId) {
		if(!blameMap.containsKey(blameId)) {
			blameMap.put(blameId, new HashSet<Integer>());
		}
		blameMap.get(blameId).add(shipId);
		order.put(shipId, blameId);
	}
	public int getFirst() {
		return order.get(order.keySet().iterator().next());
	}
	public void clear(int blameId) {
		for(int shipId: blameMap.get(blameId)) {
			order.remove(shipId);
		}
		blameMap.remove(blameId);
	}
	public boolean containsKey(int blameId) {
		return blameMap.containsKey(blameId);
	}
	public Set<Integer> get(int blameId){
		return blameMap.get(blameId);
	}
	public boolean isEmpty() {
		return order.isEmpty();
	}
}
