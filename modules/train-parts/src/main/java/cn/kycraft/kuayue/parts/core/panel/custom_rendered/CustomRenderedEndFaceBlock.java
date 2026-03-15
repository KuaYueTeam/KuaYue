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
    public CustomRenderedEndFaceBlock(BlockBehaviour.Properties pProperties,
                                      TrainPanelProperties.DoorType doorType,
                                      PartialModel leftModel, PartialModel rightModel, PartialModel frameModel) {
        super(pProperties, doorType);
        this.models = Couple.create(leftModel, rightModel);
        this.frameModel = frameModel;
    }

    public CustomRenderedEndFaceBlock(BlockBehaviour.Properties prop,
                                      TrainPanelProperties.DoorType doorType,
                                      ResourceLocation left, ResourceLocation right, ResourceLocation frame) {
        super(prop, doorType);
        this.models = Couple.create(PartialModel.of(left), PartialModel.of(right));
        this.frameModel = PartialModel.of(frame);
    }

    public CustomRenderedEndFaceBlock(BlockBehaviour.Properties properties, TrainPanelProperties.DoorType doorType,
                                      String leftModel, String rightModel, String frameModel) {
        super(properties, doorType);
        this.models = Couple.create(
                leftModel == null ? null : PartialModel.of(ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "block/" + leftModel)),
                rightModel == null ? null : PartialModel.of(ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "block/" + rightModel)));
        this.frameModel = PartialModel.of(ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "block/" + frameModel));
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
