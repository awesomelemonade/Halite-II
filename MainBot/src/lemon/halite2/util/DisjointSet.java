package lemon.halite2.util;

import java.util.HashMap;
import java.util.Map;

public class DisjointSet<T> {
	private Map<T, T> parents;
	private Map<T, Integer> ranks;
	public DisjointSet() {
		parents = new HashMap<T, T>();
		ranks = new HashMap<T, Integer>();
	}
	public int getRank(T object) {
		if(ranks.containsKey(object)) {
			return ranks.get(object);
		}else {
			return 0;
		}
	}
	public T getParent(T object) {
		if(parents.containsKey(object)) {
			return parents.get(object);
		}else {
			return object;
		}
	}
	public T find(T object) {
		if(!object.equals(getParent(object))) {
			parents.put(object, find(getParent(object)));
		}
		return getParent(object);
	}
	public void union(T object, T object2) {
		T root = find(object);
		T root2 = find(object2);
		if(getRank(root)<getRank(root2)) {
			parents.put(root, root2);
		}else if(getRank(root)>getRank(root2)) {
			parents.put(root2, root);
		}else {
			parents.put(root2, root);
			ranks.put(root, getRank(root)+1);
		}
	}
}
