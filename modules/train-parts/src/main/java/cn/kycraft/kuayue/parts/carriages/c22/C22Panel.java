package cn.kycraft.kuayue.parts.carriages.c22;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.*;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedDoorBlock;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.CustomRenderedEndFaceBlock;
import cn.kycraft.kuayue.parts.core.panel.registration.PanelReg;
import cn.kycraft.kuayue.parts.core.panel.registration.SlabReg;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainSmallWindowBlock;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.item.ItemRegConfigurations;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;

import static cn.kycraft.kuayue.parts.KuaYueTrainPanelModule.TRAIN_PANEL_TAB;

@Context()
public class C22Panel {

    public static final RegistryGroup GROUP_C22 =
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

    public static final SlabReg<SlabBlock> C22_FLOOR =
            new SlabReg<>("22_floor", p -> new SlabBlock(p, false))
                    .setParent(GROUP_C22);

    public static final SlabReg<SlabBlock> C22_FLOOR_CENTER =
            new SlabReg<>("22_floor_center", p -> new SlabBlock(p, false))
                    .setParent(GROUP_C22);

    public static final PanelReg<TrainPanelBlock> C22_PANEL_BOTTOM =
            new PanelReg<>("22_panel_bottom", TrainPanelBlock::new)
                    .setParent(GROUP_C22);

    public static final PanelReg<TrainPanelBlock> C22_PANEL_MIDDLE =
            new PanelReg<>("22_panel_middle", TrainPanelBlock::new)
                    .setParent(GROUP_C22);

    public static final SlabReg<LadderBlock> C22_LADDER =
            new SlabReg<>("22_ladder", p -> new LadderBlock(p, false))
                    .setParent(GROUP_C22);

    public static final PanelReg<TrainSmallWindowBlock> C22_WINDOW =
            new PanelReg<>("22_window", TrainSmallWindowBlock::new)
                    .setParent(GROUP_C22);

    public static final PanelReg<TrainOpenableWindowBlock> C22_LARGE_WINDOW =
            new PanelReg<>("22_large_window", p -> new TrainOpenableWindowBlock(p, 2))
                    .setParent(GROUP_C22);

    public static final PanelReg<TrainOpenableWindowBlock> C22_DOUBLE_SMALL_WINDOW =
            new PanelReg<>("22_double_small_window", p -> new TrainOpenableWindowBlock(p, 2))
                    .setParent(GROUP_C22);

    public static final PanelReg<TrainOpenableWindowBlock> C22_SMALL_WINDOW =
            new PanelReg<>("22_small_window", p -> new TrainOpenableWindowBlock(p, 1))
                    .setParent(GROUP_C22);

    public static final PanelReg<CustomRenderedDoorBlock> DOOR_22 =
            new PanelReg<>("22_door", p -> new CustomRenderedDoorBlock(
                    p,
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_22/door/22_door_bottom_lh"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_22/door/22_door_upper_lh")
                    ),
                    Couple.create(
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_22/door/22_door_bottom"),
                            ResourceLocation.fromNamespaceAndPath(KuaYue.MODID, "carriage/carriage_22/door/22_door_upper")
                    ),
                    new Vec3(0, 0, 0),
                    new Vec3(0, 0, 0),
                    RenderShape.ENTITYBLOCK_ANIMATED,
                    false
            ))
                    .setParent(GROUP_C22);

    public static final SlabReg<SlabBlock> C22_COUPLER =
            new SlabReg<>("22_coupler", p -> new SlabBlock(p, false))
                    .setParent(GROUP_C22);

    public static final PanelReg<CustomRenderedEndFaceBlock> C22_END_FACE =
            new PanelReg<>("22_end_face", p -> new CustomRenderedEndFaceBlock(
                    p,
                    TrainPanelProperties.DoorType.NO_DOOR,
                    null,
                    null,
                    "carriage/carriage_22/22_end_face"
            ))
                    .setParent(GROUP_C22);

    public static final PanelReg<TrainPanelBlock> C22_CARPORT_PANEL_TOP =
            new PanelReg<>("22_carport_panel_top", TrainPanelBlock::new)
                    .setParent(GROUP_C22);

    public static final PanelReg<TrainHingePanelBlock> C22_CARPORT_PANEL_TOP_LAMP =
            new PanelReg<>("22_carport_panel_top_lamp", TrainHingePanelBlock::new)
                    .setParent(GROUP_C22);

    public static final SlabReg<SlabBlock> C22_CARPORT_TOP_CAP =
            new SlabReg<>("22_carport_top_cap", p -> new SlabBlock(p, true))
                    .setParent(GROUP_C22);

    public static final SlabReg<AirVentBlock> C22_AIR_VENT =
            new SlabReg<>("22_air_vent", AirVentBlock::new)
                    .setParent(GROUP_C22);
}
