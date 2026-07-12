package com.falcon2235.moremultiblock.block;

import com.falcon2235.moremultiblock.MekanismMoreMultiblock;
import com.falcon2235.moremultiblock.blockentity.ConduitBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

/**
 * A quantum conduit: the tier above Mekanism's ultimate transmitters. Forms simple
 * six-way pipe networks per {@link Type}; anything pushed into any segment is
 * instantly routed to every connected consumer, and faces toggled to "extract" mode
 * (right-click with an empty hand) actively pull from the adjacent block. See
 * {@link ConduitBlockEntity} for the network logic.
 */
public class ConduitBlock extends PipeBlock implements EntityBlock {

    /** What the conduit carries; decides which capability it exposes and connects to. */
    public enum Type {
        ENERGY, FLUID, GAS, ITEM
    }

    public final Type type;

    public ConduitBlock(Properties props, Type type) {
        super(0.1875F, props);
        this.type = type;
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    private boolean canConnect(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.getBlock() instanceof ConduitBlock conduit) {
            return conduit.type == type;
        }
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be == null) {
            return false;
        }
        return be.getCapability(connectionCapability(), dir.getOpposite()).isPresent();
    }

    /** The capability whose presence on a neighbour makes this conduit visually connect. */
    private Capability<?> connectionCapability() {
        return switch (type) {
            case ENERGY -> ForgeCapabilities.ENERGY;
            case FLUID -> ForgeCapabilities.FLUID_HANDLER;
            case GAS -> mekanism.common.capabilities.Capabilities.GAS_HANDLER;
            case ITEM -> ForgeCapabilities.ITEM_HANDLER;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), canConnect(level, pos, dir));
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(dir), canConnect(level, pos, dir));
    }

    /** Empty-hand right click toggles extract mode on the clicked face. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Direction face = hit.getDirection();
        if (!(level.getBlockEntity(pos) instanceof ConduitBlockEntity conduit)) {
            return InteractionResult.PASS;
        }
        boolean now = conduit.toggleExtract(face);
        player.displayClientMessage(Component.translatable(
                "gui." + MekanismMoreMultiblock.MODID + (now ? ".conduit_extract_on" : ".conduit_extract_off"),
                Component.translatable("gui." + MekanismMoreMultiblock.MODID + ".face_" + face.getName())), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConduitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof ConduitBlockEntity conduit) {
                conduit.serverTick();
            }
        };
    }
}
