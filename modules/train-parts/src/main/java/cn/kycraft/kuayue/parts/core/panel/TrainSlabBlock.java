package cn.kycraft.kuayue.parts.core.panel;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class TrainSlabBlock extends TrainPanelBlock implements IWrenchable {
    public final boolean carport;


    public TrainSlabBlock(Properties pProperties, boolean isCarport) {
        super(pProperties);
        this.carport = isCarport;
    }

    public TrainSlabBlock(Properties properties, boolean isCarport, int left, int right){
        super(properties, new Vec2(left, 0), new Vec2(right, 1));
        this.carport = isCarport;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (carport) return TrainPanelShapes.CARPORT_CENTER;
        return TrainPanelShapes.FLOOR;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return super.getStateForPlacement(pContext)
                .setValue(FACING, pContext.getHorizontalDirection());
    }


}
