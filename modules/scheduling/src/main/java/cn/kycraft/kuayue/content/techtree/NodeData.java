package cn.kycraft.kuayue.content.techtree;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parsed definition of a single node. Item ids stay strings until the registry is available. */
public final class NodeData {

    public final GroupData group;
    public final String identifier;
    public final String nameKey;
    public final String descriptionKey;
    public final String unlockDescriptionKey;
    public final NodeType type;
    public final String logo;
    public final int exp;
    public final int level;
    public final boolean hidden;
    public final HideRules hide;
    public final Map<String, Integer> consume = new LinkedHashMap<>();
    public final Map<String, Integer> blueprints = new LinkedHashMap<>();
    public final Map<String, Integer> rewards = new LinkedHashMap<>();
    public final List<NodeLocation> nextNodes = new ArrayList<>();
    public final List<NodeLocation> prevNodes = new ArrayList<>();
    public final List<ResourceLocation> nextGroups = new ArrayList<>();

    NodeData(GroupData group, String identifier, JsonObject json) {
        this.group = group;
        this.identifier = identifier;
        this.nameKey = TechTreeJson.str(json, "name", identifier);
        this.descriptionKey = TechTreeJson.str(json, "description", "");
        this.unlockDescriptionKey = TechTreeJson.str(json, "on_unlock_description", "");
        this.type = NodeType.byName(TechTreeJson.str(json, "type", "plain"));
        this.logo = TechTreeJson.str(json, "logo", "minecraft:barrier");
        this.exp = TechTreeJson.integer(json, "exp", 0);
        this.level = TechTreeJson.integer(json, "level", 0);
        this.hidden = json.has("hide") && json.get("hide").isJsonObject();
        this.hide = hidden ? HideRules.parse(TechTreeJson.object(json, "hide"), group) : HideRules.NONE;

        consume.putAll(TechTreeJson.itemCounts(json, "item_consume"));
        blueprints.putAll(TechTreeJson.itemCounts(json, "blueprint"));

        // The reference format declares rewards in the "item" block and its effect in "on_unlock".
        rewards.putAll(TechTreeJson.itemCounts(json, "item"));
        rewards.putAll(TechTreeJson.itemCounts(TechTreeJson.object(json, "on_unlock"), "item"));

        for (String next : TechTreeJson.strings(json, "next_nodes")) {
            nextNodes.add(NodeLocation.parse(group.namespace, group.identifier, next));
        }
        for (String next : TechTreeJson.strings(json, "next_groups")) {
            nextGroups.add(ResourceLocation.parse(next));
        }
    }

    public NodeLocation location() {
        return group.location(identifier);
    }
}
