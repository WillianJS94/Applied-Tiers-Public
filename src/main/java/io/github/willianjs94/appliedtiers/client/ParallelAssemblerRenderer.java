/*
 * Rendering adapted from Applied Energistics 2 v26.1.14-beta.
 * Copyright (c) 2013-2014, AlgorithmX2 and AE2 contributors.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See META-INF/AE2-LICENSE.txt.
 */
package io.github.willianjs94.appliedtiers.client;

import com.mojang.blaze3d.vertex.PoseStack;

import appeng.core.AEConfig;
import appeng.core.particles.ParticleTypes;
import io.github.willianjs94.appliedtiers.blockentity.ParallelAssemblerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ParallelAssemblerRenderer
        implements BlockEntityRenderer<ParallelAssemblerBlockEntity, ParallelAssemblerRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        private final ItemStackRenderState item = new ItemStackRenderState();
        private boolean blockItem;
    }

    private final ItemModelResolver itemModelResolver;

    public ParallelAssemblerRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ParallelAssemblerBlockEntity assembler, State state, float partialTicks,
            Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(assembler, state, partialTicks, cameraPos, overlay);
        state.item.clear();

        var animation = assembler.animation();
        var level = assembler.getLevel();
        if (animation == null || level == null || assembler.isRemoved()) {
            return;
        }
        long now = level.getGameTime();
        var visual = animation.current(now);
        if (visual == null) {
            return;
        }

        var pos = assembler.getBlockPos();
        var stack = visual.item();
        if (AEConfig.instance().isEnableEffects() && !Minecraft.getInstance().isPaused()
                && animation.takeParticleBurst(now)) {
            var particle = new ItemParticleOption(ParticleTypes.CRAFTING, ItemStackTemplate.fromNonEmptyStack(stack));
            for (int i = 0; i < (visual.speed() + 4) / 5; i++) {
                level.addParticle(particle, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
            }
        }

        itemModelResolver.updateForTopItem(state.item, stack, ItemDisplayContext.FIXED,
                level, null, (int) pos.asLong());
        state.blockItem = stack.getItem() instanceof BlockItem;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector nodes, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, state.blockItem ? 0.3 : 0.2, 0.5);
        state.item.submit(poseStack, nodes, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
