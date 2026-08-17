package com.falcon2235.moremultiblock.blockentity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Verifier-safe trampoline between always-loaded code and the Botania-typed
 * {@link ManaHatchBlockEntity}. This class deliberately mentions NO mana-hatch
 * types anywhere (signatures, casts or instanceof), because the JVM verifier
 * eagerly loads types that appear in stack frames — which would explode without
 * Botania installed. All bodies bounce to {@link ManaHatchAccess}, which is only
 * classloaded when one of these methods actually RUNS (all call sites are gated
 * on Botania being loaded).
 */
public final class ManaHatchSupport {

    private ManaHatchSupport() {
    }

    public static BlockEntity create(BlockPos pos, BlockState state) {
        // Botania is an optional dependency: a world that used mana hatches and then
        // dropped Botania would otherwise try to build the Botania-typed block entity
        // on load and blow up in the verifier.
        if (!net.minecraftforge.fml.ModList.get().isLoaded("botania")) {
            return null;
        }
        return ManaHatchAccess.create(pos, state);
    }

    /** Whether the Botania-backed mana hatch can operate at all in this instance. */
    public static boolean available() {
        return net.minecraftforge.fml.ModList.get().isLoaded("botania");
    }

    /** Total mana buffered in the hatches among the given structure positions. */
    public static long available(Level level, List<BlockPos> positions) {
        return available() ? ManaHatchAccess.available(level, positions) : 0L;
    }

    /** Drains the given mana amount across the structure's hatches (assumes availability). */
    public static void drain(Level level, List<BlockPos> positions, int amount) {
        if (available()) {
            ManaHatchAccess.drain(level, positions, amount);
        }
    }
}
