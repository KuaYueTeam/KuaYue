package cn.kycraft.kuayue.parts.locomotives.jy;

import lib.kasuga.rendering.models.mc.proxies.ProxyModelBlockEntity;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** FML checks the actual embedded parcel, rather than registering another set of test blocks. */
class JYMaintenancePresetTest {
    private static final String PARCEL = "/kuayue-packs/kuayue-preset-industrial/parcels/jy_maintenance/";

    @Test
    void embeddedParcelRegistersItsOriginalBlockIdsAndProxyHosts() {
        for (String part : List.of("head", "carport", "console", "floor", "floor_ladder", "outrigger",
                "endface", "door", "panel", "tail", "handrail")) {
            ResourceLocation id = id(part);
            var block = BuiltInRegistries.BLOCK.get(id);
            assertNotSame(Blocks.AIR, block, id.toString());
            assertNotSame(Items.AIR, BuiltInRegistries.ITEM.get(id), id.toString());
            assertTrue(block.defaultBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING), part);
            if (block instanceof EntityBlock entityBlock) {
                var entity = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                assertTrue(entity instanceof ProxyModelBlockEntity, part);
                assertEquals("jy_maintenance", ((ProxyModelBlockEntity) entity).model().getNamespace(), part);
            }
        }
        assertTrue(BuiltInRegistries.BLOCK.get(id("door")) instanceof JYMaintenanceDoorBlock);
        assertTrue(BuiltInRegistries.BLOCK.get(id("outrigger")) instanceof JYMaintenanceOutriggerBlock);
    }

    @Test
    void embeddedNativeRecipeDecodesWithTheCurrentCreateSerializer() throws Exception {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        // The accepted parcel's other recipes use the legacy blueprint system, which has not
        // migrated its serializer or crafting materials. This test covers the native Create recipe.
        String path = PARCEL + "data/jy_maintenance/recipe/jy_maintenance_floor_ladder.json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            assertDoesNotThrow(() -> Recipe.CODEC.parse(
                    registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow(), path);
        }
    }

    private static ResourceLocation id(String part) {
        return ResourceLocation.fromNamespaceAndPath("jy_maintenance", "jy_maintenance_" + part);
    }
}
