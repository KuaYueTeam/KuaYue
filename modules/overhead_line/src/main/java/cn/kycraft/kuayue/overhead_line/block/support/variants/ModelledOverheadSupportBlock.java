package cn.kycraft.kuayue.overhead_line.block.support.variants;

import cn.kycraft.kuayue.overhead_line.block.support.OverheadModelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ModelledOverheadSupportBlock extends NormalOverheadSupportBlock implements EntityBlock {
    private final ResourceLocation model;
    private final int modelYaw;
    private final int modelYawStep;

    public ModelledOverheadSupportBlock(
            Properties properties, TrussShape shape,
            ResourceLocation model, int modelYaw, int modelYawStep) {
        this(properties, shape, model, modelYaw, modelYawStep, List.of());
    }

    public ModelledOverheadSupportBlock(Properties properties, TrussShape shape,
                                        ResourceLocation model, int modelYaw, int modelYawStep , List<Vec3> connectionsPoints) {
        super(properties, shape , connectionsPoints);
        this.model = model;
        this.modelYaw = modelYaw;
        this.modelYawStep = modelYawStep;
    }

    public ResourceLocation model() {
        return model;
    }

    public int modelYaw(BlockState state) {
        int steps = switch (state.getValue(FACING)) {
            case EAST -> 0;
            case SOUTH -> 1;
            case WEST -> 2;
            case NORTH -> 3;
            default -> throw new IllegalStateException("Non-horizontal overhead support facing");
        };
        return Math.floorMod(modelYaw + steps * modelYawStep, 360);
    }

    @Override
    public OverheadModelBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OverheadModelBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
