package com.falcon2235.moremultiblock.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * EBF heating coil. The blast furnace lights up its coil rings (LIT) while it is
 * actively processing; lit coils use a glowing texture and emit light.
 */
public class CoilBlock extends Block {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CoilBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? 10 : 0));
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}
