package io.github.willianjs94.appliedtiers.registry;

import java.util.function.Supplier;
import io.github.willianjs94.appliedtiers.menu.ParallelAssemblerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import appeng.block.AEBaseBlockItem;
import appeng.block.crafting.PatternProviderBlock;
import io.github.willianjs94.appliedtiers.AppliedTiers;
import io.github.willianjs94.appliedtiers.block.AdvancedPatternProviderBlock;
import io.github.willianjs94.appliedtiers.block.ParallelAssemblerBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final String ADVANCED_ID = "advanced_pattern_provider";
    private static final String ELITE_ID = "elite_pattern_provider";
    private static final String ULTIMATE_ID = "ultimate_pattern_provider";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AppliedTiers.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AppliedTiers.MOD_ID);

    public static final DeferredBlock<AdvancedPatternProviderBlock> ADVANCED_PATTERN_PROVIDER =
            BLOCKS.registerBlock(ADVANCED_ID, AdvancedPatternProviderBlock::new);

    public static final DeferredItem<AEBaseBlockItem> ADVANCED_PATTERN_PROVIDER_ITEM =
            ITEMS.register(ADVANCED_ID, () -> new AEBaseBlockItem(
                    ADVANCED_PATTERN_PROVIDER.get(),
                    new Item.Properties().setId(ResourceKey.create(
                            Registries.ITEM,
                            Identifier.fromNamespaceAndPath(AppliedTiers.MOD_ID, ADVANCED_ID)))
                            .useBlockDescriptionPrefix()));

    public static final DeferredBlock<PatternProviderBlock> ELITE_PATTERN_PROVIDER =
            BLOCKS.registerBlock(ELITE_ID, PatternProviderBlock::new);

    public static final DeferredBlock<PatternProviderBlock> ULTIMATE_PATTERN_PROVIDER =
            BLOCKS.registerBlock(ULTIMATE_ID, PatternProviderBlock::new);

    public static final DeferredItem<AEBaseBlockItem> ELITE_PATTERN_PROVIDER_ITEM =
            registerBlockItem(ELITE_ID, ELITE_PATTERN_PROVIDER);

    public static final DeferredItem<AEBaseBlockItem> ULTIMATE_PATTERN_PROVIDER_ITEM =
            registerBlockItem(ULTIMATE_ID, ULTIMATE_PATTERN_PROVIDER);

    private static DeferredItem<AEBaseBlockItem> registerBlockItem(
            String name, DeferredBlock<? extends Block> block) {
        return ITEMS.register(name, () -> new AEBaseBlockItem(
                block.get(),
                new Item.Properties().setId(ResourceKey.create(
                        Registries.ITEM,
                        Identifier.fromNamespaceAndPath(AppliedTiers.MOD_ID, name)))
                        .useBlockDescriptionPrefix()));
    }

    public static final String ADVANCED_ASSEMBLER_ID = "advanced_molecular_assembler";
    public static final DeferredBlock<ParallelAssemblerBlock> ADVANCED_MOLECULAR_ASSEMBLER =
            registerAssembler(ADVANCED_ASSEMBLER_ID, () -> ModMenus.ADVANCED_MOLECULAR_ASSEMBLER.get());
    public static final DeferredItem<AEBaseBlockItem> ADVANCED_MOLECULAR_ASSEMBLER_ITEM =
            registerBlockItem(ADVANCED_ASSEMBLER_ID, ADVANCED_MOLECULAR_ASSEMBLER);

    public static final String ELITE_ASSEMBLER_ID = "elite_molecular_assembler";
    public static final DeferredBlock<ParallelAssemblerBlock> ELITE_MOLECULAR_ASSEMBLER =
            registerAssembler(ELITE_ASSEMBLER_ID, () -> ModMenus.ELITE_MOLECULAR_ASSEMBLER.get());
    public static final DeferredItem<AEBaseBlockItem> ELITE_MOLECULAR_ASSEMBLER_ITEM =
            registerBlockItem(ELITE_ASSEMBLER_ID, ELITE_MOLECULAR_ASSEMBLER);

    public static final String ULTIMATE_ASSEMBLER_ID = "ultimate_molecular_assembler";
    public static final DeferredBlock<ParallelAssemblerBlock> ULTIMATE_MOLECULAR_ASSEMBLER =
            registerAssembler(ULTIMATE_ASSEMBLER_ID, () -> ModMenus.ULTIMATE_MOLECULAR_ASSEMBLER.get());
    public static final DeferredItem<AEBaseBlockItem> ULTIMATE_MOLECULAR_ASSEMBLER_ITEM =
            registerBlockItem(ULTIMATE_ASSEMBLER_ID, ULTIMATE_MOLECULAR_ASSEMBLER);

    private static DeferredBlock<ParallelAssemblerBlock> registerAssembler(String id,
            Supplier<MenuType<
                    ParallelAssemblerMenu>> menu) {
        return BLOCKS.registerBlock(id, properties -> new ParallelAssemblerBlock(properties, menu),
                () -> BlockBehaviour.Properties.of()
                        .strength(2.2f, 11.0f)
                        .sound(SoundType.METAL)
                        .noOcclusion().requiresCorrectToolForDrops());
    }

    public static final DeferredBlock<appeng.block.crafting.CraftingUnitBlock> ADVANCED_CRAFTING_ACCELERATOR =
            BLOCKS.registerBlock("advanced_crafting_accelerator", p -> new appeng.block.crafting.CraftingUnitBlock(p,
                    io.github.willianjs94.appliedtiers.block.TierCraftingUnitType.ADVANCED));
    public static final DeferredItem<AEBaseBlockItem> ADVANCED_CRAFTING_ACCELERATOR_ITEM =
            registerBlockItem("advanced_crafting_accelerator", ADVANCED_CRAFTING_ACCELERATOR);

    public static final DeferredBlock<appeng.block.crafting.CraftingUnitBlock> ELITE_CRAFTING_ACCELERATOR =
            BLOCKS.registerBlock("elite_crafting_accelerator", p -> new appeng.block.crafting.CraftingUnitBlock(p,
                    io.github.willianjs94.appliedtiers.block.TierCraftingUnitType.ELITE));
    public static final DeferredItem<AEBaseBlockItem> ELITE_CRAFTING_ACCELERATOR_ITEM =
            registerBlockItem("elite_crafting_accelerator", ELITE_CRAFTING_ACCELERATOR);

    public static final DeferredBlock<appeng.block.crafting.CraftingUnitBlock> ULTIMATE_CRAFTING_ACCELERATOR =
            BLOCKS.registerBlock("ultimate_crafting_accelerator", p -> new appeng.block.crafting.CraftingUnitBlock(p,
                    io.github.willianjs94.appliedtiers.block.TierCraftingUnitType.ULTIMATE));
    public static final DeferredItem<AEBaseBlockItem> ULTIMATE_CRAFTING_ACCELERATOR_ITEM =
            registerBlockItem("ultimate_crafting_accelerator", ULTIMATE_CRAFTING_ACCELERATOR);
    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
