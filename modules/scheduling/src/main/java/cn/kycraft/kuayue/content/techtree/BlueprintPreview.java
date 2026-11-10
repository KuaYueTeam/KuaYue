package cn.kycraft.kuayue.content.techtree;

import lib.kasuga.rendering.models.mc.backend.GuiModelPreview;
import lib.kasuga.rendering.models.mc.scene.BlockModelSource;
import lib.kasuga.rendering.models.mc.scene.MinecraftModelSources;
import lib.kasuga.rendering.models.mc.scene.SchematicModelData;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Blueprint previews of the table, built from the {@code assets/kuayue/blueprints} files.
 *
 * <p>Only the files shipped in that directory are rendered: they are read as bundled resources (there is
 * no filesystem path for a jar entry), decoded through Kasuga's {@code SchematicModelData} and handed to
 * {@link GuiModelPreview}, which draws the model into its own framebuffer with its own camera.</p>
 *
 * <p>Nothing is placed in the world, so the preview is one draw call onto the desktop rectangle.</p>
 *
 * <p>This class only loads; the camera and input live in {@link CarModelScene}, which is what keeps the
 * preview independent of the tech tree and reusable from another host.</p>
 */
final class BlueprintPreview {

    /** Directory of the shipped blueprints, relative to the {@code kuayue} namespace. */
    private static final String DIRECTORY = "blueprints";

    /** Currently shipped blueprint, used until the table selects another one. */
    static final ResourceLocation DEFAULT = ResourceLocation.fromNamespaceAndPath("kuayue", "test_bogie");

    private static final Map<ResourceLocation, GuiModelPreview> OPEN = new HashMap<>();
    /** Decoded block sources, kept for the world-space scene renderer. */
    private static final Map<ResourceLocation, BlockModelSource> SOURCES = new HashMap<>();

    /** Decoded structures, so the world-space renderer can draw the same blocks without re-reading the file. */
    private static final Map<ResourceLocation, List<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo>> BLOCKS = new HashMap<>();

    private BlueprintPreview() {
    }

    /** Opens (or reuses) the preview of one blueprint; null when the resource is missing or unreadable. */
    @Nullable
    static GuiModelPreview open(ResourceLocation blueprint) {
        ResourceLocation id = blueprint == null ? DEFAULT : blueprint;
        ensureDecoded(id);
        GuiModelPreview preview = OPEN.get(id);
        if (preview != null && !preview.isClosed()) return preview;
        GuiModelPreview created = create(id);
        if (created == null) return null;
        OPEN.put(id, created);
        return created;
    }

    /**
     * Decodes a blueprint once and keeps its blocks and model source.
     *
     * <p>Separate from {@link #open} because the two consumers are independent: the GUI preview caches a
     * framebuffer, while the world scene needs the {@code BlockModelSource} itself. Folding both into
     * {@code open} meant a cached preview skipped decoding, leaving the source cache empty.</p>
     */
    private static void ensureDecoded(ResourceLocation blueprint) {
        if (SOURCES.containsKey(blueprint)) return;
        create(blueprint);
    }

    @Nullable
    private static GuiModelPreview create(ResourceLocation blueprint) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) return null;
        Optional<Resource> resource = minecraft.getResourceManager().getResource(blueprintFile(blueprint));
        if (resource.isEmpty()) {
            TechTreeSystem.LOGGER.warn("Blueprint {} is missing from {}", blueprint, DIRECTORY);
            return null;
        }
        // The shipped blueprint references train-part blocks. If the part module is absent (scheduling
        // used to run without it) the decoder rejects the whole file, so say which module is missing
        // instead of only logging a bare palette error.
        // Train-part modules contribute the blocks a KuaYue blueprint references. Resolving their beans is
        // best-effort: a blueprint made of vanilla blocks must still decode when the part module is not on
        // this classpath (the single-module dev run), so this only warms them up.
        TrainPartBlocks.ensureLoaded();
        try (InputStream stream = resource.get().open()) {
            CompoundTag root = NbtIo.readCompressed(stream,
                    NbtAccounter.create(SchematicModelData.MAX_NBT_BYTES));
            var decoded = SchematicModelData.structure(root, minecraft.level.holderLookup(Registries.BLOCK), 0);
            BlockModelSource source = MinecraftModelSources.blocks(minecraft.level, decoded);
            BLOCKS.put(blueprint, List.copyOf(decoded));
            SOURCES.put(blueprint, source);
            GuiModelPreview preview = new GuiModelPreview(source);
            TechTreeSystem.LOGGER.info("Blueprint {} decoded into {} block infos", blueprint, decoded.size());
            return preview;
        } catch (Exception error) {
            TechTreeSystem.LOGGER.warn("Blueprint {} could not be decoded", blueprint, error);
            return null;
        }
    }

    /**
     * Decoded block model source of a blueprint, or null when it was never decoded.
     *
     * <p>Handed to Kasuga's {@code ModelDisplayTarget}, which owns it afterwards; a second caller must not
     * close it.</p>
     */
    static BlockModelSource source(ResourceLocation blueprint) {
        ResourceLocation id = blueprint == null ? DEFAULT : blueprint;
        ensureDecoded(id);
        return SOURCES.get(id);
    }

    /**
     * Decoded blocks of a blueprint, or an empty list when it was never opened.
     *
     * <p>Used by {@link BlueprintBlockRenderer}: drawing the structure as real blocks needs the block states
     * and positions, which the preview pipeline keeps private.</p>
     */
    static List<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo>
    blocks(ResourceLocation blueprint) {
        return BLOCKS.getOrDefault(blueprint, List.of());
    }

    /** {@code kuayue:test_ss8} -> {@code kuayue:blueprints/test_ss8.nbt}. */
    static ResourceLocation blueprintFile(ResourceLocation blueprint) {
        return ResourceLocation.fromNamespaceAndPath(blueprint.getNamespace(),
                DIRECTORY + "/" + blueprint.getPath() + ".nbt");
    }

    /** Releases every open preview; a world change invalidates all of their GPU resources. */
    static void closeAll() {
        for (GuiModelPreview preview : OPEN.values()) {
            if (!preview.isClosed()) preview.close();
        }
        OPEN.clear();
        BLOCKS.clear();
        SOURCES.clear();
    }
}
