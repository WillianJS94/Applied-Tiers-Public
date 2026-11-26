package io.github.willianjs94.appliedtiers.registry;

import java.util.function.Supplier;
import io.github.willianjs94.appliedtiers.block.ParallelAssemblerBlock;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import java.util.concurrent.atomic.AtomicReference;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import io.github.willianjs94.appliedtiers.AppliedTiers;
import io.github.willianjs94.appliedtiers.blockentity.AdvancedPatternProviderBlockEntity;
import io.github.willianjs94.appliedtiers.blockentity.ParallelAssemblerBlockEntity;
import io.github.willianjs94.appliedtiers.blockentity.ElitePatternProviderBlockEntity;
import io.github.willianjs94.appliedtiers.blockentity.UltimatePatternProviderBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AppliedTiers.MOD_ID);

    // Parent AE2 block uses AEBaseEntityBlock<PatternProviderBlockEntity>.
    // Keep that generic type, but instantiate our specialized subclass.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternProviderBlockEntity>>
            ADVANCED_PATTERN_PROVIDER = TYPES.register("advanced_pattern_provider", () -> {
                AtomicReference<BlockEntityType<PatternProviderBlockEntity>> typeHolder =
                        new AtomicReference<>();
                BlockEntityType<PatternProviderBlockEntity> type = new BlockEntityType<>(
                        (pos, state) -> new AdvancedPatternProviderBlockEntity(
                                typeHolder.get(), pos, state),
                        ModBlocks.ADVANCED_PATTERN_PROVIDER.get());
                typeHolder.set(type);

                ModBlocks.ADVANCED_PATTERN_PROVIDER.get().setBlockEntity(
                        PatternProviderBlockEntity.class, type, null, null);
                AEBaseBlockEntity.registerBlockEntityItem(
                        type, ModBlocks.ADVANCED_PATTERN_PROVIDER_ITEM.get());

                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternProviderBlockEntity>>
            ELITE_PATTERN_PROVIDER = TYPES.register("elite_pattern_provider", () -> {
                AtomicReference<BlockEntityType<PatternProviderBlockEntity>> typeHolder =
                        new AtomicReference<>();
                BlockEntityType<PatternProviderBlockEntity> type = new BlockEntityType<>(
                        (pos, state) -> new ElitePatternProviderBlockEntity(
                                typeHolder.get(), pos, state),
                        ModBlocks.ELITE_PATTERN_PROVIDER.get());
                typeHolder.set(type);

                ModBlocks.ELITE_PATTERN_PROVIDER.get().setBlockEntity(
                        PatternProviderBlockEntity.class, type, null, null);
                AEBaseBlockEntity.registerBlockEntityItem(
                        type, ModBlocks.ELITE_PATTERN_PROVIDER_ITEM.get());

                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternProviderBlockEntity>>
            ULTIMATE_PATTERN_PROVIDER = TYPES.register("ultimate_pattern_provider", () -> {
                AtomicReference<BlockEntityType<PatternProviderBlockEntity>> typeHolder =
                        new AtomicReference<>();
                BlockEntityType<PatternProviderBlockEntity> type = new BlockEntityType<>(
                        (pos, state) -> new UltimatePatternProviderBlockEntity(
                                typeHolder.get(), pos, state),
                        ModBlocks.ULTIMATE_PATTERN_PROVIDER.get());
                typeHolder.set(type);

                ModBlocks.ULTIMATE_PATTERN_PROVIDER.get().setBlockEntity(
                        PatternProviderBlockEntity.class, type, null, null);
                AEBaseBlockEntity.registerBlockEntityItem(
                        type, ModBlocks.ULTIMATE_PATTERN_PROVIDER_ITEM.get());

                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ParallelAssemblerBlockEntity>>
            ADVANCED_MOLECULAR_ASSEMBLER = registerAssembler(ModBlocks.ADVANCED_ASSEMBLER_ID,
                    ModBlocks.ADVANCED_MOLECULAR_ASSEMBLER, ModBlocks.ADVANCED_MOLECULAR_ASSEMBLER_ITEM, 2);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ParallelAssemblerBlockEntity>>
            ELITE_MOLECULAR_ASSEMBLER = registerAssembler(ModBlocks.ELITE_ASSEMBLER_ID,
                    ModBlocks.ELITE_MOLECULAR_ASSEMBLER, ModBlocks.ELITE_MOLECULAR_ASSEMBLER_ITEM, 3);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ParallelAssemblerBlockEntity>>
            ULTIMATE_MOLECULAR_ASSEMBLER = registerAssembler(ModBlocks.ULTIMATE_ASSEMBLER_ID,
                    ModBlocks.ULTIMATE_MOLECULAR_ASSEMBLER, ModBlocks.ULTIMATE_MOLECULAR_ASSEMBLER_ITEM, 4);

    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<ParallelAssemblerBlockEntity>> registerAssembler(
            String id, DeferredBlock<
                    ParallelAssemblerBlock> block,
            Supplier<? extends Item> item, int capacity) {
        return TYPES.register(id, () -> {
            var holder = new AtomicReference<BlockEntityType<ParallelAssemblerBlockEntity>>();
            var type = new BlockEntityType<ParallelAssemblerBlockEntity>(
                    (pos, state) -> new ParallelAssemblerBlockEntity(holder.get(), pos, state, block.get(), capacity),
                    block.get());
            holder.set(type);
            block.get().setBlockEntity(ParallelAssemblerBlockEntity.class, type, null, null);
            AEBaseBlockEntity.registerBlockEntityItem(type, item.get());
            return type;
        });
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<appeng.blockentity.crafting.CraftingBlockEntity>>
            ADVANCED_CRAFTING_ACCELERATOR = registerAccelerator("advanced_crafting_accelerator", ModBlocks.ADVANCED_CRAFTING_ACCELERATOR, 2);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<appeng.blockentity.crafting.CraftingBlockEntity>>
            ELITE_CRAFTING_ACCELERATOR = registerAccelerator("elite_crafting_accelerator", ModBlocks.ELITE_CRAFTING_ACCELERATOR, 3);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<appeng.blockentity.crafting.CraftingBlockEntity>>
            ULTIMATE_CRAFTING_ACCELERATOR = registerAccelerator("ultimate_crafting_accelerator", ModBlocks.ULTIMATE_CRAFTING_ACCELERATOR, 4);
    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<appeng.blockentity.crafting.CraftingBlockEntity>> registerAccelerator(
            String id, DeferredBlock<appeng.block.crafting.CraftingUnitBlock> block, int power) {
        return TYPES.register(id, () -> {
            var holder = new AtomicReference<BlockEntityType<appeng.blockentity.crafting.CraftingBlockEntity>>();
            var type = new BlockEntityType<appeng.blockentity.crafting.CraftingBlockEntity>((pos, state) -> {
                var entity = new appeng.blockentity.crafting.CraftingBlockEntity(holder.get(), pos, state);
                entity.getMainNode().setIdlePowerUsage(power);
                return entity;
            }, block.get());
            holder.set(type);
            block.get().setBlockEntity(appeng.blockentity.crafting.CraftingBlockEntity.class, type, null, null);
            return type;
        });
    }

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
    }
}
