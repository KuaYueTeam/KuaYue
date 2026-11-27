package cn.kycraft.kuayue.packs.shapes;

import com.google.gson.JsonObject;
import lib.kasuga.registration.data_driven.handler.BlockDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KuaYueBlockTypeHandlerTest {
    @Test
    void rejectsTheLegacyParamsShapesKeyWithoutReadingIt() {
        JsonObject params = new JsonObject();
        params.add("shapes", new JsonObject());
        BlockDef definition = new BlockDef(
                "example:legacy",
                "test",
                null,
                null,
                null,
                params
        );

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new KuaYueBlockTypeHandler().apply(definition, null)
        );
        assertTrue(failure.getMessage().contains("params.shapes"), failure.getMessage());
        assertTrue(
                failure.getMessage().contains("params.shape_bindings"),
                failure.getMessage()
        );
    }
}
