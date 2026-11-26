package io.github.willianjs94.appliedtiers.client;

import appeng.client.InitScreens;
import appeng.client.gui.implementations.PatternProviderScreen;
import io.github.willianjs94.appliedtiers.AppliedTiers;
import io.github.willianjs94.appliedtiers.blockentity.AssemblerAnimation;
import io.github.willianjs94.appliedtiers.blockentity.ParallelAssemblerBlockEntity;
import io.github.willianjs94.appliedtiers.network.AssemblerAnimationPayload;
import io.github.willianjs94.appliedtiers.registry.ModBlockEntities;
import io.github.willianjs94.appliedtiers.menu.AdvancedPatternProviderMenu;
import io.github.willianjs94.appliedtiers.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = AppliedTiers.MOD_ID, value = Dist.CLIENT)
public final class ModClient {
    @SubscribeEvent
    public static void registerCraftingModels(net.neoforged.neoforge.client.event.RegisterBlockStateModels event) {
        event.registerModel(net.minecraft.resources.Identifier.fromNamespaceAndPath(AppliedTiers.MOD_ID, "crafting_cube"),
                TierCraftingModel.CODEC);
    }

    private ModClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ADVANCED_MOLECULAR_ASSEMBLER.get(),
                ParallelAssemblerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ELITE_MOLECULAR_ASSEMBLER.get(),
                ParallelAssemblerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ULTIMATE_MOLECULAR_ASSEMBLER.get(),
                ParallelAssemblerRenderer::new);
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(AssemblerAnimationPayload.TYPE, (payload, context) -> {
            var level = Minecraft.getInstance().level;
            if (level == null || context.player().level() != level
                    || !payload.dimension().equals(level.dimension().identifier())
                    || payload.speed() <= 0 || payload.speed() > AssemblerAnimation.MAX_SPEED
                    || payload.result().getCount() != 1 || !level.hasChunkAt(payload.pos())) {
                return;
            }

            if (level.getBlockEntity(payload.pos()) instanceof ParallelAssemblerBlockEntity assembler
                    && !assembler.isRemoved()) {
                assembler.recordAnimation(payload.result(), payload.speed(), level.getGameTime());
            }
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, ModMenus.ADVANCED_MOLECULAR_ASSEMBLER.get(),
                ParallelAssemblerScreen::new, "/screens/appliedtiers_advanced_molecular_assembler.json");
        InitScreens.register(event, ModMenus.ELITE_MOLECULAR_ASSEMBLER.get(),
                ParallelAssemblerScreen::new, "/screens/appliedtiers_elite_molecular_assembler.json");
        InitScreens.register(event, ModMenus.ULTIMATE_MOLECULAR_ASSEMBLER.get(),
                ParallelAssemblerScreen::new, "/screens/appliedtiers_ultimate_molecular_assembler.json");
        InitScreens.register(event,
                ModMenus.ADVANCED_PATTERN_PROVIDER.get(),
                PatternProviderScreen<AdvancedPatternProviderMenu>::new,
                "/screens/appliedtiers_advanced.json");
        InitScreens.register(event,
                ModMenus.ULTIMATE_PATTERN_PROVIDER.get(),
                PatternProviderScreen<AdvancedPatternProviderMenu>::new,
                "/screens/appliedtiers_ultimate.json");
        InitScreens.register(event,
                ModMenus.ELITE_PATTERN_PROVIDER.get(),
                PatternProviderScreen<AdvancedPatternProviderMenu>::new,
                "/screens/appliedtiers_elite.json");
    }
}
