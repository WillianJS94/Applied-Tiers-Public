package io.github.willianjs94.appliedtiers.block;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import io.github.willianjs94.appliedtiers.blockentity.ParallelAssemblerBlockEntity;
import io.github.willianjs94.appliedtiers.menu.ParallelAssemblerMenu;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public final class ParallelAssemblerBlock extends AEBaseEntityBlock<ParallelAssemblerBlockEntity> {
    private final Supplier<MenuType<ParallelAssemblerMenu>> menu;

    public ParallelAssemblerBlock(Properties properties, Supplier<MenuType<ParallelAssemblerMenu>> menu) {
        super(properties);
        this.menu = menu;
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.POWERED);
    }

    @Override
    protected BlockState updateBlockStateFromBlockEntity(BlockState state, ParallelAssemblerBlockEntity machine) {
        return state.setValue(BlockStateProperties.POWERED, machine.isPowered());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var machine = getBlockEntity(level, pos);
        if (machine == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            MenuOpener.open(menu.get(), player, MenuLocators.forBlockEntity(machine));
        }
        return InteractionResult.SUCCESS;
    }
}
