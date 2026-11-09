package cn.kycraft.kuayue.parts.bogeys;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlock;
import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyModels;
import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyRenderers;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.track.TrackMaterial;
import io.micronaut.context.annotation.Context;
import lib.kasuga.create.registration.bogey.BogeyBlockReg;
import lib.kasuga.create.registration.bogey.BogeyGroupReg;
import lib.kasuga.create.registration.bogey.BogeySizeReg;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.rendering.models.mc.proxies.ModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Context
public final class AllCarriageBogeys {

    public static final RegistryGroup CARRIAGE_BOGEY_REGISTRY = new RegistryGroup()
            .configure(BlockRegConfigurations.adopt(block -> block
                    .noOcclusion()
                    .strength(1.5f, 3f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)))
            .setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    private static final Set<TrackMaterial.TrackType> STANDARD_VALID_TYPES =
            Set.of(TrackMaterial.TrackType.STANDARD);
    private static final Set<TrackMaterial.TrackType> METER_VALID_TYPES =
            Set.of(TrackMaterial.TrackType.STANDARD);

    public static final BogeyGroupReg CARRIAGE_GROUP =
            new BogeyGroupReg("carriage", "kuayue_bogey")
                    .displayName(Component.translatable("kuayue.bogey.style.carriage"))
                    .setParent(CARRIAGE_BOGEY_REGISTRY);
    public static final BogeyGroupReg METER_CARRIAGE_GROUP =
            new BogeyGroupReg("meter_carriage", "kuayue_bogey_2")
                    .displayName(Component.translatable("kuayue.bogey.style.meter_carriage"))
                    .setParent(CARRIAGE_BOGEY_REGISTRY);
    public static final BogeyGroupReg ANDESITE_CARRIAGE_GROUP =
            new BogeyGroupReg("andesite_carriage", "standard")
                    .displayName(Component.translatable("kuayue.bogey.style.andesite_carriage"))
                    .withProperty(ResourceLocation.class, AllCarriageBogeys::createStandardCycle)
                    .setParent(CARRIAGE_BOGEY_REGISTRY);

    public static final BogeySizeReg PK209P = size("pk209p");
    public static final BogeySizeReg PK209P_BACKWARD = size("pk209p_backward");
    public static final BogeySizeReg PK209P_NO_MOTOR = size("pk209p_no_motor");
    public static final BogeySizeReg CW2 = size("cw2");
    public static final BogeySizeReg SW220K = size("sw220k");
    public static final BogeySizeReg SW220K_BACKWARD = size("sw220k_backward");
    public static final BogeySizeReg SW160 = size("sw160");
    public static final BogeySizeReg ZK6 = size("zk6");
    public static final BogeySizeReg MKZ = size("mkz");
    public static final BogeySizeReg TKZ2 = size("tkz2");
    public static final BogeySizeReg TKZ2_MOTOR = size("tkz2_motor");
    public static final BogeySizeReg TKZ2_MOTOR_BACKWARD = size("tkz2_motor_backward");
    public static final BogeySizeReg TKZ1 = size("tkz1");
    public static final BogeySizeReg PK209P_A = size("pk209p_a");
    public static final BogeySizeReg PK209P_BACKWARD_A = size("pk209p_backward_a");
    public static final BogeySizeReg PK209P_NO_MOTOR_A = size("pk209p_no_motor_a");
    public static final BogeySizeReg CW2_A = size("cw2_a");
    public static final BogeySizeReg SW220K_A = size("sw220k_a");
    public static final BogeySizeReg SW220K_BACKWARD_A = size("sw220k_backward_a");
    public static final BogeySizeReg SW160_A = size("sw160_a");
    public static final BogeySizeReg ZK6_A = size("zk6_a");

    public static final BogeyBlockReg<CommonBogeyBlock> CW2_BLOCK =
            block("cw2_bogey", CW2, () -> CARRIAGE_GROUP.getEntry(), 2.0);
    public static final BogeyBlockReg<CommonBogeyBlock> PK209P_BLOCK =
            block("pk209p_bogey", PK209P, () -> CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> PK209P_BACKWARD_BLOCK =
            block("pk209p_backward_bogey", PK209P_BACKWARD,
                    () -> CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> PK209P_NO_MOTOR_BLOCK =
            block("pk209p_no_motor_bogey", PK209P_NO_MOTOR,
                    () -> CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> SW220K_BLOCK =
            block("sw220k_bogey", SW220K, () -> CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> SW220K_BACKWARD_BLOCK =
            block("sw220k_backward_bogey", SW220K_BACKWARD,
                    () -> CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> SW160_BLOCK =
            block("sw160_bogey", SW160, () -> CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> ZK6_BLOCK =
            block("zk6_bogey", ZK6, () -> CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> MKZ_BLOCK =
            meterBlock("mkz_bogey", MKZ, () -> METER_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> TKZ2_BLOCK =
            meterBlock("tkz2_bogey", TKZ2, () -> METER_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> TKZ2_MOTOR_BLOCK =
            meterBlock("tkz2_motor_bogey", TKZ2_MOTOR,
                    () -> METER_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> TKZ2_MOTOR_BACKWARD_BLOCK =
            meterBlock("tkz2_motor_backward_bogey", TKZ2_MOTOR_BACKWARD,
                    () -> METER_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> TKZ1_BLOCK =
            meterBlock("tkz1_bogey", TKZ1, () -> METER_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> CW2_A_BLOCK =
            block("cw2_bogey_a", CW2_A, () -> ANDESITE_CARRIAGE_GROUP.getEntry(), 2.0);
    public static final BogeyBlockReg<CommonBogeyBlock> PK209P_A_BLOCK =
            block("pk209p_bogey_a", PK209P_A,
                    () -> ANDESITE_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> PK209P_BACKWARD_A_BLOCK =
            block("pk209p_backward_bogey_a", PK209P_BACKWARD_A,
                    () -> ANDESITE_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> PK209P_NO_MOTOR_A_BLOCK =
            block("pk209p_no_motor_bogey_a", PK209P_NO_MOTOR_A,
                    () -> ANDESITE_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> SW220K_A_BLOCK =
            block("sw220k_bogey_a", SW220K_A,
                    () -> ANDESITE_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> SW220K_BACKWARD_A_BLOCK =
            block("sw220k_backward_bogey_a", SW220K_BACKWARD_A,
                    () -> ANDESITE_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> SW160_A_BLOCK =
            block("sw160_bogey_a", SW160_A,
                    () -> ANDESITE_CARRIAGE_GROUP.getEntry(), 2.5);
    public static final BogeyBlockReg<CommonBogeyBlock> ZK6_A_BLOCK =
            block("zk6_bogey_a", ZK6_A,
                    () -> ANDESITE_CARRIAGE_GROUP.getEntry(), 2.5);

    public static final List<BogeySpec> SPECS = List.of(
            spec(CW2, CW2_BLOCK, () -> KuaYueBogeyModels.cw2(false)),
            spec(PK209P, PK209P_BLOCK, () -> KuaYueBogeyModels.pk209p(false, false)),
            spec(PK209P_BACKWARD, PK209P_BACKWARD_BLOCK,
                    () -> KuaYueBogeyModels.pk209p(true, false)),
            spec(PK209P_NO_MOTOR, PK209P_NO_MOTOR_BLOCK,
                    () -> KuaYueBogeyModels.pk209pNoMotor(false, false)),
            spec(SW220K, SW220K_BLOCK, () -> KuaYueBogeyModels.sw220k(false)),
            spec(SW220K_BACKWARD, SW220K_BACKWARD_BLOCK, () -> KuaYueBogeyModels.sw220k(false)),
            spec(SW160, SW160_BLOCK, () -> KuaYueBogeyModels.sw160(false)),
            spec(ZK6, ZK6_BLOCK, () -> KuaYueBogeyModels.zk6(false)),
            spec(MKZ, MKZ_BLOCK, KuaYueBogeyModels::mkz),
            spec(TKZ2, TKZ2_BLOCK, KuaYueBogeyModels::tkz2),
            spec(TKZ2_MOTOR, TKZ2_MOTOR_BLOCK, () -> KuaYueBogeyModels.tkz2Motor(false)),
            spec(TKZ2_MOTOR_BACKWARD, TKZ2_MOTOR_BACKWARD_BLOCK,
                    () -> KuaYueBogeyModels.tkz2Motor(true)),
            spec(TKZ1, TKZ1_BLOCK, KuaYueBogeyModels::tkz1),
            spec(CW2_A, CW2_A_BLOCK, () -> KuaYueBogeyModels.cw2(true)),
            spec(PK209P_A, PK209P_A_BLOCK, () -> KuaYueBogeyModels.pk209p(false, true)),
            spec(PK209P_BACKWARD_A, PK209P_BACKWARD_A_BLOCK,
                    () -> KuaYueBogeyModels.pk209p(true, true)),
            spec(PK209P_NO_MOTOR_A, PK209P_NO_MOTOR_A_BLOCK,
                    () -> KuaYueBogeyModels.pk209pNoMotor(false, true)),
            spec(SW220K_A, SW220K_A_BLOCK, () -> KuaYueBogeyModels.sw220k(true)),
            spec(SW220K_BACKWARD_A, SW220K_BACKWARD_A_BLOCK,
                    () -> KuaYueBogeyModels.sw220k(true)),
            spec(SW160_A, SW160_A_BLOCK, () -> KuaYueBogeyModels.sw160(true)),
            spec(ZK6_A, ZK6_A_BLOCK, () -> KuaYueBogeyModels.zk6(true))
    );

    public static final List<KuaYueBogeyRenderers.Binding> RENDERERS =
            SPECS.stream().map(BogeySpec::renderer).toList();

    static {
        configureGroup(CARRIAGE_GROUP, SPECS.subList(0, 8));
        configureGroup(METER_CARRIAGE_GROUP, SPECS.subList(8, 13));
        configureGroup(ANDESITE_CARRIAGE_GROUP, SPECS.subList(13, SPECS.size()));
    }

    public AllCarriageBogeys() {
    }

    private static BogeySizeReg size(String path) {
        return new BogeySizeReg(path, 0.4575f)
                .setParent(CARRIAGE_BOGEY_REGISTRY);
    }

    private static BogeyBlockReg<CommonBogeyBlock> block(
            String name,
            BogeySizeReg size,
            Supplier<BogeyStyle> style,
            double spacing
    ) {
        return newBlock(name, size, style, spacing, STANDARD_VALID_TYPES);
    }

    private static BogeyBlockReg<CommonBogeyBlock> meterBlock(
            String name,
            BogeySizeReg size,
            Supplier<BogeyStyle> style,
            double spacing
    ) {
        return newBlock(name, size, style, spacing, METER_VALID_TYPES);
    }

    private static BogeyBlockReg<CommonBogeyBlock> newBlock(
            String name,
            BogeySizeReg size,
            Supplier<BogeyStyle> style,
            double spacing,
            Set<TrackMaterial.TrackType> validTypes
    ) {
        return new BogeyBlockReg<>(
                name,
                size,
                (properties, resolvedSize) -> new CommonBogeyBlock(
                        properties,
                        resolvedSize,
                        style,
                        TrackMaterial.TrackType.STANDARD,
                        validTypes,
                        spacing,
                        new Vec3(0, 7 / 32f, 1),
                        true
                )
        ).withBlockProperties(properties -> properties
                        .noOcclusion()
                        .strength(1.5f, 3f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.NETHERITE_BLOCK))
                .setParent(CARRIAGE_BOGEY_REGISTRY);
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
