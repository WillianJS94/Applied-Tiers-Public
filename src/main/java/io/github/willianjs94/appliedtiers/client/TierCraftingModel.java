package io.github.willianjs94.appliedtiers.client;

import appeng.client.render.crafting.LightBakedModel;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

public record TierCraftingModel(String tier) implements CustomUnbakedBlockStateModel, ModelDebugName {
    public static final MapCodec<TierCraftingModel> CODEC = Codec.STRING.fieldOf("tier").xmap(TierCraftingModel::new, TierCraftingModel::tier);
    private Material.Baked material(ModelBaker baker, String namespace, String texture) {
        return baker.materials().get(new Material(Identifier.fromNamespaceAndPath(namespace, "block/crafting/" + texture)), this);
    }
    @Override public BlockStateModel bake(ModelBaker baker) {
        return new LightBakedModel(material(baker, "ae2", "ring_corner"),
                material(baker, "ae2", "ring_side_hor"), material(baker, "ae2", "ring_side_ver"),
                material(baker, "ae2", "light_base"), material(baker, "appliedtiers", tier + "/accelerator_light"));
    }
    @Override public void resolveDependencies(Resolver resolver) {}
    @Override public MapCodec<TierCraftingModel> codec() { return CODEC; }
    @Override public String debugName() { return "Applied Tiers crafting cube " + tier; }
}
