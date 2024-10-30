
package com.github.tno.pokayoke.transform.common;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class BiMapUtils {
    private BiMapUtils() {
    }

    /**
     * Inverts a given bidirectional mapping while preserving the original iteration order.
     *
     * @param <K> The type of keys.
     * @param <V> The type of values.
     * @param map The map to invert.
     * @return The inverse map with the iteration order preserved.
     */
    public static <K, V> BiMap<V, K> orderPreservingInverse(BiMap<K, V> map) {
        BiMap<V, K> inverseMap = HashBiMap.create(map.size());
        map.keySet().forEach(key -> inverseMap.put(map.get(key), key));
        return inverseMap;
    }
}
