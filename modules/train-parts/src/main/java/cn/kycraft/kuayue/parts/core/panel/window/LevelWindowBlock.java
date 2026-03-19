package cn.kycraft.kuayue.parts.core.panel.window;

import cn.kycraft.kuayue.parts.core.panel.TrainPanelProperties;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

public class LevelWindowBlock extends TrainOpenableWindowBlock implements IWrenchable {
    public LevelWindowBlock(Properties pProperties) {
        super(pProperties, -1, 2);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.EAST)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, false)
                .setValue(TrainPanelProperties.TYPE, 0)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(TrainPanelProperties.TYPE));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        BlockState rotated = state.cycle(TrainPanelProperties.TYPE);
        if (!rotated.canSurvive(world, context.getClickedPos())) {
            return InteractionResult.PASS;
        }

        KineticBlockEntity.switchToBlockState(world, context.getClickedPos(), updateAfterWrenched(rotated, context));

        if (world.getBlockState(context.getClickedPos()) != state) {
            IWrenchable.playRotateSound(world, context.getClickedPos());
        }

        return InteractionResult.SUCCESS;
    }
}
