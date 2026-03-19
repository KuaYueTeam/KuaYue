package cn.kycraft.kuayue.parts.carriages.freight;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.*;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.DoubleRotateDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.item.ItemRegConfigurations;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec2;

import static cn.kycraft.kuayue.parts.KuaYueTrainPanelModule.TRAIN_PANEL_TAB;

@Context()
public class CFreightPanel {

    public static final RegistryGroup GROUP_FREIGHT =
            new RegistryGroup().configure(
                    BlockRegConfigurations.adopt(block -> block
                            .noOcclusion()
                            .strength(1.5f, 3f)
                            .mapColor(DyeColor.BLUE)
                    )
            ).configure(
                    ItemRegConfigurations.adopt(
                            item -> item.stacksTo(8)
                                    .tabTo(TRAIN_PANEL_TAB)
                    )
            ).setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final PanelReg<FreightC70EndFaceBlock> FREIGHT_C70_END_FACE =
            new PanelReg<>("freight_c70_end_face", p ->
                    new FreightC70EndFaceBlock(p, FreightEndFaceBlock.FreightType.C70)
            ).setParent(GROUP_FREIGHT);

    public static final PanelReg<TrainPanelBlock> FREIGHT_C70_SLAB_BOTTOM =
            new PanelReg<>("freight_c70_slab_bottom", p -> new TrainPanelBlock(p, new Vec2(0, 0), new Vec2(2, 1)))
                    .setParent(GROUP_FREIGHT);

    public static final PanelReg<TrainOpenableWindowBlock> FREIGHT_C70_SLAB_TOP =
            new PanelReg<>("freight_c70_slab_top", p -> new TrainOpenableWindowBlock(p, -1, 1, 1))
                    .setParent(GROUP_FREIGHT);

    public static final PanelReg<DoubleRotateDoorBlock> FREIGHT_C70_DOOR =
            new PanelReg<>("freight_c70_door", p ->
                    new DoubleRotateDoorBlock(
                            p,
                            TrainPanelProperties.DoorType.ROTATE_SINGLE_SIDED,
                            "carriage/freight/c70/freight_c70_door_left",
                            "carriage/freight/c70/freight_c70_door_right",
                            "carriage/freight/c70/freight_c70_door_frame"
                    )
            ).setParent(GROUP_FREIGHT);

    public static final SlabReg<SlabBlock> FREIGHT_NX70_FLOOR_CENTER =
            new SlabReg<>("freight_nx70_floor_center", p -> new SlabBlock(p, false))
                    .setParent(GROUP_FREIGHT);

    public static final SlabReg<SlabBlock> FREIGHT_NX70_FLOOR_HIGH =
            new SlabReg<>("freight_nx70_floor_high", p -> new SlabBlock(p, false))
                    .setParent(GROUP_FREIGHT);

    public static final SlabReg<SlabBlock> FREIGHT_NX70_FLOOR_COVER_PLATE =
            new SlabReg<>("freight_nx70_floor_cover_plate", p -> new SlabBlock(p, true))
                    .setParent(GROUP_FREIGHT);

    public static final SlabReg<HingeSlabBlock> FREIGHT_NX70_FLOOR_CONNECTION =
            new SlabReg<>("freight_nx70_floor_connection", p -> new HingeSlabBlock(p, false, 2))
                    .setParent(GROUP_FREIGHT);

    public static final SlabReg<SlabBlock> FREIGHT_NX70_FLOOR_LOW =
            new SlabReg<>("freight_nx70_floor_low", p -> new SlabBlock(p, false))
                    .setParent(GROUP_FREIGHT);

    public static final SlabReg<SlabBlock> FREIGHT_NX70_FLOOR_LOW_LADDER =
            new SlabReg<>("freight_nx70_floor_low_ladder", p -> new SlabBlock(p, false))
                    .setParent(GROUP_FREIGHT);

    public static final PanelReg<FreightNX70EndFaceBlock> FREIGHT_NX70_END_FACE =
            new PanelReg<>("freight_nx70_end_face", p ->
                    new FreightNX70EndFaceBlock(p, FreightEndFaceBlock.FreightType.NX70)
            ).setParent(GROUP_FREIGHT);
}
