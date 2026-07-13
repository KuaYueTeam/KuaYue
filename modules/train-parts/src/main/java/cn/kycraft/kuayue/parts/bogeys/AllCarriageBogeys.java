package cn.kycraft.kuayue.parts.bogeys;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlock;
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
// BlockEntity/Renderer/Visual registered in AllLocoBogeys
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

@Context
public class AllCarriageBogeys {

    public static final RegistryGroup CARRIAGE_BOGEY_REGISTRY = new RegistryGroup()
            .configure(BlockRegConfigurations.adopt(block -> block
                    .noOcclusion()
                    .strength(1.5f, 3f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)))
            .setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    private static ResourceLocation rl(String path) {
        return ResourceLocation.parse("kuayue:" + path);
    }

    // ============== Track type compatibility ==============
    private static final Set<TrackMaterial.TrackType> STANDARD_VALID_TYPES = Set.of(
            TrackMaterial.TrackType.STANDARD
    );
    private static final Set<TrackMaterial.TrackType> METER_VALID_TYPES = Set.of(
            TrackMaterial.TrackType.STANDARD
    );

    // ============== BogeySizes (must be registered first) ==============
    public static final BogeySize PK209P = regSize(rl("pk209p"), 0.4575F);
    public static final BogeySize PK209P_BACKWARD = regSize(rl("pk209p_backward"), 0.4575F);
    public static final BogeySize PK209P_NO_MOTOR = regSize(rl("pk209p_no_motor"), 0.4575F);
    public static final BogeySize CW2 = regSize(rl("cw2"), 0.4575F);
    public static final BogeySize SW220K = regSize(rl("sw220k"), 0.4575F);
    public static final BogeySize SW220K_BACKWARD = regSize(rl("sw220k_backward"), 0.4575F);
    public static final BogeySize SW160 = regSize(rl("sw160"), 0.4575F);
    public static final BogeySize ZK6 = regSize(rl("zk6"), 0.4575F);

    // ============== Meter BogeySizes ==============
    public static final BogeySize MKZ = regSize(rl("mkz"), 0.4575F);
    public static final BogeySize TKZ2 = regSize(rl("tkz2"), 0.4575F);
    public static final BogeySize TKZ2_MOTOR = regSize(rl("tkz2_motor"), 0.4575F);
    public static final BogeySize TKZ2_MOTOR_BACKWARD = regSize(rl("tkz2_motor_backward"), 0.4575F);
    public static final BogeySize TKZ1 = regSize(rl("tkz1"), 0.4575F);

    // ============== Andesite BogeySizes ==============
    public static final BogeySize PK209P_A = regSize(rl("pk209p_a"), 0.4575F);
    public static final BogeySize PK209P_BACKWARD_A = regSize(rl("pk209p_backward_a"), 0.4575F);
    public static final BogeySize PK209P_NO_MOTOR_A = regSize(rl("pk209p_no_motor_a"), 0.4575F);
    public static final BogeySize CW2_A = regSize(rl("cw2_a"), 0.4575F);
    public static final BogeySize SW220K_A = regSize(rl("sw220k_a"), 0.4575F);
    public static final BogeySize SW220K_BACKWARD_A = regSize(rl("sw220k_backward_a"), 0.4575F);
    public static final BogeySize SW160_A = regSize(rl("sw160_a"), 0.4575F);
    public static final BogeySize ZK6_A = regSize(rl("zk6_a"), 0.4575F);

    private static BogeySize regSize(ResourceLocation id, float radius) {
        var size = new BogeySize(id, radius);
        BogeySizes.register(size);
        return size;
    }

    // ============== BlockRegs ==============
    // The style reference is null during static init but will be resolved when blocks are
    // actually created (during registration phase, after all static init completes).
    private static BogeyStyle CARRIAGE_STYLE;
    private static BogeyStyle METER_CARRIAGE_STYLE;
    private static BogeyStyle ANDESITE_CARRIAGE_STYLE;

    public static final BlockReg<CommonBogeyBlock> CW2_BLOCK =
            block("cw2_bogey", CW2, () -> CARRIAGE_STYLE, 2.0, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> PK209P_BLOCK =
            block("pk209p_bogey", PK209P, () -> CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> PK209P_BACKWARD_BLOCK =
            block("pk209p_backward_bogey", PK209P_BACKWARD, () -> CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> PK209P_NO_MOTOR_BLOCK =
            block("pk209p_no_motor_bogey", PK209P_NO_MOTOR, () -> CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> SW220K_BLOCK =
            block("sw220k_bogey", SW220K, () -> CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> SW220K_BACKWARD_BLOCK =
            block("sw220k_backward_bogey", SW220K_BACKWARD, () -> CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> SW160_BLOCK =
            block("sw160_bogey", SW160, () -> CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> ZK6_BLOCK =
            block("zk6_bogey", ZK6, () -> CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    // ============== Meter Carriage BlockRegs ==============
    public static final BlockReg<CommonBogeyBlock> MKZ_BLOCK =
            meterBlock("mkz_bogey", MKZ, () -> METER_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> TKZ2_BLOCK =
            meterBlock("tkz2_bogey", TKZ2, () -> METER_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> TKZ2_MOTOR_BLOCK =
            meterBlock("tkz2_motor_bogey", TKZ2_MOTOR, () -> METER_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> TKZ2_MOTOR_BACKWARD_BLOCK =
            meterBlock("tkz2_motor_backward_bogey", TKZ2_MOTOR_BACKWARD, () -> METER_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    public static final BlockReg<CommonBogeyBlock> TKZ1_BLOCK =
            meterBlock("tkz1_bogey", TKZ1, () -> METER_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    // ============== Andesite Carriage BlockRegs ==============
    public static final BlockReg<CommonBogeyBlock> CW2_A_BLOCK =
            block("cw2_bogey_a", CW2_A, () -> ANDESITE_CARRIAGE_STYLE, 2.0, new Vec3(0, 7 / 32f, 1));
    public static final BlockReg<CommonBogeyBlock> PK209P_A_BLOCK =
            block("pk209p_bogey_a", PK209P_A, () -> ANDESITE_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));
    public static final BlockReg<CommonBogeyBlock> PK209P_BACKWARD_A_BLOCK =
            block("pk209p_backward_bogey_a", PK209P_BACKWARD_A, () -> ANDESITE_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));
    public static final BlockReg<CommonBogeyBlock> PK209P_NO_MOTOR_A_BLOCK =
            block("pk209p_no_motor_bogey_a", PK209P_NO_MOTOR_A, () -> ANDESITE_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));
    public static final BlockReg<CommonBogeyBlock> SW220K_A_BLOCK =
            block("sw220k_bogey_a", SW220K_A, () -> ANDESITE_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));
    public static final BlockReg<CommonBogeyBlock> SW220K_BACKWARD_A_BLOCK =
            block("sw220k_backward_bogey_a", SW220K_BACKWARD_A, () -> ANDESITE_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));
    public static final BlockReg<CommonBogeyBlock> SW160_A_BLOCK =
            block("sw160_bogey_a", SW160_A, () -> ANDESITE_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));
    public static final BlockReg<CommonBogeyBlock> ZK6_A_BLOCK =
            block("zk6_bogey_a", ZK6_A, () -> ANDESITE_CARRIAGE_STYLE, 2.5, new Vec3(0, 7 / 32f, 1));

    private static BlockReg<CommonBogeyBlock> block(String name, BogeySize size,
                                                       java.util.function.Supplier<BogeyStyle> style,
                                                       double wheelSpacing, Vec3 connectorOffset) {
        return BlockReg.of(name, p -> {
                    var b = new CommonBogeyBlock(p, size, style.get(),
                            TrackMaterial.TrackType.STANDARD, STANDARD_VALID_TYPES,
                            wheelSpacing, connectorOffset);
                    return b;
                })
                .setParent(CARRIAGE_BOGEY_REGISTRY);
    }

    private static BlockReg<CommonBogeyBlock> meterBlock(String name, BogeySize size,
                                                           java.util.function.Supplier<BogeyStyle> style,
                                                           double wheelSpacing, Vec3 connectorOffset) {
        return BlockReg.of(name, p -> {
                    var b = new CommonBogeyBlock(p, size, style.get(),
                            TrackMaterial.TrackType.STANDARD, METER_VALID_TYPES,
                            wheelSpacing, connectorOffset);
                    return b;
                })
                .setParent(CARRIAGE_BOGEY_REGISTRY);
    }

    // ============== BlockEntity / Renderer / Visual ==============
    // All registered in AllLocoBogeys via unified LOCO_BE (covers all 33 bogey blocks)

    // ============== BogeyStyle ==============
    static {
        CARRIAGE_STYLE = new BogeyStyle.Builder(rl("carriage"), rl("kuayue_bogey"))
                .displayName(Component.translatable("kuayue.bogey.style.carriage"))
                .size(CW2, CW2_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new CW2Renderer(), CW2Visual::new))
                .size(PK209P, PK209P_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new PK209PRenderer(), PK209PVisual::new))
                .size(PK209P_BACKWARD, PK209P_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new PK209PRenderer.Backward(), PK209PVisual::new))
                .size(PK209P_NO_MOTOR, PK209P_NO_MOTOR_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new PK209PRenderer.NoMotor(), PK209PVisual::new))
                .size(SW220K, SW220K_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SW220KRenderer(), SW220KVisual::new))
                .size(SW220K_BACKWARD, SW220K_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SW220KRenderer(), SW220KVisual::new))
                .size(SW160, SW160_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SW160Renderer(), SW160Visual::new))
                .size(ZK6, ZK6_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new ZK6Renderer(), ZK6Visual::new))
                .build();

        METER_CARRIAGE_STYLE = new BogeyStyle.Builder(rl("meter_carriage"), rl("kuayue_bogey_2"))
                .displayName(Component.translatable("kuayue.bogey.style.meter_carriage"))
                .size(MKZ, MKZ_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new MKZRenderer(), MKZVisual::new))
                .size(TKZ2, TKZ2_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new TKZ2Renderer(), TKZ2Visual::new))
                .size(TKZ2_MOTOR, TKZ2_MOTOR_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new TKZ2Renderer.Motor(), TKZ2Visual.Motor::new))
                .size(TKZ2_MOTOR_BACKWARD, TKZ2_MOTOR_BACKWARD_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new TKZ2Renderer.MotorBackward(), TKZ2Visual.MotorBackward::new))
                .size(TKZ1, TKZ1_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new TKZ1Renderer(), TKZ1Visual::new))
                .build();

        ANDESITE_CARRIAGE_STYLE = new BogeyStyle.Builder(rl("andesite_carriage"), ResourceLocation.parse("create:standard"))
                .displayName(Component.translatable("kuayue.bogey.style.andesite_carriage"))
                .size(CW2_A, CW2_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new CW2Renderer.Andesite(), CW2Visual.Andesite::new))
                .size(PK209P_A, PK209P_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new PK209PRenderer.Andesite(), PK209PVisual.Andesite::new))
                .size(PK209P_BACKWARD_A, PK209P_BACKWARD_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new PK209PRenderer.Andesite.Backward(), PK209PVisual.Andesite::new))
                .size(PK209P_NO_MOTOR_A, PK209P_NO_MOTOR_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new PK209PRenderer.Andesite.NoMotor(), PK209PVisual.Andesite::new))
                .size(SW220K_A, SW220K_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SW220KRenderer.Andesite(), SW220KVisual.Andesite::new))
                .size(SW220K_BACKWARD_A, SW220K_BACKWARD_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SW220KRenderer.Andesite(), SW220KVisual.Andesite::new))
                .size(SW160_A, SW160_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new SW160Renderer.Andesite(), SW160Visual.Andesite::new))
                .size(ZK6_A, ZK6_A_BLOCK::getEntry,
                        () -> () -> new SizeRenderer(new ZK6Renderer.Andesite(), ZK6Visual.Andesite::new))
                .build();
    }

}
