package cn.kycraft.kuayue.content.techtree;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Evaluates {@link HideRules} for one player.
 *
 * <p>Advancement progress is the only gate that can change while the board is closed, so the state is
 * rebuilt here and pushed through the normal snapshot when something becomes visible.</p>
 */
public final class Visibility {

    private Visibility() {
    }

    /** Plugin-provided namespaces; KuaYue's own tree namespace is always considered present. */
    private static final String OWN_NAMESPACE = "kuayue";

    public static boolean canSee(ServerPlayer player, UnlockState state, HideRules rules) {
        if (rules == null || rules.isNone()) return true;
        for (String namespace : rules.namespaces) {
            if (!OWN_NAMESPACE.equals(namespace) && !namespaceLoaded(player.server, namespace)) {
                return false;
            }
        }
        if (!rules.advancements.isEmpty()) {
            PlayerAdvancements progress = player.getAdvancements();
            for (ResourceLocation id : rules.advancements) {
                AdvancementHolder holder = player.server.getAdvancements().get(id);
                if (holder == null) return false;
                AdvancementProgress advancementProgress = progress.getOrStartProgress(holder);
                if (!advancementProgress.isDone()) return false;
            }
        }
        for (NodeLocation required : rules.nodes) {
            if (!state.unlocked.contains(required)) return false;
        }
        return true;
    }

    private static boolean namespaceLoaded(MinecraftServer server, String namespace) {
        return server.getResourceManager().getNamespaces().contains(namespace);
    }

    /**
     * Re-evaluates every gated view for the player and records what became visible.
     *
     * @return {@code true} when anything changed and the client should be refreshed
     */
    public static boolean refresh(ServerPlayer player, UnlockState state) {
        boolean changed = false;
        Map<NodeLocation, TechTreeNode> nodes = TechTreeManager.get().nodes();
        for (TechTreeNode node : nodes.values()) {
            if (!node.hidden() || state.visibleNodes.contains(node.location)) continue;
            if (canSee(player, state, node.data.hide)) {
                state.visibleNodes.add(node.location);
                state.visibleGroups.add(node.group.id);
                changed = true;
            }
        }
        for (TechTreeGroup group : TechTreeManager.get().groups().values()) {
            if (group.data.hide.isNone() || state.visibleGroups.contains(group.id)) continue;
            if (canSee(player, state, group.data.hide)) {
                state.visibleGroups.add(group.id);
                changed = true;
            }
        }
        return changed;
    }
}
