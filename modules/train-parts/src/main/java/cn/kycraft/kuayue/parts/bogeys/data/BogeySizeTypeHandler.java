package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonObject;
import lib.kasuga.create.registration.bogey.BogeySizeReg;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.context.RegBuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;
import net.minecraft.resources.ResourceLocation;

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
