package net.ttzplayz.create_wizardry.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Client-side registry of Blaze Caster proxy entity ids that are currently channeling Ray of
 * Siphoning, so {@link CWClientRenderEvents} knows which (invisible) ArmorStand proxies should
 * draw the beam. Populated from the synced channel state in
 * {@code BlazeCasterBlockEntity#read}. Mirrors the static client-cache pattern ISS uses for
 * {@code ClientMagicData}. Kept free of client-only imports so it is safe to reference from the
 * common block-entity class.
 */
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
