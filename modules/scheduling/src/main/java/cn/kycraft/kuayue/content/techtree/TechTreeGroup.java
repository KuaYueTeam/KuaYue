package cn.kycraft.kuayue.content.techtree;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A runtime tech tree group: its nodes, its root and the previously published links into it. */
public final class TechTreeGroup {

    public final GroupData data;
    public final ResourceLocation id;
    public final String nameKey;
    public final String descriptionKey;
    public final ItemStack icon;
    public final @Nullable ResourceLocation cover;
    public final boolean initialVisibility;

    private final Map<NodeLocation, TechTreeNode> nodes = new LinkedHashMap<>();
    private final List<NodeLocation> incoming = new ArrayList<>();
    private TechTreeNode root;

    TechTreeGroup(GroupData data) {
        this.data = data;
        this.id = data.id;
        this.nameKey = data.nameKey;
        this.descriptionKey = data.descriptionKey;
        this.icon = ItemStacks.resolve(data.icon);
        this.cover = data.cover;
        this.initialVisibility = data.initialVisibility;
    }

    void addNode(TechTreeNode node) {
        nodes.put(node.location, node);
    }

    void link() {
        root = nodes.get(data.root());
        for (TechTreeNode node : nodes.values()) {
            for (NodeLocation next : node.data.nextNodes) {
                TechTreeNode target = TechTreeManager.get().getNode(next);
                if (target != null) {
                    node.next.add(target);
                    target.prev.add(node);
                    target.data.prevNodes.add(node.location);
                }
            }
            for (ResourceLocation nextGroup : node.data.nextGroups) {
                TechTreeGroup group = TechTreeManager.get().getGroup(nextGroup);
                if (group != null) {
                    node.nextGroups.add(group);
                }
            }
        }
    }

    void addIncoming(NodeLocation from) {
        incoming.add(from);
    }

    public Map<NodeLocation, TechTreeNode> nodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public List<NodeLocation> incoming() {
        return Collections.unmodifiableList(incoming);
    }

    public @Nullable TechTreeNode root() {
        return root;
    }

    public String namespace() {
        return data.namespace;
    }

    /** Breadth-first layout: column = distance from the root, row = order inside that column. */
    public Map<NodeLocation, int[]> layout() {
        Map<NodeLocation, int[]> positions = new LinkedHashMap<>();
        if (root == null) {
            return positions;
        }
        Map<Long, Integer> columnRows = new LinkedHashMap<>();
        List<TechTreeNode> queue = new ArrayList<>();
        java.util.Set<NodeLocation> seen = new java.util.HashSet<>();
        queue.add(root);
        seen.add(root.location);
        for (int head = 0; head < queue.size(); head++) {
            TechTreeNode node = queue.get(head);
            int column = node.column();
            if (column < 0) {
                column = 0;
            }
            int row = columnRows.merge((long) column, 1, Integer::sum) - 1;
            positions.put(node.location, new int[]{column, row});
            for (TechTreeNode next : node.next) {
                if (seen.add(next.location)) {
                    queue.add(next);
                }
            }
        }
        return positions;
    }
}
