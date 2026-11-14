package cn.kycraft.kuayue.content.techtree;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pre-check of a node unlock, answering {@link CheckResult}.
 *
 * <p>Same rules as {@link ServerUnlock} but read-only: it never mutates the player, so the client can
 * call it on every node selection to paint the detail pane and the main progress arrow.</p>
 */
public final class ServerCheck {

    private ServerCheck() {
    }

    public static CheckResult checkNode(ServerPlayer player, String location) {
        TechTreeNode node;
        try {
            node = TechTreeManager.get().getNode(NodeLocation.parseFull(location));
        } catch (IllegalArgumentException e) {
            return CheckResult.empty();
        }
        if (node == null) return CheckResult.empty();

        UnlockState state = PlayerProgress.of(player);
        float progress = groupProgress(state, node.group);

        if (state.unlocked.contains(node.location)) {
            return new CheckResult(true, true, true, true, List.of(), List.of(),
                    new ArrayList<>(node.rewards().keySet()), progress);
        }

        boolean seen = !node.hidden() || Visibility.canSee(player, state, node.data.hide);
        boolean groupUnlocked = state.unlockedGroups.contains(node.group.id)
                || node.group.initialVisibility
                || ServerUnlock.canPublish(player, node.group);

        List<NodeLocation> missingNodes = new ArrayList<>();
        for (TechTreeNode parent : node.prev) {
            if (!state.unlocked.contains(parent.location)) missingNodes.add(parent.location);
        }

        boolean enoughLevel = player.experienceLevel >= node.level() && player.totalExperience >= node.exp();

        List<ItemStack> missingItems = new ArrayList<>();
        for (Map.Entry<ItemStack, Integer> entry : node.consume().entrySet()) {
            if (count(player, entry.getKey()) < entry.getValue()) missingItems.add(entry.getKey());
        }

        boolean unlockable = seen && groupUnlocked && enoughLevel && missingNodes.isEmpty() && missingItems.isEmpty();
        List<ItemStack> rewards = unlockable
                ? new ArrayList<>(node.rewards().keySet())
                : List.of();
        return new CheckResult(unlockable, seen, groupUnlocked, enoughLevel,
                missingItems, missingNodes, rewards, progress);
    }

    /** Unlocked fraction of one group, used for the main progress arrow. */
    public static float groupProgress(UnlockState state, TechTreeGroup group) {
        int total = 0;
        int done = 0;
        for (NodeLocation location : group.nodes().keySet()) {
            total++;
            if (state.unlocked.contains(location)) done++;
        }
        return total == 0 ? 0f : (float) done / (float) total;
    }

    public static float groupProgress(ServerPlayer player, ResourceLocation groupId) {
        TechTreeGroup group = TechTreeManager.get().getGroup(groupId);
        if (group == null) return 0f;
        return groupProgress(PlayerProgress.of(player), group);
    }

    private static int count(ServerPlayer player, ItemStack sample) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, sample)) total += stack.getCount();
        }
        return total;
    }
}
