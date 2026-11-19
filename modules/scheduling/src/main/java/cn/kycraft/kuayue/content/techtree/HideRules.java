package cn.kycraft.kuayue.content.techtree;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Visibility gate of a node or group ({@code "hide": {"condition": {...}}}).
 *
 * <p>A view is revealed only when every declared requirement is met: the namespaces must be loaded,
 * the advancements must be completed and the listed nodes must be unlocked.</p>
 */
public final class HideRules {

    public static final HideRules NONE = new HideRules(List.of(), List.of(), List.of());

    public final List<String> namespaces;
    public final List<ResourceLocation> advancements;
    public final List<NodeLocation> nodes;

    private HideRules(List<String> namespaces, List<ResourceLocation> advancements, List<NodeLocation> nodes) {
        this.namespaces = List.copyOf(namespaces);
        this.advancements = List.copyOf(advancements);
        this.nodes = List.copyOf(nodes);
    }

    public static HideRules parse(JsonObject json, GroupData group) {
        if (json == null) return NONE;
        JsonObject condition = TechTreeJson.object(json, "condition");
        List<String> namespaces = TechTreeJson.strings(condition, "contains_namespaces");
        List<ResourceLocation> advancements = new ArrayList<>();
        TechTreeJson.strings(condition, "advancements")
                .forEach(raw -> advancements.add(ResourceLocation.parse(raw)));
        List<NodeLocation> nodes = new ArrayList<>();
        TechTreeJson.strings(condition, "nodes").forEach(raw ->
                nodes.add(NodeLocation.parse(group.namespace, group.identifier, raw)));
        if (namespaces.isEmpty() && advancements.isEmpty() && nodes.isEmpty()) return NONE;
        return new HideRules(namespaces, advancements, nodes);
    }

    public boolean isNone() {
        return namespaces.isEmpty() && advancements.isEmpty() && nodes.isEmpty();
    }
}
