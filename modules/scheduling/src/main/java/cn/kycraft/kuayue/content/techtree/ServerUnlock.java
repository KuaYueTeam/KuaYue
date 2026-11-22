package cn.kycraft.kuayue.content.techtree;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Server-side unlock rules and state transitions.
 *
 * <p>Rules mirror the reference implementation: a node needs its predecessors unlocked, the owning
 * group published, the listed items in the inventory and enough experience; creative players bypass
 * everything except the graph order.</p>
 */
public final class ServerUnlock {

    private ServerUnlock() {
    }

    public static TreeSnapshot applyNode(ServerPlayer player, String location) {
        try {
            TechTreeNode node = TechTreeManager.get().getNode(NodeLocation.parseFull(location));
            if (node == null) {
                TechTreeSystem.logWarn("Unlock requested for unknown node {}", location);
            } else {
                UnlockResult result = unlockNode(player, node);
                if (!result.ok()) {
                    // The client only receives the snapshot, so a refused unlock is otherwise invisible: log
                    // the rule that rejected it together with what the player actually brought.
                    TechTreeSystem.logWarn("Unlock of {} refused: {} (items={} level={}/{}, prevUnlocked={}/{})",
                            node.location, result.errorKey,
                            hasItems(player, node.consume()), player.experienceLevel, node.level(),
                            player.totalExperience, node.exp(),
                            node.prev.stream().filter(n -> PlayerProgress.of(player).unlocked.contains(n.location)).count(),
                            node.prev.size());
                }
            }
        } catch (IllegalArgumentException ignored) {
            TechTreeSystem.logWarn("Unlock requested with a malformed node id {}", location);
        }
        return TreeSnapshot.build(player);
    }

    public static TreeSnapshot applyGroup(ServerPlayer player, String group) {
        UnlockState state = PlayerProgress.of(player);
        try {
            TechTreeGroup target = TechTreeManager.get().getGroup(ResourceLocation.parse(group));
            if (target == null) {
                TechTreeSystem.logWarn("Publish requested for unknown group {}", group);
            } else if (canPublish(player, target)) {
                publish(player, state, target);
                TechTreeSystem.logInfo("Group {} published for {}", target.id, player.getGameProfile().getName());
            } else {
                // Same reason as a refused node: the client only sees the snapshot, so say why.
                long done = target.incoming().stream().filter(state.unlocked::contains).count();
                TechTreeSystem.logWarn("Publish of {} refused: {}/{} incoming unlocked, needs {}%",
                        target.id, done, target.incoming().size(), target.data.requiredPercentage);
            }
        } catch (IllegalArgumentException ignored) {
            TechTreeSystem.logWarn("Publish requested with a malformed group id {}", group);
        }
        return TreeSnapshot.build(player);
    }

    public static boolean canPublish(ServerPlayer player, TechTreeGroup group) {
        if (group.initialVisibility || player.isCreative()) return true;
        UnlockState state = PlayerProgress.of(player);
        if (state.unlockedGroups.contains(group.id)) return true;
        if (group.incoming().isEmpty()) return true;
        long done = group.incoming().stream().filter(state.unlocked::contains).count();
        return done * 100 >= (long) group.incoming().size() * group.data.requiredPercentage;
    }

    public static void publish(ServerPlayer player, UnlockState state, TechTreeGroup group) {
        state.visibleGroups.add(group.id);
        if (!group.initialVisibility) {
            state.unlockedGroups.add(group.id);
        }
    }

    public static UnlockResult unlockNode(ServerPlayer player, TechTreeNode node) {
        UnlockState state = PlayerProgress.of(player);
        if (state.unlocked.contains(node.location)) {
            return UnlockResult.alreadyUnlocked();
        }
        TechTreeGroup group = node.group;
        boolean groupPublished = state.unlockedGroups.contains(group.id)
                || group.initialVisibility
                || canPublish(player, group);
        if (!groupPublished) {
            return UnlockResult.failed("tech_tree.error.group_locked");
        }
        for (TechTreeNode parent : node.prev) {
            if (!state.unlocked.contains(parent.location)) {
                return UnlockResult.failed("tech_tree.error.requires_nodes");
            }
        }
        if (node.hidden() && !Visibility.canSee(player, state, node.data.hide)) {
            return UnlockResult.failed("tech_tree.error.locked_by_advancement");
        }
        if (!player.isCreative()) {
            if (!hasItems(player, node.consume())) {
                return UnlockResult.failed("tech_tree.error.requires_items");
            }
            if (player.experienceLevel < node.level() || player.totalExperience < node.exp()) {
                return UnlockResult.failed("tech_tree.error.requires_experience");
            }
            consume(player, node.consume());
            player.giveExperiencePoints(-node.exp());
            state.unlockedGroups.add(group.id);
            state.visibleGroups.add(group.id);
        }
        state.unlocked.add(node.location);
        state.visibleNodes.add(node.location);
        grant(player, node.blueprints());
        grant(player, node.rewards());
        revealNext(state, node);
        Visibility.refresh(player, state);
        return UnlockResult.success();
    }

    /** Unlocking a node reveals its successors so they can be drawn on the board. */
    private static void revealNext(UnlockState state, TechTreeNode node) {
        node.next.forEach(next -> state.visibleNodes.add(next.location));
        node.nextGroups.forEach(group -> {
            state.visibleGroups.add(group.id);
            if (group.initialVisibility) {
                state.unlockedGroups.add(group.id);
            }
        });
    }

    private static boolean hasItems(ServerPlayer player, Map<ItemStack, Integer> required) {
        for (Map.Entry<ItemStack, Integer> entry : required.entrySet()) {
            if (count(player, entry.getKey()) < entry.getValue()) return false;
        }
        return true;
    }

    private static int count(ServerPlayer player, ItemStack sample) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, sample)) total += stack.getCount();
        }
        return total;
    }

    private static void consume(ServerPlayer player, Map<ItemStack, Integer> required) {
        required.forEach((sample, amount) -> {
            int remaining = amount;
            for (ItemStack stack : player.getInventory().items) {
                if (remaining <= 0) break;
                if (!ItemStack.isSameItemSameComponents(stack, sample)) continue;
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
            }
        });
        player.getInventory().setChanged();
    }

    private static void grant(ServerPlayer player, Map<ItemStack, Integer> rewards) {
        rewards.forEach((sample, amount) -> {
            ItemStack stack = sample.copyWithCount(amount);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        });
    }

    public record UnlockResult(boolean ok, String errorKey) {
        public static UnlockResult success() {
            return new UnlockResult(true, "");
        }

        public static UnlockResult alreadyUnlocked() {
            return new UnlockResult(true, "");
        }

        public static UnlockResult failed(String key) {
            return new UnlockResult(false, key);
        }
    }
}
