package io.github.willianjs94.appliedtiers.client;

import java.util.ArrayList;
import java.util.List;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.BackgroundPanel;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.ProgressBar;
import appeng.util.Icon;
import appeng.menu.interfaces.IProgressProvider;
import io.github.willianjs94.appliedtiers.menu.ParallelAssemblerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ParallelAssemblerScreen extends UpgradeableScreen<ParallelAssemblerMenu> {
    public ParallelAssemblerScreen(ParallelAssemblerMenu menu, Inventory inventory,
            Component title, ScreenStyle style) {
        super(menu, inventory, title, style);
        widgets.add("patternPanel", new BackgroundPanel(style.getImage("patternPanel")));
        addToLeftToolbar(new IconButton(button -> menu.cycleProcessingMode()) {
            private static final String KEY = "gui.appliedtiers.assembler_mode.";

            @Override
            protected Icon getIcon() {
                return switch (menu.processingMode) {
                    case AUTOMATIC -> Icon.VIEW_MODE_ALL;
                    case EXCLUSIVE -> Icon.LOCKED;
                    case DIVERSIFIED -> Icon.SORT_BY_NAME;
                };
            }

            @Override
            public List<Component> getTooltipMessage() {
                String mode = KEY + menu.processingMode.id();
                var tooltip = new ArrayList<Component>();
                tooltip.add(Component.translatable(KEY + "title", Component.translatable(mode)));
                tooltip.add(Component.translatable(mode + ".description"));
                if (menu.standalone) {
                    tooltip.add(Component.translatable(KEY + "standalone"));
                } else if (menu.processingModeTransitioning) {
                    tooltip.add(Component.translatable(mode + ".transition"));
                }
                return tooltip;
            }
        });
        for (int i = 0; i < menu.capacity(); i++) {
            int unit = i;
            var key = "progress" + i;
            widgets.add(key, new ProgressBar(new IProgressProvider() {
                @Override
                public int getCurrentProgress() {
                    return menu.progress(unit);
                }

                @Override
                public int getMaxProgress() {
                    return 100;
                }
            }, style.getImage("progress"), ProgressBar.Direction.VERTICAL));
        }
    }
}
