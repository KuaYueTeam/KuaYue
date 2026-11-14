package cn.kycraft.kuayue.content.techtree;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A runtime tech tree node. Connections are resolved once, after the whole tree is loaded. */
public final class TechTreeNode {

    public final NodeData data;
    public final TechTreeGroup group;
    public final NodeLocation location;
    public final List<TechTreeNode> prev = new ArrayList<>();
    public final List<TechTreeNode> next = new ArrayList<>();
    public final List<TechTreeGroup> nextGroups = new ArrayList<>();

    private final Map<ItemStack, Integer> consume = new LinkedHashMap<>();
    private final Map<ItemStack, Integer> blueprints = new LinkedHashMap<>();
    private final Map<ItemStack, Integer> rewards = new LinkedHashMap<>();
    public final ItemStack logo;

    TechTreeNode(TechTreeGroup group, NodeData data) {
        this.data = data;
        this.group = group;
        this.location = data.location();
        this.logo = ItemStacks.resolve(data.logo);
        ItemStacks.collect(data.consume, consume);
        ItemStacks.collect(data.blueprints, blueprints);
        ItemStacks.collect(data.rewards, rewards);
    }

    void bind() {
        for (ResourceLocation nextGroup : data.nextGroups) {
            TechTreeGroup target = TechTreeManager.get().getGroup(nextGroup);
            if (target != null) {
                target.addIncoming(location);
            }
        }
    }

    public NodeType type() {
        return data.type;
    }

    public boolean hidden() {
        return data.hidden;
    }

    public int exp() {
        return data.exp;
    }

    public int level() {
        return data.level;
    }

    public Map<ItemStack, Integer> consume() {
        return consume;
    }

    public Map<ItemStack, Integer> blueprints() {
        return blueprints;
    }

    public Map<ItemStack, Integer> rewards() {
        return rewards;
    }

    public @Nullable TechTreeNode firstPrev() {
        return prev.isEmpty() ? null : prev.get(0);
    }

    /** Column index used by the world-space layout: longest path from a root, capped at 64. */
    int column() {
        int depth = 0;
        TechTreeNode cursor = this;
        java.util.Set<TechTreeNode> seen = new java.util.HashSet<>();
        while (seen.add(cursor) && depth < 64) {
            TechTreeNode parent = cursor.firstPrev();
            if (parent == null) break;
            cursor = parent;
            depth++;
        }
        return depth;
    }
}
