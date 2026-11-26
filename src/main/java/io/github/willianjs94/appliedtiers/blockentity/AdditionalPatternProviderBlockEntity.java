package io.github.willianjs94.appliedtiers.blockentity;

import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared AE2 behavior for additional block tiers.
 * The Advanced provider remains unchanged to preserve its tested behavior.
 */
public abstract class AdditionalPatternProviderBlockEntity extends PatternProviderBlockEntity {
    protected AdditionalPatternProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract int getPatternCapacity();

    protected abstract MenuType<?> getTierMenu();

    protected abstract ItemStack getTierItem();

    @Override
    protected PatternProviderLogic createLogic() {
        return new PatternProviderLogic(getMainNode(), this, getPatternCapacity());
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(getTierMenu(), player, locator);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(getTierMenu(), player, subMenu.getLocator());
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(getTierItem());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return getTierItem();
    }
}
