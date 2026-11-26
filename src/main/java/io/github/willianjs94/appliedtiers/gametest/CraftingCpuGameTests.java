package io.github.willianjs94.appliedtiers.gametest;

import java.util.List;
import com.mojang.serialization.MapCodec;
import appeng.api.AECapabilities;
import appeng.block.crafting.AbstractCraftingUnitBlock;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.server.testworld.PlotTestHelper;
import io.github.willianjs94.appliedtiers.AppliedTiers;
import io.github.willianjs94.appliedtiers.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = AppliedTiers.MOD_ID)
public final class CraftingCpuGameTests {
    private CraftingCpuGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = Holder.<TestEnvironmentDefinition<?>>direct(new TestEnvironmentDefinition.AllOf());
        var data = new TestData<>(environment, id("empty_provider_test"), 400, 0,
                true, Rotation.NONE, false, 1, 1, false, 1);
        var advanced = ModBlocks.ADVANCED_CRAFTING_ACCELERATOR.get();
        var elite = ModBlocks.ELITE_CRAFTING_ACCELERATOR.get();
        var ultimate = ModBlocks.ULTIMATE_CRAFTING_ACCELERATOR.get();
        event.registerTest(id("cpu_advanced"), new CpuTest(data, List.of(advanced), List.of(2), true));
        event.registerTest(id("cpu_elite"), new CpuTest(data, List.of(elite), List.of(3), true));
        event.registerTest(id("cpu_ultimate"), new CpuTest(data, List.of(ultimate), List.of(4), true));
        event.registerTest(id("cpu_mixed"), new CpuTest(data, List.of(advanced, elite, ultimate), List.of(2, 3, 4), true));
        event.registerTest(id("cpu_mixed_original"), new CpuTest(data,
                List.of(advanced, elite, ultimate, AEBlocks.CRAFTING_ACCELERATOR.block()), List.of(2, 3, 4, 1), true));
        event.registerTest(id("cpu_without_storage"), new CpuTest(data,
                List.of(advanced, elite, ultimate), List.of(2, 3, 4), false));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(AppliedTiers.MOD_ID, name);
    }

    private static final class CpuTest extends GameTestInstance {
        private final List<Block> blocks;
        private final List<Integer> counts;
        private final boolean storage;

        CpuTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, List<Block> blocks,
                List<Integer> counts, boolean storage) {
            super(data);
            this.blocks = blocks;
            this.counts = counts;
            this.storage = storage;
        }

        @Override
        public void run(GameTestHelper raw) {
            var helper = new PlotTestHelper(new BlockPos(2, 1, 2), raw.testInfo);
            var origin = BlockPos.ZERO;
            var storagePos = origin.west(2);
            var energyPos = storagePos.above();
            if (storage) helper.setBlock(storagePos, AEBlocks.CRAFTING_STORAGE_1K.block());
            helper.setBlock(energyPos, AEBlocks.CREATIVE_ENERGY_CELL.block());
            for (int i = 0; i < blocks.size(); i++) {
                helper.setBlock(storagePos.east(i + 1), blocks.get(i));
            }
            var sequence = helper.startSequence().thenWaitUntil(() -> {
                for (int i = 0; i < blocks.size(); i++) {
                    var pos = storagePos.east(i + 1);
                    var entity = helper.getBlockEntity(pos, CraftingBlockEntity.class);
                    helper.check(entity.getMainNode().isReady(), "Node not initialized", pos);
                    helper.check(entity.getStorageBytes() == 0, "Accelerator provides storage", pos);
                    helper.check(entity.getAcceleratorThreads() == counts.get(i), "Incorrect individual coprocessor count", pos);
                    helper.check(helper.getCapability(pos, AECapabilities.IN_WORLD_GRID_NODE_HOST, null) == entity,
                            "Missing grid host capability", pos);
                    if (counts.get(i) > 1) {
                        helper.check(Math.abs(helper.getGridNode(pos).getIdlePowerUsage() - counts.get(i)) < 0.00001,
                                "Incorrect nominal idle AE/t", pos);
                    }
                }
            });
            if (!storage) {
                sequence.thenIdle(40).thenExecute(() -> {
                    for (int i = 0; i < blocks.size(); i++) {
                        var pos = storagePos.east(i + 1);
                        var entity = helper.getBlockEntity(pos, CraftingBlockEntity.class);
                        helper.check(!entity.isFormed() && entity.getCluster() == null,
                                "CPU formed without storage", pos);
                        helper.check(!entity.getBlockState().getValue(AbstractCraftingUnitBlock.FORMED),
                                "Invalid formed block state", pos);
                    }
                }).thenSucceed();
                return;
            }
            sequence.thenWaitUntil(() -> {
                helper.assertGridOnline(storagePos);
                var storageEntity = helper.getBlockEntity(storagePos, CraftingBlockEntity.class);
                var cluster = storageEntity.getCluster();
                helper.check(cluster != null && cluster.isActive(), "CPU not formed and active");
                helper.check(cluster.getAvailableStorage() == 1024, "Wrong storage capacity");
                helper.check(cluster.getCoProcessors() == counts.stream().mapToInt(Integer::intValue).sum(),
                        "Incorrect aggregate coprocessor count");
                helper.check(helper.getGrid(storagePos).getCraftingService().getCpus().contains(cluster),
                        "CPU not discovered by crafting service");
                for (int i = 0; i < blocks.size(); i++) {
                    var pos = storagePos.east(i + 1);
                    var entity = helper.getBlockEntity(pos, CraftingBlockEntity.class);
                    helper.check(entity.isFormed() && entity.getCluster() == cluster && entity.isActive(),
                            "Accelerator not in the active CPU", pos);
                    helper.check(entity.getBlockState().getValue(AbstractCraftingUnitBlock.FORMED)
                            && entity.getBlockState().getValue(AbstractCraftingUnitBlock.POWERED),
                            "Wrong formed/powered block state", pos);
                }
            }).thenExecute(() -> helper.setBlock(storagePos, Blocks.AIR))
              .thenWaitUntil(() -> {
                  for (int i = 0; i < blocks.size(); i++) {
                      var pos = storagePos.east(i + 1);
                      var entity = helper.getBlockEntity(pos, CraftingBlockEntity.class);
                      helper.check(!entity.isFormed() && entity.getCluster() == null,
                              "CPU remains formed after removing storage", pos);
                  }
              }).thenSucceed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() { return MapCodec.unit(this); }

        @Override
        protected MutableComponent typeDescription() { return Component.literal("Applied Tiers crafting CPU formation and idle power"); }
    }
}
