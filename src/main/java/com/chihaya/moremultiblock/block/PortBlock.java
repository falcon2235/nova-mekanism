package com.chihaya.moremultiblock.block;

import com.chihaya.moremultiblock.MMMRegistry;
import com.chihaya.moremultiblock.MekanismMoreMultiblock;
import com.chihaya.moremultiblock.blockentity.PortBlockEntity;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Wall block that relays energy, items, gas or fluid between the outside world and
 * the multiblock controller it is built into. Right-click opens a small GUI where
 * the port's auto-transfer mode (pull for input ports / push for output ports) can
 * be toggled.
 */
public class PortBlock extends Block implements EntityBlock {

    /** Visual skin the port adopts to match the machine it is built into. */
    public enum PortStyle implements net.minecraft.util.StringRepresentable {
        DEFAULT("default"),
        HEAT_PROOF("heat_proof"),
        PTFE("ptfe"),
        STAINLESS("stainless"),
        BRICK("brick"),
        ALLOY("alloy"),
        FROST("frost"),
        ASSEMBLY("assembly"),
        ELECTROLYZER("electrolyzer"),
        CENTRIFUGE("centrifuge"),
        FUSION("fusion");

        private final String name;

        PortStyle(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<PortStyle> STYLE =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("style", PortStyle.class);

    public enum PortType {
        ENERGY,
        ITEM_INPUT,
        ITEM_OUTPUT,
        GAS_INPUT,
        GAS_OUTPUT,
        FLUID_INPUT,
        FLUID_OUTPUT;

        public String translationKey() {
            return "tooltip." + MekanismMoreMultiblock.MODID + "." + name().toLowerCase(Locale.ROOT) + "_port";
        }

        /** Whether this port's auto mode moves things INTO the machine (otherwise out of it). */
        public boolean isAutoInput() {
            return this == ENERGY || this == ITEM_INPUT || this == GAS_INPUT || this == FLUID_INPUT;
        }
    }

    public final PortType type;

    public PortBlock(Properties properties, PortType type) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(STYLE, PortStyle.DEFAULT));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STYLE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortBlockEntity(pos, state);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != MMMRegistry.PORT_BE.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<PortBlockEntity>) (lvl, pos, st, be) -> be.serverTick();
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof PortBlockEntity be && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, be, buf -> {
                buf.writeBlockPos(pos);
                boolean hasHost = be.resolveHost() != null && be.getControllerPos() != null;
                buf.writeBoolean(hasHost);
                if (hasHost) {
                    buf.writeBlockPos(be.getControllerPos());
                }
            });
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PortBlockEntity be) {
            be.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(type.translationKey()).withStyle(ChatFormatting.GRAY));
    }
}
