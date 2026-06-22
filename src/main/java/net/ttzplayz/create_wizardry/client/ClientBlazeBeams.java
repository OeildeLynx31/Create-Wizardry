package net.ttzplayz.create_wizardry.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// beam registry
public final class ClientBlazeBeams {
    private static final Set<Integer> RAY_OF_SIPHONING_PROXIES = Collections.synchronizedSet(new HashSet<>());

    private ClientBlazeBeams() {}

    public static void put(int proxyEntityId) {
        RAY_OF_SIPHONING_PROXIES.add(proxyEntityId);
    }

    public static void remove(int proxyEntityId) {
        RAY_OF_SIPHONING_PROXIES.remove(proxyEntityId);
    }

    public static boolean contains(int proxyEntityId) {
        return RAY_OF_SIPHONING_PROXIES.contains(proxyEntityId);
    }

    public static void clear() {
        RAY_OF_SIPHONING_PROXIES.clear();
    }
}
