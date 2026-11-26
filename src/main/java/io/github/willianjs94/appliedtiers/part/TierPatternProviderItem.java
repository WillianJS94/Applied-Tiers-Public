package io.github.willianjs94.appliedtiers.part;

import java.util.function.Supplier;

import appeng.items.parts.PartItem;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;

public final class TierPatternProviderItem extends PartItem<TierPatternProviderPart> {
    private final int patternCapacity;
    private final Supplier<? extends MenuType<?>> menu;
    private final Supplier<? extends Item> blockItem;

    public TierPatternProviderItem(Properties properties, int patternCapacity,
            Supplier<? extends MenuType<?>> menu, Supplier<? extends Item> blockItem) {
        super(properties, TierPatternProviderPart.class,
                item -> new TierPatternProviderPart((TierPatternProviderItem) item));
        this.patternCapacity = patternCapacity;
        this.menu = menu;
        this.blockItem = blockItem;
    }

    int patternCapacity() {
        return patternCapacity;
    }

    MenuType<?> menuType() {
        return menu.get();
    }

    Item blockItem() {
        return blockItem.get();
    }
}
