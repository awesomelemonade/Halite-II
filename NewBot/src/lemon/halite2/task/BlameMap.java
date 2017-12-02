package lemon.halite2.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlameMap {
	private Map<Integer, Set<Integer>> blameMap;
	public BlameMap() {
		blameMap = new HashMap<Integer, Set<Integer>>();
	}
	public void add(int key, int value) {
		if(!blameMap.containsKey(key)) {
			blameMap.put(key, new HashSet<Integer>());
		}
		blameMap.get(key).add(value);
	}
	public void clear(int key) {
		blameMap.remove(key);
	}
	public boolean containsKey(int key) {
		return blameMap.containsKey(key);
	}
	public Set<Integer> get(int key){
		return blameMap.get(key);
	}
	public Set<Integer> keySet(){
		return blameMap.keySet();
	}
	public boolean isEmpty() {
		return blameMap.isEmpty();
	}
}
