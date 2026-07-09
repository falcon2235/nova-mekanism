package com.falcon2235.moremultiblock.multiblock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Tracks which controller currently owns each parallel processor unit, so a unit
 * can only boost a single multiblock at a time. Everything else (casing, ports,
 * even shared walls) may be reused by overlapping structures — parallel units are
 * the sole exclusive component.
 *
 * <p>Each claim carries an expiry tick and is renewed on every controller
 * revalidation, so a controller that is broken or unloaded releases its units
 * automatically once the lease lapses. All access happens on the server thread.
 */
public final class ParallelClaimRegistry {

    private record Claim(BlockPos controller, long expiry) {
    }

    private static final Map<ResourceKey<Level>, Map<BlockPos, Claim>> CLAIMS = new HashMap<>();

    private ParallelClaimRegistry() {
    }

    public static boolean tryClaim(Level level, BlockPos unit, BlockPos controller, long now, long ttl) {
        Map<BlockPos, Claim> map = CLAIMS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Claim current = map.get(unit);
        if (current != null && current.expiry() > now && !current.controller().equals(controller)) {
            return false;
        }
        map.put(unit, new Claim(controller.immutable(), now + ttl));
        return true;
    }

    public static void release(Level level, BlockPos controller) {
        Map<BlockPos, Claim> map = CLAIMS.get(level.dimension());
        if (map == null) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Claim>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().controller().equals(controller)) {
                it.remove();
            }
        }
    }

    public static void clear() {
        CLAIMS.clear();
    }
}
