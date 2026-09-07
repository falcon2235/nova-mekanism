package com.falcon2235.moremultiblock.block;

import com.falcon2235.moremultiblock.MekanismMoreMultiblock;
import com.falcon2235.moremultiblock.blockentity.ChemMachineBlockEntity;
import com.falcon2235.moremultiblock.blockentity.ResearchHatchBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Research data hatch: build it into any multiblock's wall and it feeds the
 * machine whatever research data it holds. Right-click with research data to
 * install it, right-click empty-handed to take it back — no GUI needed, and
 * hoppers/pipes can swap it automatically through the item capability.
 */
public class ResearchHatchBlock extends Block implements EntityBlock {

    /** Lit when the hatch holds research data, so a line's state is readable at a glance. */
    public static final BooleanProperty LOADED = BooleanProperty.create("loaded");
    private static final String LANG = "block." + MekanismMoreMultiblock.MODID + ".research_hatch.";

    public ResearchHatchBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LOADED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOADED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ResearchHatchBlockEntity(pos, state);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand,
                                          @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ResearchHatchBlockEntity hatch)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        var slots = hatch.getItem();
        if (held.isEmpty()) {
            // Empty-handed: take the last one back out, so repeated clicks unload it.
            for (int i = ResearchHatchBlockEntity.SLOTS - 1; i >= 0; i--) {
                if (slots.getStackInSlot(i).isEmpty()) {
                    continue;
                }
                ItemStack taken = slots.extractItem(i, 1, false);
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
                player.displayClientMessage(Component.translatable(LANG + "removed", taken.getHoverName()), true);
                setLoaded(level, pos, state, hatch.hasAny());
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable(LANG + "empty"), true);
            return InteractionResult.CONSUME;
        }
        if (!ChemMachineBlockEntity.isResearchData(held)) {
            player.displayClientMessage(Component.translatable(LANG + "invalid"), true);
            return InteractionResult.CONSUME;
        }
        if (hatch.holds(held)) {
            // One of each kind: a duplicate would waste a slot for no benefit.
            player.displayClientMessage(Component.translatable(LANG + "duplicate", held.getHoverName()), true);
            return InteractionResult.CONSUME;
        }
        for (int i = 0; i < ResearchHatchBlockEntity.SLOTS; i++) {
            if (slots.insertItem(i, held.copyWithCount(1), true).isEmpty()) {
                slots.insertItem(i, held.copyWithCount(1), false);
                held.shrink(1);
                player.displayClientMessage(Component.translatable(LANG + "installed",
                        slots.getStackInSlot(i).getHoverName(), hatch.installedAll().size(),
                        ResearchHatchBlockEntity.SLOTS), true);
                setLoaded(level, pos, state, true);
                return InteractionResult.CONSUME;
            }
        }
        player.displayClientMessage(Component.translatable(LANG + "full"), true);
        return InteractionResult.CONSUME;
    }

    private static void setLoaded(Level level, BlockPos pos, BlockState state, boolean loaded) {
        if (state.getValue(LOADED) != loaded) {
            level.setBlock(pos, state.setValue(LOADED, loaded), Block.UPDATE_ALL);
        }
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ResearchHatchBlockEntity hatch) {
            for (ItemStack installed : hatch.installedAll()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), installed);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof ResearchHatchBlockEntity hatch)) {
            return 0;
        }
        int loaded = hatch.installedAll().size();
        return loaded == 0 ? 0 : Math.max(1, loaded * 15 / ResearchHatchBlockEntity.SLOTS);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable BlockGetter level,
                                @NotNull java.util.List<Component> tooltip,
                                @NotNull net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable(LANG + "tip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
