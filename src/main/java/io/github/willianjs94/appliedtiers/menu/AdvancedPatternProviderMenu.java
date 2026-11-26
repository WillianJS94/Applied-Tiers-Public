package io.github.willianjs94.appliedtiers.menu;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.implementations.PatternProviderMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/**
 * Reuses AE2's slot synchronization. It already creates as many pattern slots
 * as getPatternInv().size(), so the Advanced provider contributes 18 slots.
 */
public final class AdvancedPatternProviderMenu extends PatternProviderMenu {
    public AdvancedPatternProviderMenu(
            MenuType<? extends PatternProviderMenu> type,
            int id,
            Inventory playerInventory,
            PatternProviderLogicHost host) {
        super(type, id, playerInventory, host);
    }
}
