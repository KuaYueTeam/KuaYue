package cn.kycraft.kuayue.content.techtree;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads every {@code data/<namespace>/tech_tree/*.json} file and keeps the linked graph.
 *
 * <p>Reload behaviour mirrors the reference implementation: a full rebuild on each resource reload, so
 * datapack overrides behave exactly like the original.</p>
 */
public final class TechTreeManager implements ResourceManagerReloadListener {

    public static final String DIRECTORY = "tech_tree";
    public static final String FORMAT_VERSION = "1.0.0";

    private static final TechTreeManager INSTANCE = new TechTreeManager();

    private volatile Map<String, TechTreeGroup> groups = Map.of();
    private volatile Map<NodeLocation, TechTreeNode> nodes = Map.of();

    private TechTreeManager() {
    }

    public static TechTreeManager get() {
        return INSTANCE;
    }

    public Map<ResourceLocation, TechTreeGroup> groups() {
        Map<ResourceLocation, TechTreeGroup> out = new LinkedHashMap<>();
        groups.values().forEach(group -> out.put(group.id, group));
        return Collections.unmodifiableMap(out);
    }

    public Map<NodeLocation, TechTreeNode> nodes() {
        return nodes;
    }

    public @Nullable TechTreeGroup getGroup(ResourceLocation id) {
        return groups.get(id.toString());
    }

    public @Nullable TechTreeNode getNode(NodeLocation location) {
        return nodes.get(location);
    }

    /** Rebuilds the graph from the resource manager. Safe to call on every datapack reload. */
    public void reload(ResourceManager manager) {
        Map<String, GroupData> parsed = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> resources = manager.listResources(DIRECTORY,
                rl -> rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation file = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) continue;
                JsonObject json = element.getAsJsonObject();
                String version = TechTreeJson.version(json);
                if (!FORMAT_VERSION.equals(version)) {
                    TechTreeSystem.logWarn("Skipping {}: expected version {}, found {}",
                            file, FORMAT_VERSION, version);
                    continue;
                }
                GroupData data = new GroupData(file.getNamespace(), json);
                parsed.put(data.id.toString(), data);
            } catch (Exception e) {
                TechTreeSystem.logError("Failed to read tech tree file " + file, e);
            }
        }

        Map<String, TechTreeGroup> builtGroups = new LinkedHashMap<>();
        Map<NodeLocation, TechTreeNode> builtNodes = new LinkedHashMap<>();
        parsed.values().forEach(data -> {
            TechTreeGroup group = new TechTreeGroup(data);
            data.nodes.values().forEach(nodeData -> group.addNode(new TechTreeNode(group, nodeData)));
            builtGroups.put(data.id.toString(), group);
        });

        this.groups = builtGroups;
        this.nodes = builtNodes;
        builtGroups.values().forEach(group -> group.nodes().forEach((loc, node) -> builtNodes.put(loc, node)));

        builtGroups.values().forEach(TechTreeGroup::link);
        builtNodes.values().forEach(TechTreeNode::bind);

        TechTreeSystem.logInfo("Loaded {} tech tree groups, {} nodes", builtGroups.size(), builtNodes.size());
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        reload(resourceManager);
    }
}
