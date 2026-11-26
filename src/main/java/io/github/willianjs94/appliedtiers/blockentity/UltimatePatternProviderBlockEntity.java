package io.github.willianjs94.appliedtiers.blockentity;

import io.github.willianjs94.appliedtiers.registry.ModBlocks;
import io.github.willianjs94.appliedtiers.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class UltimatePatternProviderBlockEntity extends AdditionalPatternProviderBlockEntity {
    public static final int PATTERN_SLOTS = 72;

    public UltimatePatternProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected int getPatternCapacity() {
        return PATTERN_SLOTS;
    }

    @Override
    protected MenuType<?> getTierMenu() {
        return ModMenus.ULTIMATE_PATTERN_PROVIDER.get();
    }

    @Override
    protected ItemStack getTierItem() {
        return ModBlocks.ULTIMATE_PATTERN_PROVIDER_ITEM.get().getDefaultInstance();
    }
}
