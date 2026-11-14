package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonObject;
import lib.kasuga.create.registration.bogey.BogeySizeReg;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.context.RegBuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;
import lib.kasuga.registration.data_driven.pipeline.HandlerCapability;
import lib.kasuga.registration.data_driven.pipeline.RegistryClaim;
import lib.kasuga.registration.data_driven.validation.ValidationContext;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/**
 * Creates Create bogey size registrations from {@code bogey_sizes}.
 */
public final class BogeySizeTypeHandler implements TypeHandler<BogeySizeDefinition> {
    private final BogeyDefinitionParser parser = new BogeyDefinitionParser();

    @Override
    public String getTypeName() {
        return BogeyDataDrivenTypes.SIZES;
    }

    @Override
    public int getPhase() {
        return 10;
    }

    @Override
    public Set<HandlerCapability> capabilities() {
        return Set.of(
                HandlerCapability.STRICT_PREFLIGHT,
                HandlerCapability.REGISTRY_CLAIMS
        );
    }

    @Override
    public List<RegistryClaim> claims(
            BogeySizeDefinition definition,
            ValidationContext context
    ) {
        ResourceLocation id = ResourceLocation.parse(
                EffectiveId.of(context.hostModId(), definition.id()));
        return List.of(context.claim("create:bogey_size", id));
    }

    @Override
    public BogeySizeDefinition parse(JsonObject json) {
        return parser.parseSize(json);
    }

    @Override
    public void apply(BogeySizeDefinition definition, BuildContext baseContext) {
        if (!(baseContext instanceof RegBuildContext context)) {
            throw new IllegalArgumentException(getTypeName() + " requires RegBuildContext");
        }
        ResourceLocation id = ResourceLocation.parse(definition.id());
        BogeySizeReg reg = new BogeySizeReg(id.getPath(), definition.wheelRadius())
                .withProperty(ResourceLocation.class, ignored -> id)
                .setParent(context.getRootGroup());
        context.putReg(getTypeName(), definition.id(), reg);
        context.putMeta(BogeyDataDrivenTypes.SIZE_REG, definition.id(), reg);
    }

    @Override
    public String resolveIdentity(String modId, BogeySizeDefinition definition) {
        return EffectiveId.of(modId, definition.id());
    }
}
