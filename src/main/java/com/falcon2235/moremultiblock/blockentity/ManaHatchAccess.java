package com.falcon2235.moremultiblock.blockentity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Botania-touching half of the mana-hatch indirection: this class's verification
 * loads {@link ManaHatchBlockEntity} (and therefore Botania API classes), so it must
 * only ever be classloaded when Botania is present — which {@link ManaHatchSupport}
 * guarantees by design.
 */
final class ManaHatchAccess {

    private ManaHatchAccess() {
    }

    static BlockEntity create(BlockPos pos, BlockState state) {
        return new ManaHatchBlockEntity(pos, state);
    }

    static long available(Level level, List<BlockPos> positions) {
        long total = 0;
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof ManaHatchBlockEntity hatch) {
                total += hatch.getMana();
            }
        }
        return total;
    }

    static void drain(Level level, List<BlockPos> positions, int amount) {
        int remaining = amount;
        for (BlockPos pos : positions) {
            if (remaining <= 0) {
                return;
            }
            if (level.getBlockEntity(pos) instanceof ManaHatchBlockEntity hatch) {
                remaining -= hatch.drain(remaining);
            }
        }
    }
}
