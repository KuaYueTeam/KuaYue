package cn.kycraft.kuayue.content.techtree;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Client mirror of the last snapshot the server sent; read by the world-space UI. */
public final class ClientTreeState {

    private static volatile TreeSnapshot snapshot = new TreeSnapshot();
    private static volatile ResourceLocation selectedGroup;

    private ClientTreeState() {
    }

    public static void apply(TreeSnapshot next) {
        snapshot = next;
        if (selectedGroup == null || !next.groups.containsKey(selectedGroup)) {
            selectedGroup = next.groups.keySet().stream().findFirst().orElse(null);
        }
    }

    public static TreeSnapshot snapshot() {
        return snapshot;
    }

    public static Map<NodeLocation, TreeSnapshot.NodeView> nodes() {
        return Collections.unmodifiableMap(snapshot.nodes);
    }

    public static Map<ResourceLocation, TreeSnapshot.GroupView> groups() {
        return Collections.unmodifiableMap(snapshot.groups);
    }

    public static UnlockState progress() {
        return snapshot.progress;
    }

    public static @Nullable ResourceLocation selectedGroup() {
        return selectedGroup;
    }

    public static void selectGroup(@Nullable ResourceLocation group) {
        selectedGroup = group;
    }

    public static boolean visible(NodeLocation location) {
        TreeSnapshot.NodeView node = snapshot.nodes.get(location);
        if (node == null) return false;
        if (snapshot.progress.unlocked.contains(location) || snapshot.progress.visibleNodes.contains(location)) {
            return true;
        }
        return !node.hidden();
    }

    public static boolean unlocked(NodeLocation location) {
        return snapshot.progress.unlocked.contains(location);
    }

    /** Last pre-check answer for the focused node; drives slot colours and the progress arrow. */
    private static volatile CheckResult lastCheck = CheckResult.empty();
    private static volatile NodeLocation checkedNode;

    public static CheckResult lastCheck() {
        return lastCheck;
    }

    public static NodeLocation checkedNode() {
        return checkedNode;
    }

    public static void applyCheck(NodeLocation node, CheckResult result) {
        checkedNode = node;
        lastCheck = result;
    }

    /** Progress shown on the main arrow: the checked node's group progress, else the selection. */
    public static float arrowProgress() {
        CheckResult result = lastCheck;
        if (result != null && checkedNode != null) {
            return result.groupProgress();
        }
        return groupProgress(selectedGroup);
    }

    /** Unlock progress of one group in {@code [0,1]}, used by the main progress arrow. */
    public static float groupProgress(ResourceLocation group) {
        if (group == null) return 0f;
        int total = 0;
        int done = 0;
        for (NodeLocation location : snapshot.layout.getOrDefault(group, Map.of()).keySet()) {
            total++;
            if (snapshot.progress.unlocked.contains(location)) done++;
        }
        return total == 0 ? 0f : (float) done / (float) total;
    }

    /** Nodes of one group with their (column, row) layout positions. */
    public static Map<NodeLocation, int[]> layout(ResourceLocation group) {
        return snapshot.layout.getOrDefault(group, new LinkedHashMap<>());
    }
}
