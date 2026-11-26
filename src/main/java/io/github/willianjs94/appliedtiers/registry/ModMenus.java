package io.github.willianjs94.appliedtiers.registry;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.implementations.MenuTypeBuilder;
import io.github.willianjs94.appliedtiers.AppliedTiers;
import io.github.willianjs94.appliedtiers.menu.AdvancedPatternProviderMenu;
import io.github.willianjs94.appliedtiers.menu.ParallelAssemblerMenu;
import io.github.willianjs94.appliedtiers.blockentity.ParallelAssemblerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> TYPES =
            DeferredRegister.create(Registries.MENU, AppliedTiers.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedPatternProviderMenu>>
            ADVANCED_PATTERN_PROVIDER = TYPES.register("advanced_pattern_provider", () ->
                    MenuTypeBuilder.<AdvancedPatternProviderMenu, PatternProviderLogicHost>create(
                            AdvancedPatternProviderMenu::new,
                            PatternProviderLogicHost.class)
                            .buildUnregistered(Identifier.fromNamespaceAndPath(
                                    AppliedTiers.MOD_ID, "advanced_pattern_provider")));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedPatternProviderMenu>>
            ELITE_PATTERN_PROVIDER = TYPES.register("elite_pattern_provider", () ->
                    MenuTypeBuilder.<AdvancedPatternProviderMenu, PatternProviderLogicHost>create(
                            AdvancedPatternProviderMenu::new,
                            PatternProviderLogicHost.class)
                            .buildUnregistered(Identifier.fromNamespaceAndPath(
                                    AppliedTiers.MOD_ID, "elite_pattern_provider")));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedPatternProviderMenu>>
            ULTIMATE_PATTERN_PROVIDER = TYPES.register("ultimate_pattern_provider", () ->
                    MenuTypeBuilder.<AdvancedPatternProviderMenu, PatternProviderLogicHost>create(
                            AdvancedPatternProviderMenu::new,
                            PatternProviderLogicHost.class)
                            .buildUnregistered(Identifier.fromNamespaceAndPath(
                                    AppliedTiers.MOD_ID, "ultimate_pattern_provider")));

    public static final DeferredHolder<MenuType<?>, MenuType<ParallelAssemblerMenu>>
            ADVANCED_MOLECULAR_ASSEMBLER = registerAssembler(ModBlocks.ADVANCED_ASSEMBLER_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<ParallelAssemblerMenu>>
            ELITE_MOLECULAR_ASSEMBLER = registerAssembler(ModBlocks.ELITE_ASSEMBLER_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<ParallelAssemblerMenu>>
            ULTIMATE_MOLECULAR_ASSEMBLER = registerAssembler(ModBlocks.ULTIMATE_ASSEMBLER_ID);

    private static DeferredHolder<MenuType<?>, MenuType<ParallelAssemblerMenu>> registerAssembler(String id) {
        return TYPES.register(id, () -> MenuTypeBuilder.<ParallelAssemblerMenu, ParallelAssemblerBlockEntity>create(
                ParallelAssemblerMenu::new, ParallelAssemblerBlockEntity.class)
                .buildUnregistered(Identifier.fromNamespaceAndPath(AppliedTiers.MOD_ID, id)));
    }

    private ModMenus() {
    }

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
    }
}
