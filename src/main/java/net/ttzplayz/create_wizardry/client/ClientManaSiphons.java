package net.ttzplayz.create_wizardry.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// client registry of loaded mana siphon positions, so the orb render event knows what to draw
// kept free of client-only imports so the common block-entity class can reference it
public final class ClientManaSiphons {
    private static final Set<BlockPos> POSITIONS = Collections.synchronizedSet(new HashSet<>());

    private ClientManaSiphons() {}

    public static void add(BlockPos pos) {
        POSITIONS.add(pos.immutable());
    }

    public static void remove(BlockPos pos) {
        POSITIONS.remove(pos);
    }

    public static List<BlockPos> snapshot() {
        synchronized (POSITIONS) {
            return new ArrayList<>(POSITIONS);
        }
    }

    public static void clear() {
        POSITIONS.clear();
    }
}
