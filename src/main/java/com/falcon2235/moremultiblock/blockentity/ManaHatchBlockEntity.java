package com.falcon2235.moremultiblock.blockentity;

import com.falcon2235.moremultiblock.MMMRegistry;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.api.mana.spark.SparkAttachable;

/**
 * The mana hatch's tank: buffers Botania mana for the grand mana pool / elven gate /
 * terra plate multiblocks. Exposes Botania's mana-receiver and spark-attachable
 * capabilities, so mana spreaders can shoot it and a spark on top joins the spark
 * network (put a dominant spark here to pull from sparked pools).
 *
 * <p>Only classloaded when Botania is present — see {@link ManaHatchSupport}.
 */
public class ManaHatchBlockEntity extends BlockEntity implements ManaReceiver, SparkAttachable {

    /** Enough for one full terra-plate batch (16 terrasteel = 8,000,000 mana). */
    public static final int CAPACITY = 10_000_000;

    private int mana;

    public ManaHatchBlockEntity(BlockPos pos, BlockState state) {
        super(MMMRegistry.MANA_HATCH_BE.get(), pos, state);
    }

    public int getMana() {
        return mana;
    }

    /** Removes up to {@code amount} mana; returns how much was actually taken. */
    public int drain(int amount) {
        int taken = Math.min(amount, mana);
        if (taken > 0) {
            mana -= taken;
            setChanged();
        }
        return taken;
    }

    // --- Botania ManaReceiver ---

    @Override
    public Level getManaReceiverLevel() {
        return level;
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return worldPosition;
    }

    @Override
    public int getCurrentMana() {
        return mana;
    }

    @Override
    public boolean isFull() {
        return mana >= CAPACITY;
    }

    @Override
    public void receiveMana(int amount) {
        mana = Math.max(0, Math.min(CAPACITY, mana + amount));
        setChanged();
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return true;
    }

    // --- Botania SparkAttachable ---

    @Override
    public boolean canAttachSpark(ItemStack stack) {
        return true;
    }

    @Override
    public int getAvailableSpaceForMana() {
        return Math.max(0, CAPACITY - mana);
    }

    @Override
    public ManaSpark getAttachedSpark() {
        if (level == null) {
            return null;
        }
        List<Entity> sparks = level.getEntitiesOfClass(Entity.class,
                new AABB(worldPosition.above(), worldPosition.above().offset(1, 1, 1)),
                entity -> entity instanceof ManaSpark);
        return sparks.size() == 1 ? (ManaSpark) sparks.get(0) : null;
    }

    @Override
    public boolean areIncomingTranfersDone() {
        return false;
    }

    // --- capabilities ---

    private final LazyOptional<ManaReceiver> manaCap = LazyOptional.of(() -> this);
    private final LazyOptional<SparkAttachable> sparkCap = LazyOptional.of(() -> this);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!remove) {
            if (cap == BotaniaForgeCapabilities.MANA_RECEIVER) {
                return manaCap.cast();
            }
            if (cap == BotaniaForgeCapabilities.SPARK_ATTACHABLE) {
                return sparkCap.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        manaCap.invalidate();
        sparkCap.invalidate();
    }

    // --- nbt ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Mana", mana);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mana = tag.getInt("Mana");
    }
}
