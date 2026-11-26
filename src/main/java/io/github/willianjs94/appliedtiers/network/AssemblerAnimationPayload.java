package io.github.willianjs94.appliedtiers.network;

import io.github.willianjs94.appliedtiers.AppliedTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record AssemblerAnimationPayload(Identifier dimension, BlockPos pos, int speed, ItemStack result)
        implements CustomPacketPayload {
    public static final Type<AssemblerAnimationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(AppliedTiers.MOD_ID, "assembler_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblerAnimationPayload> CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC, AssemblerAnimationPayload::dimension,
                    BlockPos.STREAM_CODEC, AssemblerAnimationPayload::pos,
                    ByteBufCodecs.VAR_INT, AssemblerAnimationPayload::speed,
                    ItemStack.STREAM_CODEC, AssemblerAnimationPayload::result,
                    AssemblerAnimationPayload::new);

    @Override
    public Type<AssemblerAnimationPayload> type() {
        return TYPE;
    }
}
