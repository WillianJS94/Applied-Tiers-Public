package io.github.willianjs94.appliedtiers.block;

import appeng.block.crafting.PatternProviderBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A separate tier, keeping all of AE2's normal block behavior.
 */
public final class AdvancedPatternProviderBlock extends PatternProviderBlock {
    public AdvancedPatternProviderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
