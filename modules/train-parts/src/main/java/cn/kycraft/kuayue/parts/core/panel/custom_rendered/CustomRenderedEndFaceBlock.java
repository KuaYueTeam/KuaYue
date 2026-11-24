package cn.kycraft.kuayue.parts.core.panel.custom_rendered;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.core.panel.EndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelProperties;
import com.simibubi.create.foundation.block.IBE;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomRenderedEndFaceBlock extends EndFaceBlock implements IBE<CustomRenderedEndFaceEntity> {

    public final Couple<PartialModel> models;
    public final PartialModel frameModel;
    /**
     * 端面框额外的 Y 偏移（格）。
     * 渲染器默认给端面框加 1 格 Y —— 用 OBJ 建模的端面正是按这个约定把框建低了一格；
     * 而用原生方块元素模型建模的端面，其元素坐标就是碰撞盒坐标，需要用 -1 抵消这一格。
     */
    public final double frameOffsetY;

    public CustomRenderedEndFaceBlock(BlockBehaviour.Properties pProperties,
                                      TrainPanelProperties.DoorType doorType,
                                      PartialModel leftModel, PartialModel rightModel, PartialModel frameModel) {
        this(pProperties, doorType, leftModel, rightModel, frameModel, 0);
    }

    public CustomRenderedEndFaceBlock(BlockBehaviour.Properties pProperties,
                                      TrainPanelProperties.DoorType doorType,
                                      PartialModel leftModel, PartialModel rightModel, PartialModel frameModel,
                                      double frameOffsetY) {
        super(pProperties, doorType);
        this.models = Couple.create(leftModel, rightModel);
        this.frameModel = frameModel;
        this.frameOffsetY = frameOffsetY;
    }

    public CustomRenderedEndFaceBlock(BlockBehaviour.Properties prop,
                                      TrainPanelProperties.DoorType doorType,
                                      ResourceLocation left, ResourceLocation right, ResourceLocation frame) {
        super(prop, doorType);
        this.models = Couple.create(PartialModel.of(left), PartialModel.of(right));
        this.frameModel = PartialModel.of(frame);
        this.frameOffsetY = 0;
    }

    public CustomRenderedEndFaceBlock(BlockBehaviour.Properties properties, TrainPanelProperties.DoorType doorType,
                                      String leftModel, String rightModel, String frameModel) {
        this(properties, doorType, leftModel, rightModel, frameModel, 0);
    }

    public CustomRenderedEndFaceBlock(BlockBehaviour.Properties properties, TrainPanelProperties.DoorType doorType,
                                      String leftModel, String rightModel, String frameModel, double frameOffsetY) {
        super(properties, doorType);
        this.models = Couple.create(
                leftModel == null ? null : PartialModel.of(ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "block/" + leftModel)),
                rightModel == null ? null : PartialModel.of(ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "block/" + rightModel)));
        this.frameModel = frameModel == null ? null : PartialModel.of(ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "block/" + frameModel));
        this.frameOffsetY = frameOffsetY;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pPos, BlockState pState) {
        return getBlockEntityType().create(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public Class<CustomRenderedEndFaceEntity> getBlockEntityClass() {
        return CustomRenderedEndFaceEntity.class;
    }

    @Override
    public BlockEntityType<? extends CustomRenderedEndFaceEntity> getBlockEntityType() {
        return CustomRenderedRegistry.CUSTOM_RENDERED_END_FACE_ENTITY.getEntry();
    }
}
