package com.chihaya.moremultiblock.blockentity;

import com.chihaya.moremultiblock.block.PortBlock;

import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.energy.IStrictEnergyHandler;

import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A multiblock controller that ports can relay to. Implemented by both the parallel
 * machine controller and the chemical machine controller, so the same energy / item
 * ports work on either kind of multiblock. Gas and fluid are only provided by the
 * chemical machines; the parallel machines return null (their ports stay inert).
 */
public interface PortHost {

    boolean isFormed();

    ItemStackHandler getInputs();

    ItemStackHandler getOutputs();

    IEnergyStorage getFeHandler();

    IStrictEnergyHandler getStrictEnergyHandler();

    @Nullable
    default IGasHandler getGasHandler() {
        return null;
    }

    @Nullable
    default IFluidHandler getFluidHandler() {
        return null;
    }

    /** Visual skin ports built into this machine should adopt. */
    default PortBlock.PortStyle portStyle() {
        return PortBlock.PortStyle.DEFAULT;
    }
}
