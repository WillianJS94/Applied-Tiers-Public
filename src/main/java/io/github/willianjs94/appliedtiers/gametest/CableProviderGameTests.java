package io.github.willianjs94.appliedtiers.gametest;

import java.util.List;

import com.mojang.serialization.MapCodec;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.AEColor;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.me.helpers.BaseActionSource;
import appeng.server.testplots.CraftingPatternHelper;
import appeng.server.testworld.PlotTestHelper;
import appeng.server.testworld.TestCraftingJob;
import appeng.util.Platform;
import io.github.willianjs94.appliedtiers.AppliedTiers;
import io.github.willianjs94.appliedtiers.menu.AdvancedPatternProviderMenu;
import io.github.willianjs94.appliedtiers.part.TierPatternProviderItem;
import io.github.willianjs94.appliedtiers.part.TierPatternProviderPart;
import io.github.willianjs94.appliedtiers.registry.ModBlocks;
import io.github.willianjs94.appliedtiers.registry.ModMenus;
import io.github.willianjs94.appliedtiers.registry.ModParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = AppliedTiers.MOD_ID)
public final class CableProviderGameTests {
    private CableProviderGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = Holder.direct(new TestEnvironmentDefinition.AllOf());
        var data = new TestData<>(environment, id("empty_provider_test"), 400, 0,
                true, Rotation.NONE, false, 1, 1, false, 1);
        event.registerTest(id("advanced_cable_provider"),
                new ProviderTest(data, ModParts.ADVANCED.get(), ModBlocks.ADVANCED_PATTERN_PROVIDER.get(),
                        ModMenus.ADVANCED_PATTERN_PROVIDER.get(), 18));
        event.registerTest(id("elite_cable_provider"),
                new ProviderTest(data, ModParts.ELITE.get(), ModBlocks.ELITE_PATTERN_PROVIDER.get(),
                        ModMenus.ELITE_PATTERN_PROVIDER.get(), 36));
        event.registerTest(id("ultimate_cable_provider"),
                new ProviderTest(data, ModParts.ULTIMATE.get(), ModBlocks.ULTIMATE_PATTERN_PROVIDER.get(),
                        ModMenus.ULTIMATE_PATTERN_PROVIDER.get(), 72));

        for (var assembler : List.of(ModBlocks.ELITE_MOLECULAR_ASSEMBLER, ModBlocks.ULTIMATE_MOLECULAR_ASSEMBLER)) {
            String suffix = assembler.getId().getPath();
            event.registerTest(id("advanced_cable_provider_" + suffix),
                    new ProviderTest(data, ModParts.ADVANCED.get(), ModBlocks.ADVANCED_PATTERN_PROVIDER.get(),
                            ModMenus.ADVANCED_PATTERN_PROVIDER.get(), 18, assembler.get()));
            event.registerTest(id("elite_cable_provider_" + suffix),
                    new ProviderTest(data, ModParts.ELITE.get(), ModBlocks.ELITE_PATTERN_PROVIDER.get(),
                            ModMenus.ELITE_PATTERN_PROVIDER.get(), 36, assembler.get()));
            event.registerTest(id("ultimate_cable_provider_" + suffix),
                    new ProviderTest(data, ModParts.ULTIMATE.get(), ModBlocks.ULTIMATE_PATTERN_PROVIDER.get(),
                            ModMenus.ULTIMATE_PATTERN_PROVIDER.get(), 72, assembler.get()));
        }
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(AppliedTiers.MOD_ID, name);
    }

    private static final class ProviderTest extends GameTestInstance {
        private final TierPatternProviderItem item;
        private final Block block;
        private final MenuType<AdvancedPatternProviderMenu> menu;
        private final int capacity;
        private final Block assembler;

        private ProviderTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, TierPatternProviderItem item,
                Block block, MenuType<AdvancedPatternProviderMenu> menu, int capacity) {
            this(data, item, block, menu, capacity, AEBlocks.MOLECULAR_ASSEMBLER.block());
        }

        private ProviderTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, TierPatternProviderItem item,
                Block block, MenuType<AdvancedPatternProviderMenu> menu, int capacity, Block assembler) {
            super(data);
            this.assembler = assembler;
            this.item = item;
            this.block = block;
            this.menu = menu;
            this.capacity = capacity;
        }

        @Override
        public void run(GameTestHelper gameTestHelper) {
            var helper = new PlotTestHelper(new BlockPos(2, 1, 2), gameTestHelper.testInfo);
            var level = helper.getLevel();
            var player = Platform.getFakePlayer(level, null);
            var origin = BlockPos.ZERO;
            var cablePos = helper.absolutePos(origin);
            var face = Direction.EAST;

            helper.setBlock(origin.relative(face), assembler);
            helper.setBlock(origin.west(), AEBlocks.CRAFTING_STORAGE_1K.block());
            helper.setBlock(origin.above(), AEBlocks.CREATIVE_ENERGY_CELL.block());
            helper.setBlock(origin.north(), AEBlocks.DRIVE.block());
            var drive = helper.getBlockEntity(origin.north(), DriveBlockEntity.class);
            drive.getInternalInventory().addItems(AEItems.ITEM_CELL_1K.stack());

            var cable = AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT);
            helper.check(PartHelper.setPart(level, cablePos, null, player, cable) != null, "Could not place the ME cable");
            // The occupied cable face is for crafting I/O, so power the assembler
            // through another face, as with AE2's own cable Pattern Provider.
            helper.check(PartHelper.setPart(level, helper.absolutePos(origin.below()), null, player, cable) != null,
                    "Could not place the assembler power branch");
            helper.check(PartHelper.setPart(level, helper.absolutePos(origin.below().relative(face)), null, player, cable) != null,
                    "Could not connect power to the assembler");
            var part = PartHelper.setPart(level, cablePos, face, player, item);
            helper.check(part != null, "Could not place " + item);
            helper.check(part.getLogic().getPatternInv().size() == capacity, "Wrong cable pattern capacity");
            helper.check(level.getCapability(AECapabilities.GENERIC_INTERNAL_INV, cablePos, face)
                    == part.getLogic().getReturnInv(), "Cable return capability did not reach the part");
            helper.check(level.getCapability(Capabilities.Item.BLOCK, cablePos, face) != null,
                    "Cable return inventory has no sided item adapter");
            helper.check(level.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, cablePos, null) != null,
                    "Cable host did not expose its ME node");
            helper.check(new AdvancedPatternProviderMenu(menu, 0, player.getInventory(), part).slots.size() == 45 + capacity,
                    "The shared menu did not expose all cable slots");

            var blockPos = new BlockPos(2, 0, 0);
            helper.setBlock(blockPos, block);
            var blockEntity = helper.getBlockEntity(blockPos, PatternProviderBlockEntity.class);
            helper.check(blockEntity.getLogic().getPatternInv().size() == capacity, "Block capacity regressed");
            helper.check(level.getCapability(AECapabilities.GENERIC_INTERNAL_INV, helper.absolutePos(blockPos), face)
                    == blockEntity.getLogic().getReturnInv(), "Block return capability regressed");

            checkConversion(helper, new ItemStack(block), new ItemStack(item));
            checkConversion(helper, new ItemStack(item), new ItemStack(block));

            var pattern = CraftingPatternHelper.encodeCraftingPattern(level, new Object[] {
                    Items.OAK_PLANKS, null, null,
                    Items.OAK_PLANKS, null, null,
                    null, null, null
            }, false, false);
            part.getLogic().getPatternInv().setItemDirect(capacity - 1, pattern);
            part.getLogic().updatePatterns();
            part.setPriority(17);
            var host = level.getBlockEntity(cablePos);
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            host.saveWithId(output);
            var savedHost = BlockEntity.loadStatic(cablePos, host.getBlockState(), output.buildResult(), level.registryAccess());
            helper.check(savedHost instanceof IPartHost, "Cable host failed to deserialize");
            var savedPart = ((IPartHost) savedHost).getPart(face);
            helper.check(savedPart instanceof TierPatternProviderPart,
                    "Cable provider failed to deserialize");
            var restored = (TierPatternProviderPart) savedPart;
            helper.check(restored.getPartItem() == item && restored.getPriority() == 17
                    && restored.getLogic().getPatternInv().getStackInSlot(capacity - 1).is(pattern.getItem()),
                    "Cable part lost its tier, priority or pattern on save/reload");

            var job = new TestCraftingJob(helper, origin, AEItemKey.of(Items.STICK), 4);
            helper.startSequence()
                    .thenWaitUntil(() -> {
                        helper.assertGridOnline(origin);
                        helper.check(part.getMainNode().isActive(), "Cable part has no active channel");
                        helper.check(helper.getGrid(origin.relative(face)) == helper.getGrid(origin),
                                "Assembler is not on the powered ME grid");
                    })
                    .thenExecute(() -> {
                        var inv = helper.getGrid(origin).getStorageService().getInventory();
                        helper.check(inv.insert(AEItemKey.of(Items.OAK_PLANKS), 2, Actionable.MODULATE,
                                new BaseActionSource()) == 2, "Could not load the test ingredients");
                    })
                    .thenWaitUntil(job::tickUntilStarted)
                    .thenWaitUntil(() -> helper.check(helper.getGrid(origin).getStorageService().getInventory()
                            .getAvailableStacks().get(AEItemKey.of(Items.STICK)) == 4,
                            "Assembler did not return exactly one batch through the cable provider"))
                    .thenSucceed();
        }

        private static void checkConversion(PlotTestHelper helper, ItemStack ingredient, ItemStack expected) {
            var level = helper.getLevel();
            var input = CraftingInput.of(1, 1, List.of(ingredient));
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                    .orElseThrow(() -> helper.assertionException("Missing conversion recipe for " + ingredient));
            var result = recipe.value().assemble(input);
            helper.check(result.is(expected.getItem()) && result.getCount() == 1,
                    "Unexpected conversion result for " + ingredient);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return MapCodec.unit(this);
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Applied Tiers cable integration");
        }
    }
}
