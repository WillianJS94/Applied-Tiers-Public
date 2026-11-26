package io.github.willianjs94.appliedtiers.part;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.ids.AEComponents;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.parts.crafting.PatternProviderPart;
import appeng.util.InteractionUtil;
import appeng.util.SettingsFrom;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class TierPatternProviderPart extends PatternProviderPart {
    public TierPatternProviderPart(TierPatternProviderItem item) {
        super(item);
    }

    private TierPatternProviderItem tierItem() {
        return (TierPatternProviderItem) getPartItem();
    }

    @Override
    protected PatternProviderLogic createLogic() {
        // AEBasePart has already stored the item when PatternProviderPart creates the logic.
        return new PatternProviderLogic(getMainNode(), this, tierItem().patternCapacity());
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(tierItem().menuType(), player, locator);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(tierItem().menuType(), player, subMenu.getLocator());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return tierItem().getDefaultInstance();
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder) {
        super.exportSettings(mode, builder);
        if (mode == SettingsFrom.MEMORY_CARD) {
            builder.set(AEComponents.EXPORTED_SETTINGS_SOURCE, tierItem().blockItem().getDefaultInstance().getItemName());
        }
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, Player player, InteractionHand hand, Vec3 pos) {
        if (heldItem.getItem() instanceof IMemoryCard
                && !InteractionUtil.isInAlternateUseMode(player)
                && tierItem().blockItem().getDefaultInstance().getItemName()
                        .equals(heldItem.get(AEComponents.EXPORTED_SETTINGS_SOURCE))) {
            // AE2's part reader matches the part item name. Normalize only a read
            // of this tier's card, leaving the real card usable on its block.
            var cardForPart = heldItem.copy();
            cardForPart.set(AEComponents.EXPORTED_SETTINGS_SOURCE, tierItem().getDefaultInstance().getItemName());
            return super.onUseItemOn(cardForPart, player, hand, pos);
        }
        return super.onUseItemOn(heldItem, player, hand, pos);
    }
}
