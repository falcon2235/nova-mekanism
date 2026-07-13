package com.falcon2235.moremultiblock.block;

import com.falcon2235.moremultiblock.blockentity.ManaHatchSupport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Mana hatch: a wall block for the Botania multiblocks (accepted anywhere a port is).
 * Put a Botania spark on top and it receives mana from sparked mana pools like any
 * spark network member; the machine drains it to pay its recipes' mana costs.
 *
 * <p>The block entity implements Botania capabilities, so it is only created (and its
 * class only loaded) when Botania is actually present — without Botania the hatch is
 * an inert block.
 */
public class ManaHatchBlock extends Block implements EntityBlock {

    public ManaHatchBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModList.get().isLoaded("botania") ? ManaHatchSupport.create(pos, state) : null;
    }
}
