package cn.kycraft.kuayue.packs.shapes;

import com.google.gson.JsonObject;
import lib.kasuga.registration.Reg;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.context.RegBuildContext;
import lib.kasuga.registration.data_driven.handler.BlockDef;
import lib.kasuga.registration.data_driven.handler.BlockTypeHandler;
import lib.kasuga.registration.data_driven.pipeline.BuildDiagnostic;
import lib.kasuga.registration.data_driven.shape.ShapeCatalog;
import lib.kasuga.registration.data_driven.validation.ValidationContext;

/**
 * Adds KuaYue's vehicle-facing shape binding adapter to the normal block registration handler.
 */
public final class KuaYueBlockTypeHandler extends BlockTypeHandler {
    private static final String LEGACY_PARAMS_FIELD = "shapes";

    @Override
    public void validate(BlockDef definition, ValidationContext context) {
        super.validate(definition, context);
        if (definition.params() != null && definition.params().has(LEGACY_PARAMS_FIELD)) {
            context.error(
                    BuildDiagnostic.Code.CONTENT_INVALID,
                    "/params/" + LEGACY_PARAMS_FIELD,
                    "Block '" + definition.id()
                            + "' uses removed key params.shapes; use params.shape_bindings",
                    "Rename params.shapes to params.shape_bindings."
            );
        }
    }

    @Override
    public void apply(BlockDef definition, BuildContext baseContext) {
        if (definition.params() != null && definition.params().has(LEGACY_PARAMS_FIELD)) {
            throw new IllegalArgumentException(
                    "Block '" + definition.id()
                            + "' uses removed key params.shapes; use params.shape_bindings"
            );
        }
        super.apply(definition, baseContext);
        JsonObject params = definition.params();
        if (params == null || !params.has(KuaYueShapeBindingSpec.PARAMS_FIELD)) return;

        RegBuildContext context = (RegBuildContext) baseContext;
        ShapeCatalog catalog = context.artifact(ShapeCatalog.class);
        if (catalog == null) {
            throw new IllegalArgumentException(
                    "Block '" + definition.id()
                            + "' declares params.shape_bindings but no shape catalog was prepared"
            );
        }
        Reg<?, ?> registration = context.getReg("blocks", definition.id());
        if (registration == null) {
            throw new IllegalArgumentException(
                    "Block '" + definition.id() + "' did not produce a registration"
            );
        }
        KuaYueShapeBindingSpec bindings =
                KuaYueShapeBindingSpec.parse(
                        params.get(KuaYueShapeBindingSpec.PARAMS_FIELD),
                        catalog
                );
        registration.withProperty(
                KuaYueShapeBindingSpec.class,
                ignored -> bindings
        );
    }
}
