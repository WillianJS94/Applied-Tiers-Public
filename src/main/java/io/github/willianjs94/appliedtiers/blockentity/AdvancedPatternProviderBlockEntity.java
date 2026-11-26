package io.github.willianjs94.appliedtiers.blockentity;

import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import io.github.willianjs94.appliedtiers.registry.ModBlocks;
import io.github.willianjs94.appliedtiers.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class AdvancedPatternProviderBlockEntity extends PatternProviderBlockEntity {
    public static final int PATTERN_SLOTS = 18;

    public AdvancedPatternProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected PatternProviderLogic createLogic() {
        return new PatternProviderLogic(getMainNode(), this, PATTERN_SLOTS);
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(ModMenus.ADVANCED_PATTERN_PROVIDER.get(), player, locator);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(ModMenus.ADVANCED_PATTERN_PROVIDER.get(), player, subMenu.getLocator());
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(ModBlocks.ADVANCED_PATTERN_PROVIDER_ITEM.get().getDefaultInstance());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return ModBlocks.ADVANCED_PATTERN_PROVIDER_ITEM.get().getDefaultInstance();
    }
}
