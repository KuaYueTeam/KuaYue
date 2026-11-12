package cn.kycraft.kuayue.parts.bogeys.data;

import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;

/**
 * Compiles reusable motion profiles from {@code bogey_motions}.
 */
public final class BogeyMotionTypeHandler implements TypeHandler<BogeyMotionDefinition> {
    private final BogeyDefinitionParser parser = new BogeyDefinitionParser();

    @Override
    public String getTypeName() {
        return BogeyDataDrivenTypes.MOTIONS;
    }

    @Override
    public int getPhase() {
        return 20;
    }

    @Override
    public BogeyMotionDefinition parse(JsonObject json) {
        return parser.parseMotion(json);
    }

    @Override
    public void apply(BogeyMotionDefinition definition, BuildContext context) {
        context.putMeta(
                BogeyDataDrivenTypes.MOTION_PROFILE,
                definition.id(),
                CompiledBogeyMotion.compile(definition)
        );
        context.putMeta(
                BogeyDataDrivenTypes.MOTION_DEFINITION,
                definition.id(),
                definition
        );
    }

    @Override
    public String resolveIdentity(String modId, BogeyMotionDefinition definition) {
        return EffectiveId.of(modId, definition.id());
    }
}
