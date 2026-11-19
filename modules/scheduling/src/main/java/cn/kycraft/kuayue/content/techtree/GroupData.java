package cn.kycraft.kuayue.content.techtree;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Parsed but not yet linked definition of one tech tree group (one JSON file). */
public final class GroupData {

    public final String namespace;
    public final String identifier;
    public final ResourceLocation id;
    public final String nameKey;
    public final String descriptionKey;
    public final String icon;
    public final ResourceLocation cover;
    public final String rootNode;
    public final boolean initialVisibility;
    public final int requiredPercentage;
    public final HideRules hide;
    public final Map<String, NodeData> nodes = new LinkedHashMap<>();

    GroupData(String namespace, JsonObject json) {
        this.namespace = namespace;
        this.id = TechTreeJson.id(json);
        this.identifier = id.getPath();
        this.nameKey = TechTreeJson.str(json, "name", "");
        this.descriptionKey = TechTreeJson.str(json, "description", "");
        this.icon = TechTreeJson.str(json, "icon", "minecraft:barrier");
        String cover = TechTreeJson.str(json, "cover", "");
        this.cover = cover.isEmpty() ? null : ResourceLocation.parse(cover);
        this.rootNode = TechTreeJson.str(json, "root_node", "");
        this.initialVisibility = TechTreeJson.bool(json, "initial_visibility", false);

        int percentage = 100;
        JsonObject unlockCondition = TechTreeJson.object(json, "unlock_condition");
        if (unlockCondition.has("percentage")) {
            percentage = (int) Math.ceil(unlockCondition.get("percentage").getAsDouble());
        } else if (json.has("unlock_condition") && json.get("unlock_condition").isJsonPrimitive()) {
            percentage = (int) Math.ceil(json.get("unlock_condition").getAsDouble());
        }
        this.requiredPercentage = percentage;
        this.hide = json.has("hide") && json.get("hide").isJsonObject()
                ? HideRules.parse(TechTreeJson.object(json, "hide"), this)
                : HideRules.NONE;

        for (Map.Entry<String, com.google.gson.JsonElement> entry : TechTreeJson.object(json, "nodes").entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            NodeData node = new NodeData(this, entry.getKey(), entry.getValue().getAsJsonObject());
            nodes.put(node.identifier, node);
        }
    }

    public NodeLocation location(String nodeName) {
        return new NodeLocation(namespace, identifier, nodeName);
    }

    public NodeLocation root() {
        return location(rootNode);
    }
}
