package io.github.willianjs94.appliedtiers;

import io.github.willianjs94.appliedtiers.network.AssemblerAnimationPayload;
import io.github.willianjs94.appliedtiers.registry.ModBlockEntities;
import io.github.willianjs94.appliedtiers.registry.ModCapabilities;
import io.github.willianjs94.appliedtiers.registry.ModBlocks;
import io.github.willianjs94.appliedtiers.registry.ModMenus;
import io.github.willianjs94.appliedtiers.registry.ModParts;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(AppliedTiers.MOD_ID)
public final class AppliedTiers {
    public static final String MOD_ID = "appliedtiers";

    private static final Logger LOGGER = LogUtils.getLogger();

    public AppliedTiers(IEventBus modEventBus, ModContainer modContainer) {
        ModParts.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        modEventBus.addListener(ModCapabilities::register);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::addToCreativeTab);
        modEventBus.addListener(this::registerUpgrades);

        LOGGER.info("Applied Tiers: Pattern Providers (18, 36 e 72 slots) registrados.");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(AssemblerAnimationPayload.TYPE, AssemblerAnimationPayload.CODEC);
    }

    private void registerUpgrades(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.ADVANCED_MOLECULAR_ASSEMBLER, 5);
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.ELITE_MOLECULAR_ASSEMBLER, 5);
            Upgrades.add(AEItems.SPEED_CARD, ModBlocks.ULTIMATE_MOLECULAR_ASSEMBLER, 5);
        });
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.ADVANCED_CRAFTING_ACCELERATOR_ITEM);
            event.accept(ModBlocks.ELITE_CRAFTING_ACCELERATOR_ITEM);
            event.accept(ModBlocks.ULTIMATE_CRAFTING_ACCELERATOR_ITEM);
            event.accept(ModBlocks.ADVANCED_PATTERN_PROVIDER_ITEM);
            event.accept(ModBlocks.ELITE_PATTERN_PROVIDER_ITEM);
            event.accept(ModBlocks.ULTIMATE_PATTERN_PROVIDER_ITEM);
            event.accept(ModParts.ADVANCED);
            event.accept(ModParts.ELITE);
            event.accept(ModParts.ULTIMATE);
            event.accept(ModBlocks.ADVANCED_MOLECULAR_ASSEMBLER_ITEM);
            event.accept(ModBlocks.ELITE_MOLECULAR_ASSEMBLER_ITEM);
            event.accept(ModBlocks.ULTIMATE_MOLECULAR_ASSEMBLER_ITEM);
        }
    }
}
