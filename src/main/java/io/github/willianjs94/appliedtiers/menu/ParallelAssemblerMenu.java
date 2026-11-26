package io.github.willianjs94.appliedtiers.menu;

import appeng.client.Point;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.ClientActionKey;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.IOptionalSlot;
import appeng.menu.slot.RestrictedInputSlot;
import io.github.willianjs94.appliedtiers.blockentity.ParallelAssemblerBlockEntity;
import io.github.willianjs94.appliedtiers.blockentity.AssemblerProcessingMode;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class ParallelAssemblerMenu extends UpgradeableMenu<ParallelAssemblerBlockEntity> {
    private static final ClientActionKey<Void> CYCLE_MODE = new ClientActionKey<>("CycleAssemblerMode");
    private static final SlotSemantic[] INPUT = {
        SlotSemantics.register("APPLIEDTIERS_ASSEMBLER_GRID_1", false),
        SlotSemantics.register("APPLIEDTIERS_ASSEMBLER_GRID_2", false),
        SlotSemantics.register("APPLIEDTIERS_ASSEMBLER_GRID_3", false),
        SlotSemantics.register("APPLIEDTIERS_ASSEMBLER_GRID_4", false)
    };
    private static final SlotSemantic[] OUTPUT = {
        SlotSemantics.register("APPLIEDTIERS_ASSEMBLER_OUTPUT_1", false),
        SlotSemantics.register("APPLIEDTIERS_ASSEMBLER_OUTPUT_2", false),
        SlotSemantics.register("APPLIEDTIERS_ASSEMBLER_OUTPUT_3", false),
        SlotSemantics.register("APPLIEDTIERS_ASSEMBLER_OUTPUT_4", false)
    };

    // Each word carries a unit's 7-bit progress, 9-bit slot mask and manual flag.
    private static final int PROGRESS_MASK = 0x7f;
    private static final int SLOT_MASK = 0x1ff;
    private static final int MANUAL_FLAG = 1 << 16;
    @GuiSync(10) public int unitFirst;
    @GuiSync(11) public int unitSecond;
    @GuiSync(12) public int unitThird;
    @GuiSync(13) public int unitFourth;
    @GuiSync(16) public boolean changePattern;
    @GuiSync(17) public AssemblerProcessingMode processingMode = AssemblerProcessingMode.AUTOMATIC;
    @GuiSync(18) public boolean processingModeTransitioning;
    @GuiSync(19) public boolean standalone;

    public ParallelAssemblerMenu(MenuType<ParallelAssemblerMenu> type, int id, Inventory inventory,
            ParallelAssemblerBlockEntity host) {
        super(type, id, inventory, host);
        registerClientAction(CYCLE_MODE, this::applyCycleProcessingMode);
    }

    public void cycleProcessingMode() {
        sendClientAction(CYCLE_MODE);
    }

    private void applyCycleProcessingMode() {
        var player = getPlayer();
        // The packet conveys only an intent to cycle, never a client-selected value.
        if (isServerSide() && player.containerMenu == this && stillValid(player)
                && player.isWithinBlockInteractionRange(getHost().getBlockPos(), 0)
                && getHost().getLevel() == player.level()
                && player.level().getBlockEntity(getHost().getBlockPos()) == getHost()) {
            getHost().cycleProcessingMode();
        }
    }

    @Override
    protected void setupConfig() {
        for (int i = 0; i < capacity(); i++) {
            int unit = i;
            for (int slot = 0; slot < 9; slot++) {
                addSlot(new IngredientSlot(unit, slot), INPUT[unit]);
            }
            addSlot(new AppEngSlot(getHost().output(unit), 0) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return manual(unit) && super.mayPickup(player);
                }
            }, OUTPUT[unit]);
        }
        addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.MOLECULAR_ASSEMBLER_PATTERN, getHost().patternInventory(), 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return canChangePattern() && super.mayPlace(stack)
                        && (!isServerSide() || getHost().validPermanentPattern(stack));
            }

            @Override
            public boolean mayPickup(Player player) {
                return canChangePattern() && super.mayPickup(player);
            }
        }, SlotSemantics.ENCODED_PATTERN);
    }

    private boolean canChangePattern() {
        return isServerSide() ? getHost().canChangePattern() : changePattern;
    }

    private boolean manual(int unit) {
        return isServerSide() ? getHost().manual(unit) : (unitState(unit) & MANUAL_FLAG) != 0;
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            unitFirst = packUnit(0);
            unitSecond = packUnit(1);
            unitThird = capacity() > 2 ? packUnit(2) : 0;
            unitFourth = capacity() > 3 ? packUnit(3) : 0;
            changePattern = getHost().canChangePattern();
            processingMode = getHost().processingMode();
            processingModeTransitioning = getHost().processingModeTransitioning();
            standalone = getHost().hasPermanentPattern();
        }
        super.broadcastChanges();
    }

    public int progress(int unit) {
        return unitState(unit) & PROGRESS_MASK;
    }

    public int capacity() {
        return getHost().capacity();
    }

    private int packUnit(int unit) {
        return getHost().progress(unit) | (getHost().enabledMask(unit) << 7)
                | (getHost().manual(unit) ? MANUAL_FLAG : 0);
    }

    private int unitState(int unit) {
        return switch (unit) {
            case 0 -> unitFirst;
            case 1 -> unitSecond;
            case 2 -> unitThird;
            case 3 -> unitFourth;
            default -> throw new IndexOutOfBoundsException(unit);
        };
    }

    private final class IngredientSlot extends AppEngSlot implements IOptionalSlot {
        private final int unit;
        private final int index;

        IngredientSlot(int unit, int index) {
            super(getHost().input(unit), index);
            this.unit = unit;
            this.index = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return manual(unit) && super.mayPlace(stack)
                    && (!isServerSide() || getHost().canInsert(unit, index, stack));
        }

        @Override
        public boolean mayPickup(Player player) {
            return manual(unit) && super.mayPickup(player);
        }

        @Override
        public boolean isSlotEnabled() {
            int mask = isServerSide() ? getHost().enabledMask(unit) : (unitState(unit) >>> 7) & SLOT_MASK;
            return (mask & 1 << index) != 0;
        }

        @Override
        public boolean isRenderDisabled() {
            return true;
        }

        @Override
        public Point getBackgroundPos() {
            return new Point(x - 1, y - 1);
        }
    }
}
