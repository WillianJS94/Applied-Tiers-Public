package io.github.willianjs94.appliedtiers.block;

import appeng.block.crafting.ICraftingUnitType;
import io.github.willianjs94.appliedtiers.registry.ModBlocks;
import net.minecraft.world.item.Item;

public enum TierCraftingUnitType implements ICraftingUnitType {
    ADVANCED(2),
    ELITE(3),
    ULTIMATE(4);

    private final int threads;
    TierCraftingUnitType(int threads) {
        this.threads = threads;
    }

    @Override
    public long getStorageBytes() {
        return 0;
    }

    @Override
    public int getAcceleratorThreads() {
        return threads;
    }

    @Override
    public Item getItemFromType() {
        return switch (this) {
            case ADVANCED -> ModBlocks.ADVANCED_CRAFTING_ACCELERATOR_ITEM.get();
            case ELITE -> ModBlocks.ELITE_CRAFTING_ACCELERATOR_ITEM.get();
            case ULTIMATE -> ModBlocks.ULTIMATE_CRAFTING_ACCELERATOR_ITEM.get();
        };
    }
}
