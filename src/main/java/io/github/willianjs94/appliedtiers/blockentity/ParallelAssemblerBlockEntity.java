/*
 * Assembly semantics adapted from Applied Energistics 2 v26.1.14-beta.
 * Copyright (c) 2013-2014, AlgorithmX2 and AE2 contributors.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See META-INF/AE2-LICENSE.txt.
 */
package io.github.willianjs94.appliedtiers.blockentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.blockentity.grid.AENetworkedInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.crafting.CraftingEvent;
import appeng.menu.AutoCraftingMenu;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import io.github.willianjs94.appliedtiers.network.AssemblerAnimationPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class ParallelAssemblerBlockEntity extends AENetworkedInvBlockEntity
        implements ICraftingMachine, IGridTickable, IUpgradeableObject, IPowerChannelState {
    private static final int INPUTS = 9;
    private static final int RETURN_SLOTS = 10;
    private static final int[] SPEED = {10, 13, 17, 20, 25, 50};
    private static final double[] ENERGY = {1.0, 1.3, 1.7, 2.0, 2.5, 5.0};

    private final List<Unit> units;
    private final AppEngInternalInventory permanentPattern = new AppEngInternalInventory(this, 1, 1);
    private final InternalInventory contents;
    private final InternalInventory external;
    private final IUpgradeInventory upgrades;
    private AssemblerProcessingMode processingMode = AssemblerProcessingMode.AUTOMATIC;
    private int nextUnit;
    private boolean loading;
    private static final int ANIMATION_SEND_INTERVAL = 4;
    private static final int MAX_ANIMATIONS_PER_INTERVAL = 4;
    private final Map<AEItemKey, Long> recentAnimations = new HashMap<>();
    private AssemblerAnimation animation;

    public ParallelAssemblerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            ItemLike item, int capacity) {
        super(type, pos, state);
        var unitList = new ArrayList<Unit>(capacity);
        var all = new ArrayList<InternalInventory>();
        var ports = new ArrayList<InternalInventory>();
        for (int i = 0; i < capacity; i++) {
            var unit = new Unit();
            unitList.add(unit);
            all.add(unit.input);
            all.add(unit.returns);
            ports.add(new FilteredInternalInventory(new CombinedInternalInventory(unit.input, unit.returns),
                    new IAEItemFilter() {
                        @Override
                        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                            return slot < INPUTS && canInsert(unit, slot, stack);
                        }

                        @Override
                        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
                            return slot >= INPUTS && unit.returnSide == null;
                        }
                    }));
        }
        units = List.copyOf(unitList);
        all.add(permanentPattern);
        contents = new CombinedInternalInventory(all.toArray(InternalInventory[]::new));
        external = new CombinedInternalInventory(ports.toArray(InternalInventory[]::new));
        upgrades = UpgradeInventories.forMachine(item, 5, this::saveChanges);
        getMainNode().setIdlePowerUsage(0).addService(IGridTickable.class, this);
    }

    private final class Unit {
        final AppEngInternalInventory input = new AppEngInternalInventory(ParallelAssemblerBlockEntity.this, INPUTS, 1);
        final AppEngInternalInventory returns = new AppEngInternalInventory(ParallelAssemblerBlockEntity.this, RETURN_SLOTS);
        final TransientCraftingContainer grid = new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3);
        ItemStack dispatchedPattern = ItemStack.EMPTY;
        Direction returnSide;
        double progress;
        boolean returning;

        CraftingInput.Positioned positionedInput() {
            for (int i = 0; i < INPUTS; i++) {
                grid.setItem(i, input.getStackInSlot(i));
            }
            return grid.asPositionedCraftInput();
        }

        boolean empty() {
            return input.isEmpty() && returns.isEmpty() && returnSide == null;
        }

        void finish() {
            dispatchedPattern = ItemStack.EMPTY;
            returnSide = null;
            returning = false;
            progress = 0;
        }
    }

    private IMolecularAssemblerSupportedPattern decode(ItemStack stack) {
        if (level instanceof ServerLevel && !stack.isEmpty()
                && PatternDetailsHelper.decodePattern(stack, level) instanceof IMolecularAssemblerSupportedPattern plan) {
            return plan;
        }
        return null;
    }

    private IMolecularAssemblerSupportedPattern plan(Unit unit) {
        return decode(unit.returnSide == null ? permanentPattern.getStackInSlot(0) : unit.dispatchedPattern);
    }

    @Override
    public boolean acceptsPlans() {
        return permanentPattern.isEmpty() && units.stream().anyMatch(Unit::empty);
    }

    public final AssemblerProcessingMode processingMode() {
        return processingMode;
    }

    public final void cycleProcessingMode() {
        if (level instanceof ServerLevel) {
            processingMode = processingMode.next();
            saveChanges();
        }
    }

    public final boolean hasPermanentPattern() {
        return !permanentPattern.isEmpty();
    }

    public final boolean processingModeTransitioning() {
        if (processingMode == AssemblerProcessingMode.AUTOMATIC || hasPermanentPattern()) {
            return false;
        }
        var identities = new HashSet<AEItemKey>();
        for (var unit : units) {
            if (unit.returnSide != null) {
                boolean distinct = identities.add(AEItemKey.of(unit.dispatchedPattern));
                if (processingMode == AssemblerProcessingMode.EXCLUSIVE ? identities.size() > 1 : !distinct) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean permitsDispatch(AEItemKey incoming) {
        if (processingMode == AssemblerProcessingMode.AUTOMATIC) {
            return true;
        }
        // The encoded pattern is already saved for each dispatched unit. Keep
        // that identity reserved through crafting and the entire return phase.
        for (var unit : units) {
            if (unit.returnSide != null) {
                boolean same = incoming.equals(AEItemKey.of(unit.dispatchedPattern));
                if (processingMode == AssemblerProcessingMode.EXCLUSIVE ? !same : same) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs, Direction side) {
        if (!(level instanceof ServerLevel) || side == null || !acceptsPlans()
                || !(details instanceof IMolecularAssemblerSupportedPattern recipe)
                || inputs.length != details.getInputs().length || !permitsDispatch(details.getDefinition())) {
            return false;
        }
        var target = units.stream().filter(Unit::empty).findFirst().orElseThrow();
        var staged = new ItemStack[INPUTS];
        Arrays.fill(staged, ItemStack.EMPTY);
        var copies = new KeyCounter[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            copies[i] = new KeyCounter();
            copies[i].addAll(inputs[i]);
        }
        try {
            recipe.fillCraftingGrid(copies, (slot, stack) -> staged[slot] = stack.copy());
            for (var counter : copies) {
                counter.removeZeros();
                if (!counter.isEmpty()) {
                    return false;
                }
            }
            if (recipe.assemble(CraftingInput.ofPositioned(3, 3, List.of(staged)).input(), level).isEmpty()) {
                return false;
            }
        } catch (RuntimeException invalidPattern) {
            // The adapter can reject malformed or stale pattern inputs.
            return false;
        }
        target.dispatchedPattern = details.getDefinition().toStack();
        target.returnSide = side;
        target.progress = 0;
        for (int i = 0; i < INPUTS; i++) {
            target.input.setItemDirect(i, staged[i]);
        }
        for (var input : inputs) {
            input.clear();
        }
        saveChanges();
        wake();
        return true;
    }

    private boolean canInsert(Unit unit, int slot, ItemStack stack) {
        if (unit.returnSide != null || unit.returning || permanentPattern.isEmpty()) {
            return false;
        }
        var recipe = plan(unit);
        var key = AEItemKey.of(stack);
        return recipe != null && key != null && recipe.isSlotEnabled(slot) && recipe.isItemValid(slot, key, level);
    }

    public boolean canInsert(int unit, int slot, ItemStack stack) {
        return canInsert(units.get(unit), slot, stack);
    }

    public boolean canChangePattern() {
        return units.stream().allMatch(Unit::empty);
    }

    public boolean validPermanentPattern(ItemStack stack) {
        return decode(stack) != null;
    }

    public int enabledMask(int index) {
        var unit = units.get(index);
        var recipe = plan(unit);
        int mask = 0;
        for (int i = 0; i < INPUTS; i++) {
            if (!unit.input.getStackInSlot(i).isEmpty() || recipe != null && recipe.isSlotEnabled(i)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    public boolean manual(int index) {
        return units.get(index).returnSide == null;
    }

    public InternalInventory input(int index) {
        return units.get(index).input;
    }

    public InternalInventory output(int index) {
        return units.get(index).returns;
    }

    public InternalInventory patternInventory() {
        return permanentPattern;
    }

    public int capacity() {
        return units.size();
    }

    public int progress(int index) {
        return (int) units.get(index).progress;
    }

    private void wake() {
        if (!loading && level instanceof ServerLevel) {
            getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inventory, int slot) {
        if (!loading && level instanceof ServerLevel) {
            // Changing a manually loaded grid invalidates any progress for that grid.
            for (var unit : units) {
                if (inventory == unit.input && unit.returnSide == null && !unit.returning) {
                    unit.progress = 0;
                }
            }
            wake();
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, units.stream().noneMatch(this::needsTick));
    }

    private boolean needsTick(Unit unit) {
        if (unit.returnSide != null || unit.returning || !unit.returns.isEmpty()) {
            return true;
        }
        if (unit.input.isEmpty()) {
            return false;
        }
        var recipe = plan(unit);
        return recipe != null
                && !recipe.assemble(unit.positionedInput().input(), level).isEmpty();
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int elapsed) {
        if (!(level instanceof ServerLevel)) {
            return TickRateModulation.SLEEP;
        }
        int cards = Math.min(5, upgrades.getInstalledUpgrades(AEItems.SPEED_CARD));
        boolean workRemaining = false;
        for (int i = 0; i < units.size(); i++) {
            workRemaining |= tick(units.get((nextUnit + i) % units.size()), node, cards);
        }
        nextUnit = (nextUnit + 1) % units.size();
        // Recheck only if no processed unit reported work: crafting callbacks or
        // adjacent automation may have changed another unit during this pass.
        return workRemaining || units.stream().anyMatch(this::needsTick)
                ? TickRateModulation.URGENT : TickRateModulation.SLEEP;
    }

    private boolean tick(Unit unit, IGridNode node, int cards) {
        if (unit.returning || !unit.returns.isEmpty()) {
            drain(unit);
            return needsTick(unit);
        }
        // Empty standalone units have no possible recipe. Avoid decoding the
        // shared pattern and building a temporary crafting input for them.
        if (unit.returnSide == null && unit.input.isEmpty()) {
            return false;
        }
        var recipe = plan(unit);
        var positioned = recipe == null ? null : unit.positionedInput();
        var input = positioned == null ? null : positioned.input();
        var result = input == null || input.size() == 0 ? ItemStack.EMPTY : recipe.assemble(input, level);
        if (result.isEmpty()) {
            if (unit.returnSide != null) {
                // A stale or cancelled recipe refunds the original staged ingredients to its provider.
                unit.returning = true;
                for (int i = 0; i < INPUTS; i++) {
                    unit.returns.setItemDirect(i + 1, unit.input.getStackInSlot(i));
                    unit.input.setItemDirect(i, ItemStack.EMPTY);
                }
                unit.progress = 0;
                saveChanges();
                drain(unit);
            }
            return needsTick(unit);
        }
        var energy = node.getGrid().getEnergyService();
        double requested = SPEED[cards] * ENERGY[cards];
        double received = energy.extractAEPower(requested, Actionable.MODULATE, PowerMultiplier.CONFIG);
        if (received <= 0) {
            return true;
        }
        unit.progress += Math.min(requested, received) / ENERGY[cards];
        if (unit.progress < 100) {
            saveChanges();
            return true;
        }

        var remainders = recipe.getRemainingItems(input);
        result.onCraftedBySystem(level);
        CraftingEvent.fireAutoCraftingEvent(level, recipe, result, unit.grid);
        unit.returning = true;
        unit.progress = 0;
        unit.returns.setItemDirect(0, result.copy());
        for (int i = 0; i < INPUTS; i++) {
            unit.input.setItemDirect(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < remainders.size(); i++) {
            var remainder = remainders.get(i);
            if (unit.returnSide == null && !remainder.isEmpty()) {
                int x = i % input.width() + positioned.left();
                int y = i / input.width() + positioned.top();
                if (recipe.isItemValid(x + y * 3, AEItemKey.of(remainder), level)) {
                    unit.input.setItemDirect(x + y * 3, remainder);
                    continue;
                }
            }
            unit.returns.setItemDirect(i + 1, remainder.copy());
        }
        saveChanges();
        drain(unit);
        if (level instanceof ServerLevel server) {
            sendAnimation(server, result, SPEED[cards]);
        }
        return needsTick(unit);
    }

    private void sendAnimation(ServerLevel server, ItemStack result, int speed) {
        long now = server.getGameTime();
        recentAnimations.values().removeIf(sent -> now - sent >= ANIMATION_SEND_INTERVAL);
        var key = AEItemKey.of(result);
        if (key == null || recentAnimations.size() >= MAX_ANIMATIONS_PER_INTERVAL || recentAnimations.putIfAbsent(key, now) != null) {
            return;
        }
        var pos = getBlockPos();
        PacketDistributor.sendToPlayersNear(server, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32,
                new AssemblerAnimationPayload(server.dimension().identifier(), pos, speed, result.copyWithCount(1)));
    }

    public void recordAnimation(ItemStack result, int speed, long now) {
        if (animation == null) {
            animation = new AssemblerAnimation();
        }
        animation.accept(result, speed, now);
    }

    public AssemblerAnimation animation() {
        return animation;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        animation = null;
        recentAnimations.clear();
    }

    private void drain(Unit unit) {
        for (int i = 0; i < RETURN_SLOTS; i++) {
            var stack = unit.returns.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            var unsent = stack.copy();
            if (unit.returnSide != null) {
                unsent = eject(unsent, unit.returnSide);
            } else {
                for (var face : Direction.values()) {
                    unsent = eject(unsent, face);
                    if (unsent.isEmpty()) {
                        break;
                    }
                }
            }
            if (unsent.getCount() != stack.getCount()) {
                unit.returns.setItemDirect(i, unsent);
            }
        }
        if (unit.returnSide == null && unit.returns.getStackInSlot(0).isEmpty()) {
            // A blocked standalone machine exposes its remaining items through the visible output.
            for (int i = 1; i < RETURN_SLOTS; i++) {
                var stack = unit.returns.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    unit.returns.setItemDirect(0, stack);
                    unit.returns.setItemDirect(i, ItemStack.EMPTY);
                    break;
                }
            }
        }
        if (unit.returns.isEmpty()) {
            unit.finish();
            saveChanges();
        }
    }

    private ItemStack eject(ItemStack stack, Direction face) {
        var destination = InternalInventory.wrapExternal(level, worldPosition.relative(face), face.getOpposite());
        return destination == null ? stack : destination.addItems(stack);
    }

    @Override
    public void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        upgrades.writeToNBT(data, "upgrades");
        data.putString("processingMode", processingMode.id());
        for (int i = 0; i < units.size(); i++) {
            var unit = units.get(i);
            var tag = data.child("unit" + i);
            tag.store("plan", ItemStack.OPTIONAL_CODEC, unit.dispatchedPattern);
            if (unit.returnSide != null) {
                tag.store("side", Direction.CODEC, unit.returnSide);
            }
            tag.putDouble("progress", unit.progress);
            tag.putBoolean("returning", unit.returning);
        }
    }

    @Override
    public void loadTag(ValueInput data) {
        loading = true;
        try {
            super.loadTag(data);
            upgrades.readFromNBT(data, "upgrades");
            processingMode = AssemblerProcessingMode.fromId(data.getStringOr("processingMode", ""));
            for (int i = 0; i < units.size(); i++) {
                var unit = units.get(i);
                var tag = data.childOrEmpty("unit" + i);
                unit.dispatchedPattern = tag.read("plan", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
                unit.returnSide = tag.read("side", Direction.CODEC).orElse(null);
                unit.progress = Math.clamp(tag.getDoubleOr("progress", 0), 0, 100);
                unit.returning = tag.getBooleanOr("returning", false);
            }
        } finally {
            loading = false;
        }
        wake();
    }

    @Override
    public void addAdditionalDrops(net.minecraft.world.level.Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (var upgrade : upgrades) {
            if (!upgrade.isEmpty()) {
                drops.add(upgrade);
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        upgrades.clear();
        units.forEach(Unit::finish);
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        int cards = upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        List<Component> tooltip = cards == 0 ? List.of() : List.of(GuiText.CompatibleUpgrade.text(
                Tooltips.of(AEItems.SPEED_CARD.asItem().getDefaultInstance().getItemName()),
                Tooltips.ofUnformattedNumber(cards)));
        return new PatternContainerGroup(AEItemKey.of(getItemFromBlockEntity()), getName(), tooltip);
    }

    @Override
    public InternalInventory getInternalInventory() {
        return contents;
    }

    @Override
    public ResourceHandler<ItemResource> getExposedItemHandler(Direction side) {
        return external.toResourceHandler();
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    @Override
    public AECableType getCableConnectionType(Direction side) {
        return AECableType.COVERED;
    }

    @Override
    public boolean isPowered() {
        return getMainNode().isPowered();
    }

    @Override
    public boolean isActive() {
        return isPowered();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        markForUpdate();
        wake();
    }
}
