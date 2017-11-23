package lemon.halite2.util;

import java.util.HashMap;
import java.util.Map;

public class BiMap<K, V> {
	private Map<K, V> map;
	private Map<V, K> map2;
	public BiMap() {
		map = new HashMap<K, V>();
		map2 = new HashMap<V, K>();
	}
	public void put(K key, V value) {
		map.put(key, value);
		map2.put(value, key);
	}
	public K getKey(V value) {
		return map2.get(value);
	}
	public V getValue(K key) {
		return map.get(key);
	}
}
