package cn.kycraft.kuayue.parts.bogeys;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.bogeys.core.BogeyVisualReg;
import cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlock;
import cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlockEntity;
import cn.kycraft.kuayue.parts.bogeys.renderer.*;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeySizes.BogeySize;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.BogeyStyle.SizeRenderer;
import com.simibubi.create.content.trains.track.TrackMaterial;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityRendererReg;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

@Context
public class AllLocoBogeys {

    public static final RegistryGroup LOCO_BOGEY_REGISTRY = new RegistryGroup()
            .configure(BlockRegConfigurations.adopt(block -> block
                    .noOcclusion()
                    .strength(2.0f, 3f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)))
            .setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    private static ResourceLocation rl(String path) {
        return ResourceLocation.parse("kuayue:" + path);
    }

    // ============== Track type compatibility ==============
    private static final Set<TrackMaterial.TrackType> STANDARD_VALID_TYPES = Set.of(TrackMaterial.TrackType.STANDARD);

    // ============== BogeySizes ==============
    // Standard loco
    public static final BogeySize DF11G = regSize(rl("df11g"), 0.4575F);
    public static final BogeySize DF11G_BACKWARD = regSize(rl("df11g_backward"), 0.4575F);
    public static final BogeySize QJ_MAIN = regSize(rl("qj_main"), 0.4575F);
    public static final BogeySize QJ_GUIDE = regSize(rl("qj_guide"), 0.4575F);
    public static final BogeySize SS3 = regSize(rl("ss3"), 0.4575F);
    public static final BogeySize SS3_BACKWARD = regSize(rl("ss3_backward"), 0.4575F);
    public static final BogeySize HXD3D = regSize(rl("hxd3d"), 0.4575F);
    public static final BogeySize HXD3D_BACKWARD = regSize(rl("hxd3d_backward"), 0.4575F);
    public static final BogeySize SS8 = regSize(rl("ss8"), 0.4575F);
    public static final BogeySize SS8_BACKWARD = regSize(rl("ss8_backward"), 0.4575F);
    public static final BogeySize DFH21_S = regSize(rl("dfh21_s"), 0.4575F);
    public static final BogeySize DFH21_S_BACKWARD = regSize(rl("dfh21_backward_s"), 0.4575F);
    public static final BogeySize JY290 = regSize(rl("jy290"), 0.4575F);
    public static final BogeySize JY290_BACKWARD = regSize(rl("jy290_backward"), 0.4575F);
    public static final BogeySize DF5 = regSize(rl("df5"), 0.4575F);
    public static final BogeySize DF5_BACKWARD = regSize(rl("df5_backward"), 0.4575F);
    // Meter loco
    public static final BogeySize DF21 = regSize(rl("df21"), 0.4575F);
    public static final BogeySize DF21_BACKWARD = regSize(rl("df21_backward"), 0.4575F);
    public static final BogeySize DFH21 = regSize(rl("dfh21"), 0.4575F);
    public static final BogeySize DFH21_BACKWARD = regSize(rl("dfh21_backward"), 0.4575F);

    // ============== Andesite BogeySizes ==============
    public static final BogeySize DF11G_A = regSize(rl("df11g_a"), 0.4575F);
    public static final BogeySize DF11G_BACKWARD_A = regSize(rl("df11g_backward_a"), 0.4575F);
    public static final BogeySize QJ_MAIN_A = regSize(rl("qj_main_a"), 0.4575F);
    public static final BogeySize QJ_GUIDE_A = regSize(rl("qj_guide_bogey_a"), 0.4575F);
    public static final BogeySize SS3_A = regSize(rl("ss3_a"), 0.4575F);
    public static final BogeySize SS3_BACKWARD_A = regSize(rl("ss3_backward_a"), 0.4575F);
    public static final BogeySize HXD3D_A = regSize(rl("hxd3d_a"), 0.4575F);
    public static final BogeySize HXD3D_BACKWARD_A = regSize(rl("hxd3d_backward_a"), 0.4575F);
    public static final BogeySize SS8_A = regSize(rl("ss8_a"), 0.4575F);
    public static final BogeySize SS8_BACKWARD_A = regSize(rl("ss8_backward_a"), 0.4575F);
    public static final BogeySize DFH21_S_A = regSize(rl("dfh21_a"), 0.4575F);
    public static final BogeySize DFH21_S_BACKWARD_A = regSize(rl("dfh21_backward_a"), 0.4575F);

    private static BogeySize regSize(ResourceLocation id, float radius) {
        var size = new BogeySize(id, radius);
        BogeySizes.register(size);
        return size;
    }

    // ============== Style references ==============
    private static BogeyStyle LOCO_STYLE;
    private static BogeyStyle METER_LOCO_STYLE;
    private static BogeyStyle ASYMMETRY_LOCO_STYLE;
    private static BogeyStyle ANDESITE_LOCO_STYLE;

    // ============== BlockRegs ==============
    // Standard loco (allowSingleBogeyCarriage = false)
    public static final BlockReg<CommonBogeyBlock> DF11G_BLOCK = block("df11g_bogey", DF11G, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DF11G_BACKWARD_BLOCK = block("df11g_backward_bogey", DF11G_BACKWARD, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> QJ_MAIN_BLOCK = block("qj_bogey", QJ_MAIN, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> SS3_BLOCK = block("ss3_bogey", SS3, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> SS3_BACKWARD_BLOCK = block("ss3_backward_bogey", SS3_BACKWARD, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> HXD3D_BLOCK = block("hxd3d_bogey", HXD3D, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> HXD3D_BACKWARD_BLOCK = block("hxd3d_backward_bogey", HXD3D_BACKWARD, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> SS8_BLOCK = block("ss8_bogey", SS8, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> SS8_BACKWARD_BLOCK = block("ss8_backward_bogey", SS8_BACKWARD, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DFH21_S_BLOCK = block("dfh21_bogey_s", DFH21_S, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DFH21_S_BACKWARD_BLOCK = block("dfh21_backward_bogey_s", DFH21_S_BACKWARD, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> JY290_BLOCK = block("jy290_bogey", JY290, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> JY290_BACKWARD_BLOCK = block("jy290_backward_bogey", JY290_BACKWARD, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DF5_BLOCK = block("df5_bogey", DF5, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DF5_BACKWARD_BLOCK = block("df5_backward_bogey", DF5_BACKWARD, () -> LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    // Asymmetry loco
    public static final BlockReg<CommonBogeyBlock> QJ_GUIDE_BLOCK = block("qj_guide_bogey", QJ_GUIDE, () -> ASYMMETRY_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    // Meter loco
    public static final BlockReg<CommonBogeyBlock> DF21_BLOCK = block("df21_bogey", DF21, () -> METER_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DF21_BACKWARD_BLOCK = block("df21_backward_bogey", DF21_BACKWARD, () -> METER_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DFH21_BLOCK = block("dfh21_bogey", DFH21, () -> METER_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DFH21_BACKWARD_BLOCK = block("dfh21_backward_bogey", DFH21_BACKWARD, () -> METER_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);

    // ============== Andesite Loco BlockRegs ==============
    public static final BlockReg<CommonBogeyBlock> DF11G_A_BLOCK = block("df11g_bogey_a", DF11G_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DF11G_BACKWARD_A_BLOCK = block("df11g_backward_bogey_a", DF11G_BACKWARD_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> QJ_MAIN_A_BLOCK = block("qj_bogey_a", QJ_MAIN_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> QJ_GUIDE_A_BLOCK = block("qj_guide_bogey_a", QJ_GUIDE_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> SS3_A_BLOCK = block("ss3_bogey_a", SS3_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> SS3_BACKWARD_A_BLOCK = block("ss3_backward_bogey_a", SS3_BACKWARD_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> HXD3D_A_BLOCK = block("hxd3d_bogey_a", HXD3D_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> HXD3D_BACKWARD_A_BLOCK = block("hxd3d_backward_bogey_a", HXD3D_BACKWARD_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> SS8_A_BLOCK = block("ss8_bogey_a", SS8_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> SS8_BACKWARD_A_BLOCK = block("ss8_backward_bogey_a", SS8_BACKWARD_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DFH21_S_A_BLOCK = block("dfh21_bogey_a", DFH21_S_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);
    public static final BlockReg<CommonBogeyBlock> DFH21_S_BACKWARD_A_BLOCK = block("dfh21_backward_bogey_a", DFH21_S_BACKWARD_A, () -> ANDESITE_LOCO_STYLE, 2.0, new Vec3(0, 7 / 32f, 1), false);

    private static BlockReg<CommonBogeyBlock> block(String name, BogeySize size,
                                                      java.util.function.Supplier<BogeyStyle> style,
                                                      double wheelSpacing, Vec3 connectorOffset, boolean allowSingle) {
        return BlockReg.of(name, p -> new CommonBogeyBlock(p, size, style.get(),
                        TrackMaterial.TrackType.STANDARD, STANDARD_VALID_TYPES,
                        wheelSpacing, connectorOffset, allowSingle))
                .setParent(LOCO_BOGEY_REGISTRY);
    }

    // ============== BlockEntity (includes all loco + carriage blocks) ==============
    public static final BlockEntityReg<CommonBogeyBlockEntity> LOCO_BE =
            BlockEntityReg.of("loco_bogey_entity", CommonBogeyBlockEntity::new)
                    .validBlocks(
                            // Standard loco
                            DF11G_BLOCK::getEntry, DF11G_BACKWARD_BLOCK::getEntry,
                            QJ_MAIN_BLOCK::getEntry, QJ_GUIDE_BLOCK::getEntry,
                            SS3_BLOCK::getEntry, SS3_BACKWARD_BLOCK::getEntry,
                            HXD3D_BLOCK::getEntry, HXD3D_BACKWARD_BLOCK::getEntry,
                            SS8_BLOCK::getEntry, SS8_BACKWARD_BLOCK::getEntry,
                            DFH21_S_BLOCK::getEntry, DFH21_S_BACKWARD_BLOCK::getEntry,
                            JY290_BLOCK::getEntry, JY290_BACKWARD_BLOCK::getEntry,
                            DF5_BLOCK::getEntry, DF5_BACKWARD_BLOCK::getEntry,
                            // Meter loco
                            DF21_BLOCK::getEntry, DF21_BACKWARD_BLOCK::getEntry,
                            DFH21_BLOCK::getEntry, DFH21_BACKWARD_BLOCK::getEntry,
                            // Andesite loco
                            DF11G_A_BLOCK::getEntry, DF11G_BACKWARD_A_BLOCK::getEntry,
                            QJ_MAIN_A_BLOCK::getEntry, QJ_GUIDE_A_BLOCK::getEntry,
                            SS3_A_BLOCK::getEntry, SS3_BACKWARD_A_BLOCK::getEntry,
                            HXD3D_A_BLOCK::getEntry, HXD3D_BACKWARD_A_BLOCK::getEntry,
                            SS8_A_BLOCK::getEntry, SS8_BACKWARD_A_BLOCK::getEntry,
                            DFH21_S_A_BLOCK::getEntry, DFH21_S_BACKWARD_A_BLOCK::getEntry,
                            // Carriage blocks from AllCarriageBogeys
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
                            // Andesite carriage
                            AllCarriageBogeys.CW2_A_BLOCK::getEntry,
                            AllCarriageBogeys.PK209P_A_BLOCK::getEntry,
                            AllCarriageBogeys.PK209P_BACKWARD_A_BLOCK::getEntry,
                            AllCarriageBogeys.PK209P_NO_MOTOR_A_BLOCK::getEntry,
                            AllCarriageBogeys.SW220K_A_BLOCK::getEntry,
                            AllCarriageBogeys.SW220K_BACKWARD_A_BLOCK::getEntry,
                            AllCarriageBogeys.SW160_A_BLOCK::getEntry,
                            AllCarriageBogeys.ZK6_A_BLOCK::getEntry
                    )
                    .setParent(LOCO_BOGEY_REGISTRY);

    // ============== BlockEntityRenderer ==============
    public static final BlockEntityRendererReg<CommonBogeyBlockEntity> LOCO_BE_RENDERER =
            new BlockEntityRendererReg<CommonBogeyBlockEntity>(() -> context ->
                    new com.simibubi.create.content.trains.bogey.BogeyBlockEntityRenderer<>(context))
                    .withBlockEntity(LOCO_BE::getEntry)
                    .setParent(LOCO_BOGEY_REGISTRY);

    // ============== Flywheel Visual ==============
    public static final BogeyVisualReg<CommonBogeyBlockEntity> LOCO_BE_VISUAL =
            new BogeyVisualReg<CommonBogeyBlockEntity>(() -> (ctx, be, partialTick) ->
                    new cn.kycraft.kuayue.parts.bogeys.core.BogeyBlockEntityVisual(ctx, be, partialTick))
                    .withBlockEntity(LOCO_BE::getEntry)
                    .setParent(LOCO_BOGEY_REGISTRY);

    // ============== BogeyStyles ==============
    static {
        LOCO_STYLE = new BogeyStyle.Builder(rl("loco"), rl("kuayue_bogey"))
                .displayName(Component.translatable("kuayue.bogey.style.loco"))
                .size(DF11G, DF11G_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DF11GRenderer(), DF11GVisual::new))
                .size(DF11G_BACKWARD, DF11G_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DF11GRenderer.Backward(), DF11GVisual.Backward::new))
                .size(QJ_MAIN, QJ_MAIN_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new QJMainRenderer(), QJMainVisual::new))
                .size(SS3, SS3_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SS3Renderer(), SS3Visual::new))
                .size(SS3_BACKWARD, SS3_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SS3Renderer.Backward(), SS3Visual.Backward::new))
                .size(HXD3D, HXD3D_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new HXD3DRenderer(), HXD3DVisual::new))
                .size(HXD3D_BACKWARD, HXD3D_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new HXD3DRenderer.Backward(), HXD3DVisual.Backward::new))
                .size(SS8, SS8_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SS8Renderer(), SS8Visual::new))
                .size(SS8_BACKWARD, SS8_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SS8Renderer.Backward(), SS8Visual.Backward::new))
                .size(DFH21_S, DFH21_S_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DFH21Renderer.Standard(), DFH21Visual.Standard::new))
                .size(DFH21_S_BACKWARD, DFH21_S_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DFH21Renderer.Standard.Backward(), DFH21Visual.Standard::new))
                .size(JY290, JY290_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new JY290Renderer(), JY290Visual::new))
                .size(JY290_BACKWARD, JY290_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new JY290Renderer.Backward(), JY290Visual.Backward::new))
                .size(DF5, DF5_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DF5Renderer(), DF5Visual::new))
                .size(DF5_BACKWARD, DF5_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DF5Renderer.Backward(), DF5Visual.Backward::new))
                .build();

        ASYMMETRY_LOCO_STYLE = new BogeyStyle.Builder(rl("asymmetry_loco"), rl("kuayue_bogey"))
                .displayName(Component.translatable("kuayue.bogey.style.asymmetry_loco"))
                .size(QJ_GUIDE, QJ_GUIDE_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new QJGuideRenderer(), QJGuideVisual::new))
                .build();

        METER_LOCO_STYLE = new BogeyStyle.Builder(rl("meter_loco"), rl("kuayue_bogey_2"))
                .displayName(Component.translatable("kuayue.bogey.style.meter_loco"))
                .size(DF21, DF21_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DF21Renderer(), DF21Visual::new))
                .size(DF21_BACKWARD, DF21_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DF21Renderer.Backward(), DF21Visual.Backward::new))
                .size(DFH21, DFH21_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DFH21Renderer(), DFH21Visual::new))
                .size(DFH21_BACKWARD, DFH21_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DFH21Renderer.Backward(), DFH21Visual::new))
                .build();

        ANDESITE_LOCO_STYLE = new BogeyStyle.Builder(rl("andesite_loco"), ResourceLocation.parse("create:standard"))
                .displayName(Component.translatable("kuayue.bogey.style.andesite_loco"))
                .size(DF11G_A, DF11G_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DF11GRenderer.Andesite(), DF11GVisual.Andesite::new))
                .size(DF11G_BACKWARD_A, DF11G_BACKWARD_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DF11GRenderer.Andesite(), DF11GVisual.Andesite::new))
                .size(QJ_MAIN_A, QJ_MAIN_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new QJMainRenderer.Andesite(), QJMainVisual.Andesite::new))
                .size(QJ_GUIDE_A, QJ_GUIDE_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new QJGuideRenderer.Andesite(), QJGuideVisual.Andesite::new))
                .size(SS3_A, SS3_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SS3Renderer.Andesite(), SS3Visual.Andesite::new))
                .size(SS3_BACKWARD_A, SS3_BACKWARD_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SS3Renderer.Andesite(), SS3Visual.Andesite::new))
                .size(HXD3D_A, HXD3D_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new HXD3DRenderer.Andesite(), HXD3DVisual.Andesite::new))
                .size(HXD3D_BACKWARD_A, HXD3D_BACKWARD_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new HXD3DRenderer.Andesite(), HXD3DVisual.Andesite::new))
                .size(SS8_A, SS8_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SS8Renderer.Andesite(), SS8Visual.Andesite::new))
                .size(SS8_BACKWARD_A, SS8_BACKWARD_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SS8Renderer.Andesite(), SS8Visual.Andesite::new))
                .size(DFH21_S_A, DFH21_S_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DFH21Renderer.Andesite(), DFH21Visual.Andesite::new))
                .size(DFH21_S_BACKWARD_A, DFH21_S_BACKWARD_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new DFH21Renderer.Andesite(), DFH21Visual.Andesite::new))
                .build();
    }

    // ============== Override global BE type ==============
    static {
        CommonBogeyBlock.setGlobalBlockEntityType(LOCO_BE::getEntry);
    }
}
