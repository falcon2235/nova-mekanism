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
        return ManaHatchAccess.create(pos, state);
    }

    /** Total mana buffered in the hatches among the given structure positions. */
    public static long available(Level level, List<BlockPos> positions) {
        return ManaHatchAccess.available(level, positions);
    }

    /** Drains the given mana amount across the structure's hatches (assumes availability). */
    public static void drain(Level level, List<BlockPos> positions, int amount) {
        ManaHatchAccess.drain(level, positions, amount);
    }
}
