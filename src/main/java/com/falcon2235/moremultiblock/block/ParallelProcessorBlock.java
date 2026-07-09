package com.falcon2235.moremultiblock.block;

import com.falcon2235.moremultiblock.MekanismMoreMultiblock;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Wall block that raises the parallel operation count of the multiblock it is built into.
 * When multiple units of different tiers are installed, the highest tier wins.
 */
public class ParallelProcessorBlock extends Block {

    public final int parallel;

    public ParallelProcessorBlock(Properties properties, int parallel) {
        super(properties);
        this.parallel = parallel;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip." + MekanismMoreMultiblock.MODID + ".parallel", parallel)
                .withStyle(ChatFormatting.GRAY));
    }
}
