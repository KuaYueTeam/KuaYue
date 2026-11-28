package cn.kycraft.kuayue.parts.locomotives.jy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Expected passenger boarding doors, independent of the production registration policy. */
final class PassengerDoorTestData {
    static final Set<String> IDS = Set.of(
            "22_door", "door_25b", "door_25g", "door_25k", "door_25t", "door_25z",
            "door_sliding_25g", "door_sliding_25k", "door_sliding_25t",
            "door_marshalled_cr200j", "door_marshalled_cr200j_2", "door_m1");

    private PassengerDoorTestData() {
    }

    static Block block(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("kuayue", path);
        assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "The actual preset must register " + id);
        return BuiltInRegistries.BLOCK.get(id);
    }

    static Map<String, Block> doors() {
        Map<String, Block> result = new LinkedHashMap<>();
        IDS.stream().sorted().forEach(id -> result.put(id, block(id)));
        return result;
    }
}
