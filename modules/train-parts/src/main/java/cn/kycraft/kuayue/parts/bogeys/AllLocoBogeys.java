package cn.kycraft.kuayue.parts.bogeys;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlock;
import cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlockEntity;
import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyModels;
import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyProxyComposition;
import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyRenderers;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.track.TrackMaterial;
import io.micronaut.context.annotation.Context;
import lib.kasuga.create.registration.bogey.BogeyBlockReg;
import lib.kasuga.create.registration.bogey.BogeyGroupReg;
import lib.kasuga.create.registration.bogey.BogeySizeReg;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;
import lib.kasuga.rendering.models.mc.proxies.BlockEntityProxyDefinitions;
import lib.kasuga.rendering.models.mc.proxies.ModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Context
public final class AllLocoBogeys {

    public static final RegistryGroup LOCO_BOGEY_REGISTRY = new RegistryGroup()
            .configure(BlockRegConfigurations.adopt(block -> block
                    .noOcclusion()
                    .strength(2.0f, 3f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)))
            .setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    private static final Set<TrackMaterial.TrackType> STANDARD_VALID_TYPES =
            Set.of(TrackMaterial.TrackType.STANDARD);

    public static final BogeyGroupReg LOCO_GROUP =
            new BogeyGroupReg("loco", "kuayue_bogey")
                    .displayName(Component.translatable("kuayue.bogey.style.loco"))
                    .setParent(LOCO_BOGEY_REGISTRY);
    public static final BogeyGroupReg ASYMMETRY_LOCO_GROUP =
            new BogeyGroupReg("asymmetry_loco", "kuayue_bogey")
                    .displayName(Component.translatable("kuayue.bogey.style.asymmetry_loco"))
                    .setParent(LOCO_BOGEY_REGISTRY);
    public static final BogeyGroupReg METER_LOCO_GROUP =
            new BogeyGroupReg("meter_loco", "kuayue_bogey_2")
                    .displayName(Component.translatable("kuayue.bogey.style.meter_loco"))
                    .setParent(LOCO_BOGEY_REGISTRY);
    public static final BogeyGroupReg ANDESITE_LOCO_GROUP =
            new BogeyGroupReg("andesite_loco", "standard")
                    .displayName(Component.translatable("kuayue.bogey.style.andesite_loco"))
                    .withProperty(ResourceLocation.class, AllLocoBogeys::createStandardCycle)
                    .setParent(LOCO_BOGEY_REGISTRY);

    public static final BogeySizeReg DF11G = size("df11g");
    public static final BogeySizeReg DF11G_BACKWARD = size("df11g_backward");
    public static final BogeySizeReg QJ_MAIN = size("qj_main");
    public static final BogeySizeReg QJ_GUIDE = size("qj_guide");
    public static final BogeySizeReg SS3 = size("ss3");
    public static final BogeySizeReg SS3_BACKWARD = size("ss3_backward");
    public static final BogeySizeReg HXD3D = size("hxd3d");
    public static final BogeySizeReg HXD3D_BACKWARD = size("hxd3d_backward");
    public static final BogeySizeReg SS8 = size("ss8");
    public static final BogeySizeReg SS8_BACKWARD = size("ss8_backward");
    public static final BogeySizeReg DFH21_S = size("dfh21_s");
    public static final BogeySizeReg DFH21_S_BACKWARD = size("dfh21_backward_s");
    public static final BogeySizeReg JY290 = size("jy290");
    public static final BogeySizeReg JY290_BACKWARD = size("jy290_backward");
    public static final BogeySizeReg DF5 = size("df5");
    public static final BogeySizeReg DF5_BACKWARD = size("df5_backward");
    public static final BogeySizeReg DF21 = size("df21");
    public static final BogeySizeReg DF21_BACKWARD = size("df21_backward");
    public static final BogeySizeReg DFH21 = size("dfh21");
    public static final BogeySizeReg DFH21_BACKWARD = size("dfh21_backward");
    public static final BogeySizeReg DF11G_A = size("df11g_a");
    public static final BogeySizeReg DF11G_BACKWARD_A = size("df11g_backward_a");
    public static final BogeySizeReg QJ_MAIN_A = size("qj_main_a");
    public static final BogeySizeReg QJ_GUIDE_A = size("qj_guide_bogey_a");
    public static final BogeySizeReg SS3_A = size("ss3_a");
    public static final BogeySizeReg SS3_BACKWARD_A = size("ss3_backward_a");
    public static final BogeySizeReg HXD3D_A = size("hxd3d_a");
    public static final BogeySizeReg HXD3D_BACKWARD_A = size("hxd3d_backward_a");
    public static final BogeySizeReg SS8_A = size("ss8_a");
    public static final BogeySizeReg SS8_BACKWARD_A = size("ss8_backward_a");
    public static final BogeySizeReg DFH21_S_A = size("dfh21_a");
    public static final BogeySizeReg DFH21_S_BACKWARD_A = size("dfh21_backward_a");

    public static final BogeyBlockReg<CommonBogeyBlock> DF11G_BLOCK =
            block("df11g_bogey", DF11G, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DF11G_BACKWARD_BLOCK =
            block("df11g_backward_bogey", DF11G_BACKWARD, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> QJ_MAIN_BLOCK =
            block("qj_bogey", QJ_MAIN, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> SS3_BLOCK =
            block("ss3_bogey", SS3, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> SS3_BACKWARD_BLOCK =
            block("ss3_backward_bogey", SS3_BACKWARD, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> HXD3D_BLOCK =
            block("hxd3d_bogey", HXD3D, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> HXD3D_BACKWARD_BLOCK =
            block("hxd3d_backward_bogey", HXD3D_BACKWARD, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> SS8_BLOCK =
            block("ss8_bogey", SS8, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> SS8_BACKWARD_BLOCK =
            block("ss8_backward_bogey", SS8_BACKWARD, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DFH21_S_BLOCK =
            block("dfh21_bogey_s", DFH21_S, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DFH21_S_BACKWARD_BLOCK =
            block("dfh21_backward_bogey_s", DFH21_S_BACKWARD, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> JY290_BLOCK =
            block("jy290_bogey", JY290, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> JY290_BACKWARD_BLOCK =
            block("jy290_backward_bogey", JY290_BACKWARD, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DF5_BLOCK =
            block("df5_bogey", DF5, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DF5_BACKWARD_BLOCK =
            block("df5_backward_bogey", DF5_BACKWARD, () -> LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> QJ_GUIDE_BLOCK =
            block("qj_guide_bogey", QJ_GUIDE, () -> ASYMMETRY_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DF21_BLOCK =
            block("df21_bogey", DF21, () -> METER_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DF21_BACKWARD_BLOCK =
            block("df21_backward_bogey", DF21_BACKWARD, () -> METER_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DFH21_BLOCK =
            block("dfh21_bogey", DFH21, () -> METER_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DFH21_BACKWARD_BLOCK =
            block("dfh21_backward_bogey", DFH21_BACKWARD, () -> METER_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DF11G_A_BLOCK =
            block("df11g_bogey_a", DF11G_A, () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DF11G_BACKWARD_A_BLOCK =
            block("df11g_backward_bogey_a", DF11G_BACKWARD_A,
                    () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> QJ_MAIN_A_BLOCK =
            block("qj_bogey_a", QJ_MAIN_A, () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> QJ_GUIDE_A_BLOCK =
            block("qj_guide_bogey_a", QJ_GUIDE_A, () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> SS3_A_BLOCK =
            block("ss3_bogey_a", SS3_A, () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> SS3_BACKWARD_A_BLOCK =
            block("ss3_backward_bogey_a", SS3_BACKWARD_A,
                    () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> HXD3D_A_BLOCK =
            block("hxd3d_bogey_a", HXD3D_A, () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> HXD3D_BACKWARD_A_BLOCK =
            block("hxd3d_backward_bogey_a", HXD3D_BACKWARD_A,
                    () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> SS8_A_BLOCK =
            block("ss8_bogey_a", SS8_A, () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> SS8_BACKWARD_A_BLOCK =
            block("ss8_backward_bogey_a", SS8_BACKWARD_A,
                    () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DFH21_S_A_BLOCK =
            block("dfh21_bogey_a", DFH21_S_A, () -> ANDESITE_LOCO_GROUP.getEntry());
    public static final BogeyBlockReg<CommonBogeyBlock> DFH21_S_BACKWARD_A_BLOCK =
            block("dfh21_backward_bogey_a", DFH21_S_BACKWARD_A,
                    () -> ANDESITE_LOCO_GROUP.getEntry());

    public static final BogeySpec DF11G_SPEC =
            spec(DF11G, DF11G_BLOCK, () -> KuaYueBogeyModels.df11g(false, false));
    public static final BogeySpec DF11G_BACKWARD_SPEC =
            spec(DF11G_BACKWARD, DF11G_BACKWARD_BLOCK, () -> KuaYueBogeyModels.df11g(true, false));
    public static final BogeySpec QJ_MAIN_SPEC =
            spec(QJ_MAIN, QJ_MAIN_BLOCK, () -> KuaYueBogeyModels.qjMain(false));
    public static final BogeySpec SS3_SPEC =
            spec(SS3, SS3_BLOCK, () -> KuaYueBogeyModels.ss3(false, false));
    public static final BogeySpec SS3_BACKWARD_SPEC =
            spec(SS3_BACKWARD, SS3_BACKWARD_BLOCK, () -> KuaYueBogeyModels.ss3(true, false));
    public static final BogeySpec HXD3D_SPEC =
            spec(HXD3D, HXD3D_BLOCK, () -> KuaYueBogeyModels.hxd3d(false, false));
    public static final BogeySpec HXD3D_BACKWARD_SPEC =
            spec(HXD3D_BACKWARD, HXD3D_BACKWARD_BLOCK,
                    () -> KuaYueBogeyModels.hxd3d(true, false));
    public static final BogeySpec SS8_SPEC =
            spec(SS8, SS8_BLOCK, () -> KuaYueBogeyModels.ss8(false, false));
    public static final BogeySpec SS8_BACKWARD_SPEC =
            spec(SS8_BACKWARD, SS8_BACKWARD_BLOCK, () -> KuaYueBogeyModels.ss8(true, false));
    public static final BogeySpec DFH21_S_SPEC =
            spec(DFH21_S, DFH21_S_BLOCK, () -> KuaYueBogeyModels.dfh21(true));
    public static final BogeySpec DFH21_S_BACKWARD_SPEC =
            spec(DFH21_S_BACKWARD, DFH21_S_BACKWARD_BLOCK,
                    () -> KuaYueBogeyModels.dfh21(true));
    public static final BogeySpec JY290_SPEC =
            spec(JY290, JY290_BLOCK, () -> KuaYueBogeyModels.jy290(false));
    public static final BogeySpec JY290_BACKWARD_SPEC =
            spec(JY290_BACKWARD, JY290_BACKWARD_BLOCK, () -> KuaYueBogeyModels.jy290(true));
    public static final BogeySpec DF5_SPEC =
            spec(DF5, DF5_BLOCK, () -> KuaYueBogeyModels.df5(false));
    public static final BogeySpec DF5_BACKWARD_SPEC =
            spec(DF5_BACKWARD, DF5_BACKWARD_BLOCK, () -> KuaYueBogeyModels.df5(true));
    public static final BogeySpec QJ_GUIDE_SPEC =
            spec(QJ_GUIDE, QJ_GUIDE_BLOCK, () -> KuaYueBogeyModels.qjGuide(false));
    public static final BogeySpec DF21_SPEC =
            spec(DF21, DF21_BLOCK, () -> KuaYueBogeyModels.df21(false));
    public static final BogeySpec DF21_BACKWARD_SPEC =
            spec(DF21_BACKWARD, DF21_BACKWARD_BLOCK, () -> KuaYueBogeyModels.df21(true));
    public static final BogeySpec DFH21_SPEC =
            spec(DFH21, DFH21_BLOCK, () -> KuaYueBogeyModels.dfh21(false));
    public static final BogeySpec DFH21_BACKWARD_SPEC =
            spec(DFH21_BACKWARD, DFH21_BACKWARD_BLOCK,
                    () -> KuaYueBogeyModels.dfh21(false));
    public static final BogeySpec DF11G_A_SPEC =
            spec(DF11G_A, DF11G_A_BLOCK, () -> KuaYueBogeyModels.df11g(false, true));
    public static final BogeySpec DF11G_BACKWARD_A_SPEC =
            spec(DF11G_BACKWARD_A, DF11G_BACKWARD_A_BLOCK,
                    () -> KuaYueBogeyModels.df11g(false, true));
    public static final BogeySpec QJ_MAIN_A_SPEC =
            spec(QJ_MAIN_A, QJ_MAIN_A_BLOCK, () -> KuaYueBogeyModels.qjMain(true));
    public static final BogeySpec QJ_GUIDE_A_SPEC =
            spec(QJ_GUIDE_A, QJ_GUIDE_A_BLOCK, () -> KuaYueBogeyModels.qjGuide(true));
    public static final BogeySpec SS3_A_SPEC =
            spec(SS3_A, SS3_A_BLOCK, () -> KuaYueBogeyModels.ss3(false, true));
    public static final BogeySpec SS3_BACKWARD_A_SPEC =
            spec(SS3_BACKWARD_A, SS3_BACKWARD_A_BLOCK,
                    () -> KuaYueBogeyModels.ss3(false, true));
    public static final BogeySpec HXD3D_A_SPEC =
            spec(HXD3D_A, HXD3D_A_BLOCK, () -> KuaYueBogeyModels.hxd3d(false, true));
    public static final BogeySpec HXD3D_BACKWARD_A_SPEC =
            spec(HXD3D_BACKWARD_A, HXD3D_BACKWARD_A_BLOCK,
                    () -> KuaYueBogeyModels.hxd3d(false, true));
    public static final BogeySpec SS8_A_SPEC =
            spec(SS8_A, SS8_A_BLOCK, () -> KuaYueBogeyModels.ss8(false, true));
    public static final BogeySpec SS8_BACKWARD_A_SPEC =
            spec(SS8_BACKWARD_A, SS8_BACKWARD_A_BLOCK,
                    () -> KuaYueBogeyModels.ss8(false, true));
    public static final BogeySpec DFH21_S_A_SPEC =
            spec(DFH21_S_A, DFH21_S_A_BLOCK, () -> KuaYueBogeyModels.dfh21(true));
    public static final BogeySpec DFH21_S_BACKWARD_A_SPEC =
            spec(DFH21_S_BACKWARD_A, DFH21_S_BACKWARD_A_BLOCK,
                    () -> KuaYueBogeyModels.dfh21(true));

    public static final List<BogeySpec> SPECS = List.of(
            DF11G_SPEC, DF11G_BACKWARD_SPEC, QJ_MAIN_SPEC, SS3_SPEC,
            SS3_BACKWARD_SPEC, HXD3D_SPEC, HXD3D_BACKWARD_SPEC, SS8_SPEC,
            SS8_BACKWARD_SPEC, DFH21_S_SPEC, DFH21_S_BACKWARD_SPEC, JY290_SPEC,
            JY290_BACKWARD_SPEC, DF5_SPEC, DF5_BACKWARD_SPEC, QJ_GUIDE_SPEC,
            DF21_SPEC, DF21_BACKWARD_SPEC, DFH21_SPEC, DFH21_BACKWARD_SPEC,
            DF11G_A_SPEC, DF11G_BACKWARD_A_SPEC, QJ_MAIN_A_SPEC, QJ_GUIDE_A_SPEC,
            SS3_A_SPEC, SS3_BACKWARD_A_SPEC, HXD3D_A_SPEC, HXD3D_BACKWARD_A_SPEC,
            SS8_A_SPEC, SS8_BACKWARD_A_SPEC, DFH21_S_A_SPEC, DFH21_S_BACKWARD_A_SPEC
    );

    public static final List<KuaYueBogeyRenderers.Binding> RENDERERS =
            SPECS.stream().map(BogeySpec::renderer).toList();

    public static final BlockEntityReg<CommonBogeyBlockEntity> LOCO_BE =
            new BlockEntityReg<CommonBogeyBlockEntity>(
                    "loco_bogey_entity",
                    reg -> (pos, state) -> new CommonBogeyBlockEntity(reg.getEntry(), pos, state)
            )
                    .validBlocks(
                            DF11G_BLOCK::getEntry, DF11G_BACKWARD_BLOCK::getEntry,
                            QJ_MAIN_BLOCK::getEntry, QJ_GUIDE_BLOCK::getEntry,
                            SS3_BLOCK::getEntry, SS3_BACKWARD_BLOCK::getEntry,
                            HXD3D_BLOCK::getEntry, HXD3D_BACKWARD_BLOCK::getEntry,
                            SS8_BLOCK::getEntry, SS8_BACKWARD_BLOCK::getEntry,
                            DFH21_S_BLOCK::getEntry, DFH21_S_BACKWARD_BLOCK::getEntry,
                            JY290_BLOCK::getEntry, JY290_BACKWARD_BLOCK::getEntry,
                            DF5_BLOCK::getEntry, DF5_BACKWARD_BLOCK::getEntry,
                            DF21_BLOCK::getEntry, DF21_BACKWARD_BLOCK::getEntry,
                            DFH21_BLOCK::getEntry, DFH21_BACKWARD_BLOCK::getEntry,
                            DF11G_A_BLOCK::getEntry, DF11G_BACKWARD_A_BLOCK::getEntry,
                            QJ_MAIN_A_BLOCK::getEntry, QJ_GUIDE_A_BLOCK::getEntry,
                            SS3_A_BLOCK::getEntry, SS3_BACKWARD_A_BLOCK::getEntry,
                            HXD3D_A_BLOCK::getEntry, HXD3D_BACKWARD_A_BLOCK::getEntry,
                            SS8_A_BLOCK::getEntry, SS8_BACKWARD_A_BLOCK::getEntry,
                            DFH21_S_A_BLOCK::getEntry, DFH21_S_BACKWARD_A_BLOCK::getEntry,
                            AllCarriageBogeys.CW2_BLOCK::getEntry,
                            AllCarriageBogeys.PK209P_BLOCK::getEntry,
                            AllCarriageBogeys.PK209P_BACKWARD_BLOCK::getEntry,
                            AllCarriageBogeys.PK209P_NO_MOTOR_BLOCK::getEntry,
                            AllCarriageBogeys.SW220K_BLOCK::getEntry,
                            AllCarriageBogeys.SW220K_BACKWARD_BLOCK::getEntry,
                            AllCarriageBogeys.SW160_BLOCK::getEntry,
                            AllCarriageBogeys.ZK6_BLOCK::getEntry,
                            AllCarriageBogeys.MKZ_BLOCK::getEntry,
                            AllCarriageBogeys.TKZ2_BLOCK::getEntry,
                            AllCarriageBogeys.TKZ2_MOTOR_BLOCK::getEntry,
                            AllCarriageBogeys.TKZ2_MOTOR_BACKWARD_BLOCK::getEntry,
                            AllCarriageBogeys.TKZ1_BLOCK::getEntry,
                            AllCarriageBogeys.CW2_A_BLOCK::getEntry,
                            AllCarriageBogeys.PK209P_A_BLOCK::getEntry,
                            AllCarriageBogeys.PK209P_BACKWARD_A_BLOCK::getEntry,
                            AllCarriageBogeys.PK209P_NO_MOTOR_A_BLOCK::getEntry,
                            AllCarriageBogeys.SW220K_A_BLOCK::getEntry,
                            AllCarriageBogeys.SW220K_BACKWARD_A_BLOCK::getEntry,
                            AllCarriageBogeys.SW160_A_BLOCK::getEntry,
                            AllCarriageBogeys.ZK6_A_BLOCK::getEntry
                    )
                    .onRegistered(type -> BlockEntityProxyDefinitions.register(
                            type,
                            ResourceLocation.fromNamespaceAndPath("kuayue", "loco_bogey"),
                            KuaYueBogeyProxyComposition::create
                    ))
                    .setParent(LOCO_BOGEY_REGISTRY);

    static {
        configureGroup(LOCO_GROUP, SPECS.subList(0, 15));
        configureGroup(ASYMMETRY_LOCO_GROUP, SPECS.subList(15, 16));
        configureGroup(METER_LOCO_GROUP, SPECS.subList(16, 20));
        configureGroup(ANDESITE_LOCO_GROUP, SPECS.subList(20, SPECS.size()));
        CommonBogeyBlock.setGlobalBlockEntityType(LOCO_BE::getEntry);
    }

    public AllLocoBogeys() {
    }

    private static BogeySizeReg size(String path) {
        return new BogeySizeReg(path, 0.4575f)
                .setParent(LOCO_BOGEY_REGISTRY);
    }

    private static BogeyBlockReg<CommonBogeyBlock> block(
            String name,
            BogeySizeReg size,
            Supplier<BogeyStyle> style
    ) {
        return new BogeyBlockReg<>(
                name,
                size,
                (properties, resolvedSize) -> new CommonBogeyBlock(
                        properties,
                        resolvedSize,
                        style,
                        TrackMaterial.TrackType.STANDARD,
                        STANDARD_VALID_TYPES,
                        2.0,
                        new Vec3(0, 7 / 32f, 1),
                        false
                )
        ).withBlockProperties(properties -> properties
                        .noOcclusion()
                        .strength(2.0f, 3f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.NETHERITE_BLOCK))
                .setParent(LOCO_BOGEY_REGISTRY);
    }

    private static BogeySpec spec(
            BogeySizeReg size,
            BogeyBlockReg<CommonBogeyBlock> block,
            Supplier<List<ModelPart>> models
    ) {
        ResourceLocation sizeId = ResourceLocation.fromNamespaceAndPath("kuayue", size.getName());
        return new BogeySpec(size, block, KuaYueBogeyRenderers.of(sizeId, size), models);
    }

    private static void configureGroup(BogeyGroupReg group, List<BogeySpec> specs) {
        specs.forEach(spec -> group.bogey(
                spec.size(), spec.block(), spec.renderer().handle()));
        group.onCommitted(style -> specs.forEach(spec -> KuaYueBogeyModels.register(
                style,
                spec.size().getEntry(),
                spec.models().get()
        )));
    }

    private static ResourceLocation createStandardCycle(ResourceLocation id) {
        return id.getNamespace().equals("kuayue") && id.getPath().equals("standard")
                ? ResourceLocation.fromNamespaceAndPath("create", "standard")
                : id;
    }

    public record BogeySpec(
            BogeySizeReg size,
            BogeyBlockReg<CommonBogeyBlock> block,
            KuaYueBogeyRenderers.Binding renderer,
            Supplier<List<ModelPart>> models
    ) {
    }
}
