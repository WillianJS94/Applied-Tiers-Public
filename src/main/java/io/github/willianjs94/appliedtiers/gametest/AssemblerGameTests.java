package io.github.willianjs94.appliedtiers.gametest;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import appeng.api.AECapabilities;
import appeng.menu.SlotSemantics;
import appeng.api.config.Actionable;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.blockentity.networking.EnergyCellBlockEntity;
import appeng.me.helpers.BaseActionSource;
import appeng.server.testworld.TestCraftingJob;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.server.testplots.CraftingPatternHelper;
import appeng.server.testworld.PlotTestHelper;
import appeng.util.Platform;
import io.github.willianjs94.appliedtiers.AppliedTiers;
import io.github.willianjs94.appliedtiers.blockentity.ParallelAssemblerBlockEntity;
import io.github.willianjs94.appliedtiers.blockentity.AssemblerAnimation;
import io.github.willianjs94.appliedtiers.blockentity.AssemblerProcessingMode;
import io.github.willianjs94.appliedtiers.menu.ParallelAssemblerMenu;
import io.github.willianjs94.appliedtiers.registry.ModBlocks;
import io.github.willianjs94.appliedtiers.registry.ModMenus;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = AppliedTiers.MOD_ID)
public final class AssemblerGameTests {
    private AssemblerGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = Holder.<TestEnvironmentDefinition<?>>direct(new TestEnvironmentDefinition.AllOf());
        var data = new TestData<>(environment, id("empty_provider_test"), 400, 0,
                true, Rotation.NONE, false, 1, 1, false, 1);
        event.registerTest(id("assembler_animation_queue"), new AssemblerTest(data, "animation"));
        event.registerTest(id("assembler_parallel_routes"), new AssemblerTest(data, "routes"));
        event.registerTest(id("assembler_filtered_standalone"), new AssemblerTest(data, "standalone"));
        event.registerTest(id("assembler_modes_power_remainders"), new AssemblerTest(data, "formats"));
        event.registerTest(id("assembler_cpu_parallel_dispatch"), new AssemblerTest(data, "cpu"));
        event.registerTest(id("assembler_acceleration_energy"), new AssemblerTest(data, "energy"));
        event.registerTest(id("assembler_processing_modes"), new AssemblerTest(data, "modes"));
        event.registerTest(id("assembler_standalone_all_modes"), new AssemblerTest(data, "standalone_modes"));
        event.registerTest(id("assembler_original_provider_exclusive"), new AssemblerTest(data, "cpu_original"));
        for (int capacity = 2; capacity <= 4; capacity++) {
            event.registerTest(id("assembler_tier_cpu_" + capacity), new AssemblerTest(data, "cpu_tier", capacity));
            event.registerTest(id("assembler_tier_cpu_original_provider_" + capacity), new AssemblerTest(data, "cpu_tier_original", capacity));
        }
        for (int capacity = 3; capacity <= 4; capacity++) {
            String tier = capacity == 3 ? "elite" : "ultimate";
            for (var scenario : List.of("routes", "cpu", "cpu_cards", "energy", "modes", "standalone_modes", "formats", "standalone")) {
                event.registerTest(id("assembler_" + tier + "_" + scenario), new AssemblerTest(data, scenario, capacity));
            }
        }
    }

    private static void testAnimation(PlotTestHelper helper) {
        var animation = new AssemblerAnimation();
        var stick = new ItemStack(Items.STICK, 4);
        var block = new ItemStack(Items.CRAFTING_TABLE);
        animation.accept(stick, 0, 0);
        helper.check(animation.current(0) == null, "Zero animation speed must be rejected");
        animation.accept(stick, 50, 1);
        animation.accept(stick, 50, 1);
        animation.accept(block, 10, 1);
        animation.accept(block, 10, 1);
        animation.accept(stick, 50, 4);
        helper.check(animation.current(4).item().is(Items.STICK)
                && animation.current(4).item().getCount() == 1, "Expected one centered result");
        helper.check(animation.takeParticleBurst(4) && !animation.takeParticleBurst(5),
                "Particle cadence must be limited independently of result changes");
        helper.check(animation.current(5).item().is(Items.CRAFTING_TABLE),
                "A repeated first result must not delay a distinct result");
        helper.check(animation.current(16).item().is(Items.CRAFTING_TABLE), "Expected AE2 display duration");
        helper.check(animation.current(17) == null, "Duplicate results must not be queued");
        animation.accept(stick, 10, 20);
        animation.accept(block, 10, 20);
        helper.check(animation.current(68) == null, "Stale queued visuals must expire off-screen");
        var results = List.of(Items.STICK, Items.CRAFTING_TABLE, Items.STONE_BRICKS, Items.OAK_BUTTON);
        for (var result : results) {
            animation.accept(new ItemStack(result), 50, 80);
        }
        for (int unit = 0; unit < results.size(); unit++) {
            helper.check(animation.current(80 + 4 * unit).item().is(results.get(unit)),
                    "Animation queue starved a distinct result at maximum capacity");
        }
        helper.check(animation.current(96) == null, "Four simultaneous results did not expire");
        helper.startSequence().thenSucceed();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AppliedTiers.MOD_ID, path);
    }

    private static final class AssemblerTest extends GameTestInstance {
        private final String scenario;
        private final int capacity;

        AssemblerTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, String scenario) {
            this(data, scenario, 2);
        }

        AssemblerTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, String scenario, int capacity) {
            super(data);
            this.scenario = scenario;
            this.capacity = capacity;
        }

        @Override
        public void run(GameTestHelper raw) {
            var helper = new PlotTestHelper(new BlockPos(2, 1, 2), raw.testInfo);
            if (scenario.equals("animation")) {
                testAnimation(helper);
                return;
            }
            var pos = BlockPos.ZERO;
            var level = helper.getLevel();
            var block = switch (capacity) {
                case 3 -> ModBlocks.ELITE_MOLECULAR_ASSEMBLER.get();
                case 4 -> ModBlocks.ULTIMATE_MOLECULAR_ASSEMBLER.get();
                default -> ModBlocks.ADVANCED_MOLECULAR_ASSEMBLER.get();
            };
            var menuType = assemblerMenuType(capacity);
            helper.setBlock(pos, block);
            if (scenario.equals("energy")) {
                helper.setBlock(pos.above(), AEBlocks.ENERGY_CELL.block());
            } else if (!scenario.equals("formats")) {
                helper.setBlock(pos.above(), AEBlocks.CREATIVE_ENERGY_CELL.block());
            }
            helper.setBlock(pos.west(), Blocks.CHEST);
            helper.setBlock(pos.east(), Blocks.CHEST);
            var machine = helper.getBlockEntity(pos, ParallelAssemblerBlockEntity.class);
            var west = helper.getBlockEntity(pos.west(), ChestBlockEntity.class);
            var east = helper.getBlockEntity(pos.east(), ChestBlockEntity.class);
            var sticks = CraftingPatternHelper.encodeCraftingPattern(level, new Object[] {
                    Items.OAK_PLANKS, null, null, Items.OAK_PLANKS, null, null, null, null, null
            }, false, false);
            var buttons = CraftingPatternHelper.encodeCraftingPattern(level, new Object[] {
                    Items.OAK_PLANKS, null, null, null, null, null, null, null, null
            }, false, false);
            var stickPlan = PatternDetailsHelper.decodePattern(sticks, level);
            var buttonPlan = PatternDetailsHelper.decodePattern(buttons, level);
            var location = helper.absolutePos(pos);
            helper.check(level.getCapability(AECapabilities.CRAFTING_MACHINE, location, Direction.WEST) == machine,
                    "Crafting machine capability missing");
            helper.check(level.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, location, null) == machine,
                    "ME node capability missing");
            helper.check(machine.getCraftingMachineInfo().icon().equals(AEItemKey.of(block)),
                    "Terminal icon does not identify the tier");
            var player = Platform.getFakePlayer(level, null);
            var menu = new ParallelAssemblerMenu(menuType, 0,
                    player.getInventory(), machine);
            helper.check(machine.capacity() == capacity && menu.slots.size() == 42 + 10 * capacity,
                    "Unexpected tier capacity or slot count");
            checkRecipe(helper, capacity, block.asItem());

            if (scenario.equals("energy")) {
                checkAcceleration(helper, machine, stickPlan);
                return;
            }
            if (scenario.equals("cpu") || scenario.equals("cpu_original") || scenario.equals("cpu_cards") || scenario.startsWith("cpu_tier")) {
                checkCpuDispatch(helper, machine, sticks, scenario.equals("cpu_original") || scenario.equals("cpu_tier_original") || capacity == 3 && scenario.equals("cpu"),
                        scenario.equals("cpu_cards") || scenario.startsWith("cpu_tier"), scenario.startsWith("cpu_tier"));
                return;
            }
            if (scenario.equals("formats")) {
                checkFormatsAndPower(helper, machine, west, east);
                return;
            }

            if (scenario.equals("modes")) {
                checkModes(helper, machine, menu, west, east);
                return;
            }
            if (scenario.equals("standalone_modes")) {
                checkStandaloneModes(helper, machine, menu, sticks, west, east);
                return;
            }
            if (scenario.equals("standalone")) {
                var port = InternalInventory.wrapExternal(level, location, null);
                helper.check(port != null, "Unsided item capability missing");
                var planks = new ItemStack(Items.OAK_PLANKS, 2);
                helper.check(port.addItems(planks.copy()).getCount() == 2,
                        "Unsided insertion accepted inputs in ME mode");
                machine.patternInventory().setItemDirect(0, sticks);
                helper.check(!machine.acceptsPlans(), "Standalone mode accepted ME plans");
                helper.check(port.addItems(new ItemStack(Items.DIRT)).is(Items.DIRT), "Invalid item entered grid");
                helper.check(port.addItems(planks.copy()).isEmpty(), "Valid standalone inputs were rejected");
                helper.check(!machine.canChangePattern(), "Mode transition could discard staged ingredients");
                var speed = AEItems.SPEED_CARD.stack(5);
                helper.check(machine.getUpgrades().addItems(speed).isEmpty(), "Five shared cards were not accepted");
                helper.check(!machine.getUpgrades().addItems(AEItems.SPEED_CARD.stack()).isEmpty(),
                        "Sixth card was accepted");
                helper.startSequence()
                        .thenWaitUntil(() -> helper.check(machine.progress(0) == 50, "Standalone unit did not advance"))
                        .thenExecute(() -> {
                            var restored = reload(helper, machine);
                            helper.check(restored.progress(0) == 50 && !restored.patternInventory().isEmpty()
                                    && restored.getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD) == 5,
                                    "Reload lost standalone progress, pattern or upgrades");
                            installReloaded(helper, restored);
                        })
                        .thenWaitUntil(() -> helper.check(count(west, Items.STICK) + count(east, Items.STICK) == 4,
                                "Reloaded standalone unit did not eject exactly one output"))
                        .thenSucceed();
                return;
            }

            // Intentionally block one return face while the other unit remains free.
            for (int i = 0; i < west.getContainerSize(); i++) {
                west.setItem(i, new ItemStack(Items.DIRT, 64));
            }
            var wrong = counters(stickPlan);
            var key = AEItemKey.of(Items.OAK_PLANKS);
            wrong[0].add(key, 1);
            helper.check(!machine.pushPattern(stickPlan, wrong, Direction.WEST) && wrong[0].get(key) == 3,
                    "Rejected push mutated its counters");
            helper.check(machine.pushPattern(stickPlan, counters(stickPlan), Direction.WEST), "First push rejected");
            helper.check(machine.pushPattern(buttonPlan, counters(buttonPlan), Direction.EAST), "Second push rejected");
            for (int unit = 2; unit < capacity; unit++) {
                helper.check(machine.pushPattern(buttonPlan, counters(buttonPlan), Direction.EAST),
                        "Tier rejected work despite a free unit");
            }
            var third = counters(stickPlan);
            helper.check(!machine.pushPattern(stickPlan, third, Direction.WEST) && third[0].get(key) == 2,
                    "Push beyond capacity consumed ingredients");
            helper.check(!machine.canChangePattern(), "Pattern can be changed while dispatched");
            var loaded = reload(helper, machine);
            helper.check(!loaded.acceptsPlans(), "Reload lost the full machine state");
            for (int unit = 0; unit < capacity; unit++) {
                helper.check(!loaded.input(unit).isEmpty(), "Staged unit was not saved: " + unit);
            }
            helper.startSequence()
                    .thenWaitUntil(() -> helper.check(count(east, Items.OAK_BUTTON) == capacity - 1
                            && machine.output(0).getStackInSlot(0).getCount() == 4,
                            "Blocked unit prevented the other unit returning its result"))
                    .thenExecute(() -> {
                        var restored = reload(helper, machine);
                        helper.check(restored.output(0).getStackInSlot(0).getCount() == 4
                                && restored.input(0).isEmpty(), "Blocked result or completion phase was lost");
                        var drops = new ArrayList<ItemStack>();
                        machine.addAdditionalDrops(level, location, drops);
                        helper.check(drops.stream().filter(x -> x.is(Items.STICK)).mapToInt(ItemStack::getCount).sum() == 4,
                                "Blocked result missing or duplicated in drops");
                        installReloaded(helper, restored);
                        west.setItem(0, new ItemStack(Items.STICK, 62));
                    })
                    .thenWaitUntil(() -> helper.check(count(west, Items.STICK) == 64
                            && currentMachine(helper).output(0).getStackInSlot(0).getCount() == 2,
                            "Partially accepted output was not reduced to the unsent remainder"))
                    .thenExecute(west::clearContent)
                    .thenWaitUntil(() -> helper.check(count(west, Items.STICK) == 2 && currentMachine(helper).canChangePattern(),
                            "Blocked result did not return exactly once"))
                    .thenExecute(() -> {
                        var current = currentMachine(helper);
                        helper.check(current.pushPattern(stickPlan, counters(stickPlan), Direction.WEST)
                                && current.pushPattern(stickPlan, counters(stickPlan), Direction.EAST),
                                "Two equal plans could not run concurrently");
                    })
                    .thenWaitUntil(() -> helper.check(count(west, Items.STICK) == 6
                            && count(east, Items.STICK) == 4, "Equal plans did not route to each origin"))
                    .thenSucceed();
        }

        private static void checkModes(PlotTestHelper helper, ParallelAssemblerBlockEntity machine,
                ParallelAssemblerMenu menu, ChestBlockEntity west, ChestBlockEntity east) {
            var level = helper.getLevel();
            var oak = PatternDetailsHelper.decodePattern(CraftingPatternHelper.encodeCraftingPattern(level, new Object[] {
                    Items.OAK_PLANKS, null, null, Items.OAK_PLANKS, null, null, null, null, null
            }, true, false), level);
            var birch = PatternDetailsHelper.decodePattern(CraftingPatternHelper.encodeCraftingPattern(level, new Object[] {
                    Items.BIRCH_PLANKS, null, null, Items.BIRCH_PLANKS, null, null, null, null, null
            }, true, false), level);
            helper.check(oak.getPrimaryOutput().equals(birch.getPrimaryOutput())
                    && !oak.getDefinition().equals(birch.getDefinition()), "Need two identities with the same output");
            helper.check(machine.processingMode() == AssemblerProcessingMode.AUTOMATIC, "Wrong default mode");

            for (int i = 0; i < west.getContainerSize(); i++) {
                west.setItem(i, new ItemStack(Items.DIRT, 64));
            }
            helper.check(machine.pushPattern(oak, counters(oak), Direction.WEST)
                    && machine.pushPattern(birch, counters(birch), Direction.EAST), "Automatic rejected distinct plans");
            var player = Platform.getFakePlayer(level, null);
            var savedMenu = player.containerMenu;
            player.setPos(machine.getBlockPos().getX() + 0.5, machine.getBlockPos().getY(), machine.getBlockPos().getZ() + 0.5);
            menu.receiveClientAction("CycleAssemblerMode", new byte[0]);
            helper.check(machine.processingMode() == AssemblerProcessingMode.AUTOMATIC, "Inactive menu changed mode");
            player.containerMenu = menu;
            try {
                menu.receiveClientAction("CycleAssemblerMode", new byte[0]);
                menu.broadcastChanges();
                var observer = new ParallelAssemblerMenu(assemblerMenuType(machine.capacity()),
                        1, player.getInventory(), machine);
                observer.broadcastChanges();
                helper.check(menu.processingMode == AssemblerProcessingMode.EXCLUSIVE && menu.processingModeTransitioning
                        && observer.processingMode == menu.processingMode && observer.processingModeTransitioning,
                        "Server menu action and observers disagreed");
            } finally {
                player.containerMenu = savedMenu;
            }
            var persisted = reload(helper, machine);
            helper.check(persisted.processingMode() == AssemblerProcessingMode.EXCLUSIVE
                    && persisted.processingModeTransitioning() && persisted.acceptsPlans() == machine.acceptsPlans(),
                    "Saved mode or occupied identities did not survive reload");
            var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            machine.saveWithId(saved);
            var oldTag = saved.buildResult();
            oldTag.remove("processingMode");
            var legacy = (ParallelAssemblerBlockEntity) BlockEntity.loadStatic(
                    machine.getBlockPos(), machine.getBlockState(), oldTag, level.registryAccess());
            helper.check(legacy.processingMode() == AssemblerProcessingMode.AUTOMATIC, "Old saves did not default");
            oldTag.putString("processingMode", "unknown");
            var invalid = (ParallelAssemblerBlockEntity) BlockEntity.loadStatic(
                    machine.getBlockPos(), machine.getBlockState(), oldTag, level.registryAccess());
            helper.check(invalid.processingMode() == AssemblerProcessingMode.AUTOMATIC, "Invalid saved mode did not default");

            helper.startSequence()
                    .thenWaitUntil(() -> helper.check(count(east, Items.STICK) == 4
                            && machine.output(0).getStackInSlot(0).getCount() == 4 && machine.acceptsPlans(),
                            "Changing mode lost active items or blocked the independent return"))
                    .thenExecute(() -> {
                        helper.check(!machine.processingModeTransitioning(), "Drain did not clear transition");
                        reject(helper, machine, birch, "Exclusive treated equal output as equal identity");
                        var substitutes = new KeyCounter[] { new KeyCounter() };
                        substitutes[0].add(AEItemKey.of(Items.BIRCH_PLANKS), 2);
                        helper.check(machine.pushPattern(oak, substitutes, Direction.EAST) && substitutes[0].isEmpty(),
                                "Exclusive rejected a substitution for the same coded plan");
                        machine.cycleProcessingMode();
                        helper.check(machine.processingMode() == AssemblerProcessingMode.DIVERSIFIED
                                && machine.processingModeTransitioning(), "Existing matching jobs did not survive mode switch");
                    })
                    .thenWaitUntil(() -> helper.check(count(east, Items.STICK) == 8 && machine.acceptsPlans(),
                            "Switched job did not finish"))
                    .thenExecute(() -> {
                        reject(helper, machine, oak, "Diversified admitted the still blocked identity");
                        helper.check(machine.pushPattern(birch, counters(birch), Direction.EAST),
                                "Diversified rejected a distinct identity with the same output");
                    })
                    .thenWaitUntil(() -> helper.check(count(east, Items.STICK) == 12, "Distinct job did not finish"))
                    .thenExecute(() -> west.clearContent())
                    .thenWaitUntil(() -> helper.check(count(west, Items.STICK) == 4 && machine.canChangePattern(),
                            "Original blocked output did not return exactly once"))
                    .thenExecute(() -> {
                        helper.check(machine.pushPattern(oak, counters(oak), Direction.WEST),
                                "Diversified did not release the reservation after full return");
                        reject(helper, machine, oak, "Diversified admitted a concurrent duplicate");
                        machine.cycleProcessingMode();
                        helper.check(machine.processingMode() == AssemblerProcessingMode.AUTOMATIC, "Cycle did not wrap");
                    })
                    .thenWaitUntil(() -> helper.check(count(west, Items.STICK) == 8 && machine.canChangePattern(),
                            "Final result was lost or duplicated"))
                    .thenSucceed();
        }

        private static void reject(PlotTestHelper helper, ParallelAssemblerBlockEntity machine,
                IPatternDetails pattern, String reason) {
            var inputs = counters(pattern);
            var key = inputs[0].getFirstKey();
            long before = inputs[0].get(key);
            helper.check(!machine.pushPattern(pattern, inputs, Direction.EAST) && inputs[0].get(key) == before, reason);
        }

        private static void checkStandaloneModes(PlotTestHelper helper, ParallelAssemblerBlockEntity machine,
                ParallelAssemblerMenu menu, ItemStack encoded, ChestBlockEntity west, ChestBlockEntity east) {
            var port = InternalInventory.wrapExternal(helper.getLevel(), machine.getBlockPos(), null);
            var player = Platform.getFakePlayer(helper.getLevel(), null);
            var pattern = menu.getSlots(SlotSemantics.ENCODED_PATTERN).getFirst();
            var hotbar = menu.getSlots(SlotSemantics.PLAYER_HOTBAR).getFirst();
            helper.check(menu.getSlots(SlotSemantics.ENCODED_PATTERN).size() == 1
                    && menu.getSlots(SlotSemantics.UPGRADE).size() == 5, "Pattern mixed with upgrades");
            var prior = hotbar.getItem().copy();
            hotbar.set(encoded.copy());
            try {
                menu.quickMoveStack(player, hotbar.index);
                helper.check(hotbar.getItem().isEmpty() && ItemStack.matches(encoded, pattern.getItem()),
                        "Shift-click did not install the permanent pattern");
            } finally {
                hotbar.set(prior);
            }
            var sequence = helper.startSequence();
            for (int round = 1; round <= 3; round++) {
                int expected = round * 4 * machine.capacity();
                sequence.thenExecute(() -> {
                    helper.check(port.addItems(new ItemStack(Items.OAK_PLANKS, 2 * machine.capacity())).isEmpty()
                            && !pattern.mayPickup(player), "Standalone input or permanent pattern lock regressed");
                    for (int unit = 0; unit < machine.capacity(); unit++) {
                        helper.check(!machine.input(unit).isEmpty(), "Standalone unit did not receive input");
                    }
                });
                sequence.thenWaitUntil(() -> helper.check(
                        count(west, Items.STICK) + count(east, Items.STICK) == expected && machine.canChangePattern(),
                        "Standalone did not finish all matching jobs"));
                sequence.thenExecute(machine::cycleProcessingMode);
            }
            sequence.thenSucceed();
        }

        private static void checkAcceleration(PlotTestHelper helper, ParallelAssemblerBlockEntity machine,
                IPatternDetails recipe) {
            var cell = helper.getBlockEntity(BlockPos.ZERO.above(), EnergyCellBlockEntity.class);
            cell.injectAEPower(50_000, Actionable.MODULATE);
            helper.startSequence().thenWaitUntil(() -> helper.assertGridOnline(BlockPos.ZERO)).thenExecute(() -> {
                var node = machine.getMainNode().getNode();
                var energy = node.getGrid().getEnergyService();
                int[] speed = {10, 13, 17, 20, 25, 50};
                double[] multiplier = {1, 1.3, 1.7, 2, 2.5, 5};
                for (int cards = 0; cards <= 5; cards++) {
                    machine.getUpgrades().clear();
                    if (cards != 0) {
                        helper.check(machine.getUpgrades().addItems(AEItems.SPEED_CARD.stack(cards)).isEmpty(),
                                "Could not install the expected shared cards");
                    }
                    for (int unit = 0; unit < machine.capacity(); unit++) {
                        helper.check(machine.pushPattern(recipe, counters(recipe), Direction.WEST),
                                "Could not stage a unit for the acceleration check");
                    }
                    double before = energy.getStoredPower();
                    machine.tickingRequest(node, 1);
                    double used = before - energy.getStoredPower();
                    double budget = machine.capacity() * speed[cards] * multiplier[cards];
                    for (int unit = 0; unit < machine.capacity(); unit++) {
                        helper.check(machine.progress(unit) == speed[cards],
                                "Progress did not match AE2 with " + cards + " cards");
                    }
                    helper.check(Math.abs(used - budget) < 0.01,
                            "Expected " + budget + " energy with " + cards + " cards, used " + used);
                    for (int i = 0; i < 12 && !machine.canChangePattern(); i++) {
                        machine.tickingRequest(node, 1);
                    }
                    helper.check(machine.canChangePattern(), "A staged unit did not finish");
                }
                double before = energy.getStoredPower();
                machine.tickingRequest(node, 1);
                helper.check(energy.getStoredPower() == before, "Idle units consumed processing power");
            }).thenSucceed();
        }

        private static void checkCpuDispatch(PlotTestHelper helper, ParallelAssemblerBlockEntity machine,
                ItemStack encoded, boolean originalProvider, boolean installCards, boolean tierCpu) {
            var pos = BlockPos.ZERO;
            if (originalProvider) {
                machine.cycleProcessingMode();
            }
            var providerBlock = originalProvider ? AEBlocks.PATTERN_PROVIDER.block() : switch (machine.capacity()) {
                case 3 -> ModBlocks.ELITE_PATTERN_PROVIDER.get();
                case 4 -> ModBlocks.ULTIMATE_PATTERN_PROVIDER.get();
                default -> ModBlocks.ADVANCED_PATTERN_PROVIDER.get();
            };
            helper.setBlock(pos.west(), providerBlock);
            helper.setBlock(pos.north(), AEBlocks.CRAFTING_STORAGE_1K.block());
            // Crafting CPUs must form a cuboid. Keep the coprocessors in one row.
            if (tierCpu) {
                helper.setBlock(pos.north().west(), switch (machine.capacity()) {
                    case 3 -> ModBlocks.ELITE_CRAFTING_ACCELERATOR.get();
                    case 4 -> ModBlocks.ULTIMATE_CRAFTING_ACCELERATOR.get();
                    default -> ModBlocks.ADVANCED_CRAFTING_ACCELERATOR.get();
                });
            } else {
            helper.setBlock(pos.north().west(), AEBlocks.CRAFTING_ACCELERATOR.block());
            if (machine.capacity() >= 3) {
                helper.setBlock(pos.north().east(), AEBlocks.CRAFTING_ACCELERATOR.block());
            }
            if (machine.capacity() >= 4) {
                helper.setBlock(pos.north().west().west(), AEBlocks.CRAFTING_ACCELERATOR.block());
            }
            }
            helper.setBlock(pos.east(), AEBlocks.DRIVE.block());
            var drive = helper.getBlockEntity(pos.east(), DriveBlockEntity.class);
            drive.getInternalInventory().addItems(AEItems.ITEM_CELL_1K.stack());
            var provider = helper.getBlockEntity(pos.west(), PatternProviderBlockEntity.class);
            provider.getLogic().getPatternInv().setItemDirect(0, encoded);
            provider.getLogic().updatePatterns();
            int batches = machine.capacity() * (installCards ? 6 : 1);
            var job = new TestCraftingJob(helper, pos, AEItemKey.of(Items.STICK), 4 * batches);
            // Higher coprocessor counts may dispatch and finish in the same tick.
            // Check the actual output and ingredient conservation rather than requiring
            // every unit to show nonzero progress at one particular sampling instant.
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertGridOnline(pos))
                    .thenExecute(() -> {
                        helper.check(provider.getLogic().getTerminalGroup().icon()
                                .equals(machine.getCraftingMachineInfo().icon()),
                                "Adjacent Pattern Provider did not advertise the assembler tier");
                        var store = helper.getGrid(pos).getStorageService().getInventory();
                        helper.check(store.insert(AEItemKey.of(Items.OAK_PLANKS), 2 * batches, Actionable.MODULATE,
                                new BaseActionSource()) == 2 * batches, "Could not stage CPU ingredients");
                    })
                    .thenWaitUntil(job::tickUntilStarted)
                    .thenExecute(() -> {
                        if (tierCpu) {
                            var cpu = helper.getBlockEntity(pos.north(), appeng.blockentity.crafting.CraftingBlockEntity.class).getCluster();
                            helper.check(cpu.isBusy(), "Expected active job before persistence roundtrip");
                            var logic = cpu.craftingLogic;
                            var planks = AEItemKey.of(Items.OAK_PLANKS);
                            var sticks = AEItemKey.of(Items.STICK);
                            long stored = logic.getStored(planks);
                            long waiting = logic.getWaitingFor(sticks);
                            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
                            cpu.writeToNBT(output);
                            cpu.readFromNBT(net.minecraft.world.level.storage.TagValueInput.create(
                                    ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
                            helper.check(cpu.isBusy() && logic.getStored(planks) == stored && logic.getWaitingFor(sticks) == waiting,
                                    "CPU lost active job or inventory during serialized roundtrip");
                        }
                    })
                    .thenWaitUntil(() -> {
                        if (!tierCpu) assertAllProgressing(helper, machine, false);
                    })
                    .thenExecute(() -> {
                        if (installCards) {
                            helper.check(machine.getUpgrades().addItems(AEItems.SPEED_CARD.stack(5)).isEmpty(),
                                    "Could not install five cards during the CPU job");
                        }
                    })
                    .thenWaitUntil(() -> {
                        if (installCards && !tierCpu) {
                            assertAllProgressing(helper, machine, true);
                        }
                    })
                    .thenWaitUntil(() -> helper.check(helper.getGrid(pos).getStorageService().getInventory()
                            .getAvailableStacks().get(AEItemKey.of(Items.STICK)) == 4 * batches,
                            "CPU job did not return every batch"))
                    .thenExecute(() -> {
                        var stocks = helper.getGrid(pos).getStorageService().getInventory().getAvailableStacks();
                        helper.check(stocks.get(AEItemKey.of(Items.OAK_PLANKS)) == 0,
                                "Unexpected leftover crafting ingredients");
                        helper.check(machine.canChangePattern(), "Assembler retained an unfinished operation");
                    })
                    .thenSucceed();
        }

        private static void assertAllProgressing(PlotTestHelper helper, ParallelAssemblerBlockEntity machine,
                boolean accelerated) {
            boolean sawAccelerated = false;
            for (int unit = 0; unit < machine.capacity(); unit++) {
                int progress = machine.progress(unit);
                helper.check(progress > 0, "Unit did not progress during the same CPU job: " + unit);
                sawAccelerated |= progress == 50;
            }
            helper.check(!accelerated || sawAccelerated, "The CPU job did not exhibit accelerated progress");
        }

        private static void checkFormatsAndPower(PlotTestHelper helper, ParallelAssemblerBlockEntity machine,
                ChestBlockEntity west, ChestBlockEntity east) {
            var level = helper.getLevel();
            var stone = PatternDetailsHelper.decodePattern(CraftingPatternHelper.encodeStoneCutterPattern(
                    level, Items.STONE, Items.STONE_BRICKS, false), level);
            var smith = PatternDetailsHelper.decodePattern(CraftingPatternHelper.encodeSmithingPattern(
                    level, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.DIAMOND_PICKAXE,
                    Items.NETHERITE_INGOT, false), level);
            var cake = PatternDetailsHelper.decodePattern(CraftingPatternHelper.encodeCraftingPattern(level, new Object[] {
                    Items.MILK_BUCKET, Items.MILK_BUCKET, Items.MILK_BUCKET,
                    Items.SUGAR, Items.EGG, Items.SUGAR,
                    Items.WHEAT, Items.WHEAT, Items.WHEAT
            }, false, false), level);
            var sticks = PatternDetailsHelper.decodePattern(CraftingPatternHelper.encodeCraftingPattern(level, new Object[] {
                    Items.OAK_PLANKS, null, null, Items.OAK_PLANKS, null, null, null, null, null
            }, true, false), level);
            helper.check(machine.pushPattern(stone, counters(stone), Direction.WEST)
                    && machine.pushPattern(smith, counters(smith), Direction.EAST),
                    "Could not dispatch stonecutting and smithing");
            helper.startSequence().thenIdle(12).thenExecute(() -> {
                helper.check(machine.progress(0) == 0 && machine.progress(1) == 0
                        && count(west, Items.STONE_BRICKS) == 0 && count(east, Items.NETHERITE_PICKAXE) == 0,
                        "Units advanced without power");
                helper.setBlock(BlockPos.ZERO.above(), AEBlocks.CREATIVE_ENERGY_CELL.block());
            }).thenWaitUntil(() -> helper.check(count(west, Items.STONE_BRICKS) == 1
                    && count(east, Items.NETHERITE_PICKAXE) == 1 && machine.canChangePattern(),
                    "Smithing/stonecutting did not resume and return after power arrived"))
                    .thenExecute(() -> {
                        var alternative = new KeyCounter[] { new KeyCounter() };
                        alternative[0].add(AEItemKey.of(Items.BIRCH_PLANKS), 2);
                        helper.check(machine.pushPattern(cake, counters(cake), Direction.WEST),
                                "Bucket-bearing pattern rejected");
                        helper.check(machine.pushPattern(sticks, alternative, Direction.EAST),
                                "Valid item substitution rejected");
                    }).thenWaitUntil(() -> helper.check(count(west, Items.CAKE) == 1
                            && count(west, Items.BUCKET) == 3 && count(east, Items.STICK) == 4,
                            "Results or container remainders did not return to their source"))
                    .thenSucceed();
        }

        private static int count(ChestBlockEntity chest, Item item) {
            int total = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                var stack = chest.getItem(i);
                if (stack.is(item)) {
                    total += stack.getCount();
                }
            }
            return total;
        }

        private static ParallelAssemblerBlockEntity currentMachine(PlotTestHelper helper) {
            return helper.getBlockEntity(BlockPos.ZERO, ParallelAssemblerBlockEntity.class);
        }

        private static void installReloaded(PlotTestHelper helper, ParallelAssemblerBlockEntity machine) {
            var level = helper.getLevel();
            level.removeBlockEntity(machine.getBlockPos());
            level.setBlockEntity(machine);
            // Level attachment schedules the AE2 node once.
        }

        private static ParallelAssemblerBlockEntity reload(PlotTestHelper helper,
                ParallelAssemblerBlockEntity machine) {
            var level = helper.getLevel();
            var tag = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            machine.saveWithId(tag);
            return (ParallelAssemblerBlockEntity) BlockEntity.loadStatic(machine.getBlockPos(),
                    machine.getBlockState(), tag.buildResult(), level.registryAccess());
        }

        private static KeyCounter[] counters(IPatternDetails plan) {
            var inputs = plan.getInputs();
            var result = new KeyCounter[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                result[i] = new KeyCounter();
                var item = inputs[i].getPossibleInputs()[0];
                result[i].add(item.what(), item.amount() * inputs[i].getMultiplier());
            }
            return result;
        }

        private static net.minecraft.world.inventory.MenuType<ParallelAssemblerMenu> assemblerMenuType(int capacity) {
            return switch (capacity) {
                case 3 -> ModMenus.ELITE_MOLECULAR_ASSEMBLER.get();
                case 4 -> ModMenus.ULTIMATE_MOLECULAR_ASSEMBLER.get();
                default -> ModMenus.ADVANCED_MOLECULAR_ASSEMBLER.get();
            };
        }

        private static void checkRecipe(PlotTestHelper helper, int capacity, Item item) {
            var level = helper.getLevel();
            var processor = switch (capacity) {
                case 3 -> AEItems.CALCULATION_PROCESSOR.stack();
                case 4 -> AEItems.ENGINEERING_PROCESSOR.stack();
                default -> AEItems.LOGIC_PROCESSOR.stack();
            };
            var previous = switch (capacity) {
                case 3 -> ModBlocks.ADVANCED_MOLECULAR_ASSEMBLER_ITEM.toStack();
                case 4 -> ModBlocks.ELITE_MOLECULAR_ASSEMBLER_ITEM.toStack();
                default -> AEBlocks.MOLECULAR_ASSEMBLER.stack();
            };
            var input = CraftingInput.of(3, 3, List.of(
                    processor, AEItems.SPEED_CARD.stack(), processor.copy(),
                    AEItems.FLUIX_CRYSTAL.stack(), previous, AEItems.FLUIX_CRYSTAL.stack(),
                    AEItems.SPEED_CARD.stack(), AEBlocks.MOLECULAR_ASSEMBLER.stack(), AEItems.SPEED_CARD.stack()));
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                    .orElseThrow(() -> helper.assertionException("Tier assembler recipe missing"));
            var output = recipe.value().assemble(input);
            helper.check(output.is(item) && output.getCount() == 1,
                    "Unexpected tier assembler recipe result");
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return MapCodec.unit(this);
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Applied Tiers Molecular Assembler");
        }
    }
}
