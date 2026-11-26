package io.github.willianjs94.appliedtiers.registry;

import java.util.function.Supplier;

import appeng.api.AECapabilities;
import appeng.api.parts.RegisterPartCapabilitiesEvent;
import io.github.willianjs94.appliedtiers.AppliedTiers;
import io.github.willianjs94.appliedtiers.blockentity.AdvancedPatternProviderBlockEntity;
import io.github.willianjs94.appliedtiers.blockentity.ElitePatternProviderBlockEntity;
import io.github.willianjs94.appliedtiers.blockentity.UltimatePatternProviderBlockEntity;
import io.github.willianjs94.appliedtiers.part.TierPatternProviderItem;
import io.github.willianjs94.appliedtiers.part.TierPatternProviderPart;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModParts {
    public static final DeferredItem<TierPatternProviderItem> ADVANCED = register("cable_advanced_pattern_provider",
            AdvancedPatternProviderBlockEntity.PATTERN_SLOTS, ModMenus.ADVANCED_PATTERN_PROVIDER,
            ModBlocks.ADVANCED_PATTERN_PROVIDER_ITEM);
    public static final DeferredItem<TierPatternProviderItem> ELITE = register("cable_elite_pattern_provider",
            ElitePatternProviderBlockEntity.PATTERN_SLOTS, ModMenus.ELITE_PATTERN_PROVIDER,
            ModBlocks.ELITE_PATTERN_PROVIDER_ITEM);
    public static final DeferredItem<TierPatternProviderItem> ULTIMATE = register("cable_ultimate_pattern_provider",
            UltimatePatternProviderBlockEntity.PATTERN_SLOTS, ModMenus.ULTIMATE_PATTERN_PROVIDER,
            ModBlocks.ULTIMATE_PATTERN_PROVIDER_ITEM);

    private ModParts() {
    }

    private static DeferredItem<TierPatternProviderItem> register(String name, int patternCapacity,
            Supplier<? extends MenuType<?>> menu, Supplier<? extends Item> blockItem) {
        var id = Identifier.fromNamespaceAndPath(AppliedTiers.MOD_ID, name);
        return ModBlocks.ITEMS.register(name, () -> new TierPatternProviderItem(
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)),
                patternCapacity, menu, blockItem));
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ModParts::registerCapabilities);
    }

    private static void registerCapabilities(RegisterPartCapabilitiesEvent event) {
        // The cable bus already exposes its grid node and adapts this generic
        // return inventory to sided item/fluid capabilities.
        event.register(AECapabilities.GENERIC_INTERNAL_INV,
                (part, side) -> part.getLogic().getReturnInv(), TierPatternProviderPart.class);
    }
}
