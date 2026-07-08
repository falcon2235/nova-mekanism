package com.chihaya.moremultiblock.item;

import com.chihaya.moremultiblock.MekanismMoreMultiblock;
import com.chihaya.moremultiblock.block.PortBlock;
import com.chihaya.moremultiblock.blockentity.ChemMachineBlockEntity;
import com.chihaya.moremultiblock.blockentity.ControllerBlockEntity;
import com.chihaya.moremultiblock.blockentity.PbfBlockEntity;
import com.chihaya.moremultiblock.multiblock.StructureBlueprint;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Construction terminal: right-click a multiblock controller and it auto-builds that
 * machine's structure, pulling the casing/coil/glass blocks from the player's inventory
 * (free in creative). It only fills empty space — existing correct blocks and ports are
 * left untouched, and it never breaks other blocks — so it is safe to re-run.
 */
public class ConstructionTerminalItem extends Item {

    private static final String LANG = "item." + MekanismMoreMultiblock.MODID + ".construction_terminal.";

    public ConstructionTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = level.getBlockState(pos);
        List<StructureBlueprint.Cell> blueprint = StructureBlueprint.forController(pos, state);
        if (blueprint == null) {
            player.displayClientMessage(Component.translatable(LANG + "not_controller").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        boolean creative = player.getAbilities().instabuild;
        int placed = 0;
        int missing = 0;
        int blocked = 0;
        for (StructureBlueprint.Cell cell : blueprint) {
            BlockState current = level.getBlockState(cell.pos());
            if (current.is(cell.block()) || current.getBlock() instanceof PortBlock) {
                continue; // already correct, or a port the player placed
            }
            if (!current.isAir() && !current.canBeReplaced()) {
                blocked++;
                continue; // occupied by something else — never destroy it
            }
            if (!creative && !takeOne(player, cell.block())) {
                missing++;
                continue;
            }
            level.setBlock(cell.pos(), cell.block().defaultBlockState(), Block.UPDATE_ALL);
            placed++;
        }

        revalidate(level, pos);
        player.displayClientMessage(Component.translatable(LANG + "built", placed, missing, blocked)
                .withStyle(missing == 0 && blocked == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        return InteractionResult.CONSUME;
    }

    /** Removes one of {@code block}'s item from the player's inventory; false if none held. */
    private static boolean takeOne(Player player, Block block) {
        Item item = block.asItem();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void revalidate(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ControllerBlockEntity c) {
            c.revalidate();
        } else if (be instanceof ChemMachineBlockEntity c) {
            c.revalidate();
        } else if (be instanceof PbfBlockEntity c) {
            c.revalidate();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(LANG + "tip").withStyle(ChatFormatting.GRAY));
    }
}
