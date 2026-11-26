package cn.kycraft.kuayue.content.tech_tree;

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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads {@code data/<namespace>/blueprint_cover/*.json}.
 *
 * <p>Client-side only: covers are pure presentation, so they are read from the client resource manager and
 * rebuilt on every resource reload, like the tech tree itself. 1.0's {@code BlueprintCoverManager} is the
 * same shape; this one also keeps the {@code is_default} cover separate so the board can show it before a
 * group has been picked.</p>
 */
final class BlueprintCoverManager implements ResourceManagerReloadListener {

    static final String DIRECTORY = "blueprint_cover";

    private static final BlueprintCoverManager INSTANCE = new BlueprintCoverManager();

    private volatile Map<ResourceLocation, BlueprintCover> covers = Map.of();
    private volatile @Nullable ResourceLocation defaultCover;

    private BlueprintCoverManager() {
    }

    static BlueprintCoverManager get() {
        return INSTANCE;
    }

    @Nullable
    BlueprintCover get(@Nullable ResourceLocation id) {
        return id == null ? null : covers.get(id);
    }

    /** The cover marked {@code is_default}, else the first one loaded, else null when the pack has none. */
    @Nullable
    BlueprintCover defaultCover() {
        BlueprintCover cover = defaultCoverId == null ? null : covers.get(defaultCoverId);
        if (cover != null) return cover;
        return covers.values().stream().findFirst().orElse(null);
    }

    private volatile @Nullable ResourceLocation defaultCoverId;

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager manager) {
        Map<ResourceLocation, BlueprintCover> parsed = new LinkedHashMap<>();
        ResourceLocation foundDefault = null;

        Map<ResourceLocation, Resource> resources =
                manager.listResources(DIRECTORY, rl -> rl.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) continue;
                JsonObject json = element.getAsJsonObject();
                BlueprintCover cover = BlueprintCover.parse(json);
                parsed.put(cover.id(), cover);
                if (cover.isDefault()) foundDefault = cover.id();
            } catch (Exception error) {
                // A broken cover must not take the board down with it: it is decoration, not gameplay.
                TechTreeSystem.logWarn("Failed to read blueprint cover " + entry.getKey() + ": "
                        + error.getMessage());
            }
        }

        this.covers = parsed;
        this.defaultCoverId = foundDefault;
        TechTreeSystem.logInfo("Loaded {} blueprint covers from {} (default={})",
                parsed.size(), DIRECTORY, foundDefault);
        if (parsed.isEmpty()) {
            TechTreeSystem.logWarn("No blueprint covers under data/*/{} — the board cover page stays empty",
                    DIRECTORY);
        }
    }
}
