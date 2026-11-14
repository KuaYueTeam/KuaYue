package cn.kycraft.kuayue.parts.tracks.data;

import cn.kycraft.kuayue.parts.tracks.core.SimpleTrackBlock;
import com.google.gson.JsonObject;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import lib.kasuga.create.registration.track.TrackMaterialReg;
import lib.kasuga.create.registration.track.TrackReg;
import lib.kasuga.registration.data_driven.TypeHandler;
import lib.kasuga.registration.data_driven.context.BuildContext;
import lib.kasuga.registration.data_driven.context.JsonRegistryGroup;
import lib.kasuga.registration.data_driven.context.RegBuildContext;
import lib.kasuga.registration.data_driven.dedup.EffectiveId;
import lib.kasuga.registration.data_driven.json.DataJson;
import lib.kasuga.registration.data_driven.pipeline.DefinitionKey;
import lib.kasuga.registration.data_driven.pipeline.HandlerCapability;
import lib.kasuga.registration.data_driven.pipeline.RegistryClaim;
import lib.kasuga.registration.data_driven.property.JsonItemParser;
import lib.kasuga.registration.data_driven.property.JsonPropertyParser;
import lib.kasuga.registration.data_driven.validation.ValidationContext;
import lib.kasuga.registration.core.CreativeTabModifiers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Handles the KuaYue-specific {@code tracks} top-level data-driven field.
 *
 * <p>The handler parses one track definition, creates its Create {@code TrackMaterial}, and
 * registers a {@link TrackReg} containing the track block and item registration. Optional block and
 * item properties, including the creative tab, are inherited from the named registry group. The
 * default bogey is resolved lazily so a bogey registration created in the same startup pass can
 * still be referenced.</p>
 */
public final class TrackTypeHandler implements TypeHandler<TrackDefinition> {

    /** Top-level content file field consumed by this handler. */
    public static final String TYPE_NAME = "tracks";
    private final TrackDefinitionParser parser = new TrackDefinitionParser();

    /**
     * {@inheritDoc}
     *
     * @return {@value #TYPE_NAME}
     */
    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Track definitions run during the content phase after registry groups have been created.</p>
     *
     * @return {@link #PHASE_CONTENT}
     */
    @Override
    public int getPhase() {
        return PHASE_CONTENT;
    }

    @Override
    public Set<HandlerCapability> capabilities() {
        return Set.of(
                HandlerCapability.STRICT_PREFLIGHT,
                HandlerCapability.REGISTRY_CLAIMS
        );
    }

    @Override
    public void validate(TrackDefinition definition, ValidationContext context) {
        context.requireDefinition(
                new DefinitionKey("registry_groups", definition.registryGroup()),
                "/registry_group"
        );
        DefinitionKey defaultBogey = new DefinitionKey(
                "bogey_blocks",
                EffectiveId.of(context.hostModId(), definition.defaultBogey())
        );
        if (context.find(defaultBogey).isEmpty()) {
            context.warning(
                    lib.kasuga.registration.data_driven.pipeline.BuildDiagnostic.Code.MISSING_REFERENCE,
                    "/default_bogey",
                    "Referenced default bogey " + defaultBogey + " is unavailable",
                    "The track will fall back to Create's small bogey at runtime."
            );
        } else {
            context.requireDefinition(defaultBogey, "/default_bogey");
        }
        if (!"simple_track".equals(definition.type())) {
            context.error(
                    lib.kasuga.registration.data_driven.pipeline.BuildDiagnostic.Code.CONTENT_INVALID,
                    "/type",
                    "Unsupported track type: " + definition.type(),
                    "Use 'simple_track'."
            );
        }
    }

    @Override
    public List<RegistryClaim> claims(TrackDefinition definition, ValidationContext context) {
        ResourceLocation id = ResourceLocation.parse(
                EffectiveId.of(context.hostModId(), definition.id()));
        return List.of(
                context.claim("minecraft:block", id),
                context.claim("minecraft:item", id)
        );
    }

    /**
     * Parses and validates one track object.
     *
     * @param json raw track object
     * @return the validated track definition
     * @throws IllegalArgumentException when the schema is malformed
     */
    @Override
    public TrackDefinition parse(JsonObject json) {
        return parser.parse(json);
    }

    /**
     * Creates the Create track material, block, and item registrations for one definition.
     *
     * @param definition  validated track definition
     * @param baseContext data-driven registration context
     * @throws IllegalArgumentException when the registry group is missing, the build context is
     *                                  incompatible, or a sleeper block cannot be resolved
     */
    @Override
    public void apply(TrackDefinition definition, BuildContext baseContext) {
        if (!(baseContext instanceof RegBuildContext context)) {
            throw new IllegalArgumentException(TYPE_NAME + " requires the registration build context");
        }

        JsonRegistryGroup parent = context.getRegistryGroup(definition.registryGroup());
        if (parent == null) {
            throw new IllegalArgumentException("Unknown registry_group '" + definition.registryGroup()
                    + "' for track " + definition.id());
        }

        ResourceLocation trackId = ResourceLocation.parse(definition.id());
        TrackMaterialDefinition materialDefinition = definition.material();
        ResourceLocation materialId = ResourceLocation.parse(materialDefinition.id());
        ResourceLocation defaultBogey = ResourceLocation.parse(definition.defaultBogey());

        TrackMaterialReg material = new TrackMaterialReg(materialId.getPath())
                .withProperty(ResourceLocation.class, ignored -> materialId)
                .lang(materialDefinition.name())
                .particle(ResourceLocation.parse(materialDefinition.particle()))
                .customModels(
                        ResourceLocation.parse(materialDefinition.models().tie()),
                        ResourceLocation.parse(materialDefinition.models().left()),
                        ResourceLocation.parse(materialDefinition.models().right())
                )
                .simpleTrackModelOffset(materialDefinition.halfDistance())
                .sleeper(resolveBlocks(materialDefinition.sleepers(), definition.id()).toArray(ItemLike[]::new));

        Supplier<AbstractBogeyBlock<?>> bogeySupplier =
                () -> resolveBogey(defaultBogey);
        TrackReg<SimpleTrackBlock> track = new TrackReg<>(
                trackId.getPath(),
                material,
                (properties, resolvedMaterial) -> new SimpleTrackBlock(
                        properties,
                        resolvedMaterial,
                        bogeySupplier
                )
        ).withProperty(ResourceLocation.class, ignored -> trackId);

        applyBlockProperties(definition.properties(), track);
        applyItemProperties(definition.itemProperties(), track);
        String tabId = DataJson.creativeTab(definition.itemProperties());
        ResourceLocation tab = tabId == null ? null : ResourceLocation.parse(tabId);
        if (tab == null) {
            tab = context.getRegistryGroupCreativeTab(definition.registryGroup());
        }
        if (tab != null) {
            ResourceLocation resolvedTab = tab;
            track.configure(CreativeTabModifiers.set(() -> resolvedTab));
        }

        material.block(track);
        material.setParent(parent);
        track.setParent(parent);
        context.putReg(TYPE_NAME, definition.id(), track);
    }

    /**
     * Normalizes the definition ID using Kasuga's registration namespace and deduplication rules.
     *
     * @param modId      owning mod ID
     * @param definition track definition
     * @return normalized registration identity
     */
    @Override
    public String resolveIdentity(String modId, TrackDefinition definition) {
        return EffectiveId.of(modId, definition.id());
    }

    /**
     * Applies optional block-property overrides to the track registration.
     *
     * @param properties JSON property object
     * @param track      target track registration
     */
    private static void applyBlockProperties(
            JsonObject properties,
            TrackReg<SimpleTrackBlock> track
    ) {
        for (Function<BlockBehaviour.Properties, BlockBehaviour.Properties> modifier
                : JsonPropertyParser.getInstance().parseBlockProperties(properties)) {
            track.withProperty(BlockBehaviour.Properties.class, modifier);
        }
    }

    /**
     * Applies optional item-property overrides to the track registration.
     *
     * @param properties JSON property object
     * @param track      target track registration
     */
    private static void applyItemProperties(
            JsonObject properties,
            TrackReg<SimpleTrackBlock> track
    ) {
        for (Function<Item.Properties, Item.Properties> modifier
                : JsonItemParser.INSTANCE.parseItemProperties(properties)) {
            track.withProperty(Item.Properties.class, modifier);
        }
    }

    /**
     * Resolves sleeper block IDs to live registry entries.
     *
     * @param ids     sleeper resource locations
     * @param trackId owning track ID used in diagnostics
     * @return resolved sleeper blocks
     * @throws IllegalArgumentException when an ID does not resolve to a non-air block
     */
    private static List<ItemLike> resolveBlocks(List<String> ids, String trackId) {
        return ids.stream().map(rawId -> {
            ResourceLocation id = ResourceLocation.parse(rawId);
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block == null || block == Blocks.AIR) {
                throw new IllegalArgumentException("Unknown sleeper block '" + rawId
                        + "' for track " + trackId);
            }
            return (ItemLike) block;
        }).toList();
    }

    /**
     * Resolves a bogey block lazily, returning {@code null} when the ID is not a bogey.
     *
     * @param id bogey block resource location
     * @return resolved bogey block, or {@code null} when the ID does not identify a bogey
     */
    private static AbstractBogeyBlock<?> resolveBogey(ResourceLocation id) {
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block instanceof AbstractBogeyBlock<?> bogey) {
            return bogey;
        }
        return null;
    }
}
