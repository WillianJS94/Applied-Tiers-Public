package io.github.willianjs94.appliedtiers.registry;

import io.github.willianjs94.appliedtiers.blockentity.ParallelAssemblerBlockEntity;
import appeng.api.AECapabilities;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * AE2 registers its own block entities as capability providers, but that
 * registration does not automatically cover block entities from addons.
 *
 * Without GENERIC_INTERNAL_INV, a Molecular Assembler cannot insert its
 * crafting output into this provider's return inventory. AE2's adapter
 * translates that generic inventory to a sided item capability.
 *
 * IN_WORLD_GRID_NODE_HOST is also required for adjacent AE2 grid nodes to
 * discover the addon block.
 */
public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ADVANCED_CRAFTING_ACCELERATOR.get(), (entity, side) -> entity);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ELITE_CRAFTING_ACCELERATOR.get(), (entity, side) -> entity);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ULTIMATE_CRAFTING_ACCELERATOR.get(), (entity, side) -> entity);
        registerTier(event, ModBlockEntities.ADVANCED_PATTERN_PROVIDER.get());
        registerTier(event, ModBlockEntities.ELITE_PATTERN_PROVIDER.get());
        registerTier(event, ModBlockEntities.ULTIMATE_PATTERN_PROVIDER.get());
        registerAssembler(event, ModBlockEntities.ADVANCED_MOLECULAR_ASSEMBLER.get());
        registerAssembler(event, ModBlockEntities.ELITE_MOLECULAR_ASSEMBLER.get());
        registerAssembler(event, ModBlockEntities.ULTIMATE_MOLECULAR_ASSEMBLER.get());
    }

    private static void registerAssembler(RegisterCapabilitiesEvent event,
            BlockEntityType<ParallelAssemblerBlockEntity> assembler) {
        event.registerBlockEntity(AECapabilities.CRAFTING_MACHINE, assembler, (machine, side) -> machine);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, assembler, (machine, ignored) -> machine);
        event.registerBlockEntity(Capabilities.Item.BLOCK, assembler, (machine, side) -> machine.getExposedItemHandler(side));
    }

    private static void registerTier(
            RegisterCapabilitiesEvent event,
            BlockEntityType<PatternProviderBlockEntity> type) {
        event.registerBlockEntity(AECapabilities.GENERIC_INTERNAL_INV, type,
                (blockEntity, side) -> blockEntity.getLogic().getReturnInv());

        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type,
                (blockEntity, ignored) -> blockEntity);
    }
}
