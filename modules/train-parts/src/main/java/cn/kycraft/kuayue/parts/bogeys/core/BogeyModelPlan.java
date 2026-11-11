package cn.kycraft.kuayue.parts.bogeys.core;

import lib.kasuga.rendering.models.mc.proxies.ModelPart;
import lib.kasuga.rendering.models.uml.dynamic.animation.AnimationClip;
import lib.kasuga.rendering.models.uml.dynamic.animation.FormulaTransformSampler;
import lib.kasuga.rendering.models.uml.dynamic.fsm.FsmAnimationClips;
import lib.kasuga.rendering.models.uml.dynamic.fsm.FsmRegistries;
import lib.kasuga.rendering.models.uml.dynamic.fsm.Id;
import lib.kasuga.rendering.models.uml.math.Transform;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Compiled, host-independent description of one bogey model group.
 */
public final class BogeyModelPlan {
    private final ResourceLocation id;
    private final float yOffset;
    private final List<Part> parts;

    /**
     * Creates an immutable model plan.
     *
     * @param id      stable model definition ID
     * @param yOffset vertical offset from the block centre
     * @param parts   ordered model parts
     */
    public BogeyModelPlan(ResourceLocation id, float yOffset, List<Part> parts) {
        this.id = Objects.requireNonNull(id, "id");
        if (!Float.isFinite(yOffset)) {
            throw new IllegalArgumentException("model y offset must be finite");
        }
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("model plan must contain at least one part");
        }
        this.yOffset = yOffset;
        this.parts = List.copyOf(parts);
    }

    /**
     * Returns the stable model definition ID.
     */
    public ResourceLocation id() {
        return id;
    }

    /**
     * Returns the vertical model offset used by the proxy root.
     */
    public float yOffset() {
        return yOffset;
    }

    /**
     * Returns the ordered compiled parts.
     */
    public List<Part> parts() {
        return parts;
    }

    /**
     * Creates static model parts for the shared placement registry.
     */
    public List<ModelPart> staticParts() {
        return parts.stream()
                .map(part -> new ModelPart(part.model(), null, part.local(), null))
                .toList();
    }

    /**
     * Binds per-host motion sources to a fresh list of model parts.
     *
     * @param wheelAngle  Create wheel angle source
     * @param frameDomain whether the host supplies frame-interpolated angles
     * @return parts ready for the model pipeline
     */
    public List<ModelPart> bind(Supplier<Float> wheelAngle, boolean frameDomain) {
        Objects.requireNonNull(wheelAngle, "wheelAngle");
        BindingContext context = new BindingContext();
        return parts.stream()
                .map(part -> part.motion() == null
                        ? new ModelPart(part.model(), null, part.local(), null)
                        : new ModelPart(
                                part.model(),
                                null,
                                part.local(),
                                part.motion().bind(part.id(), wheelAngle, frameDomain, context)
                        ))
                .toList();
    }

    /**
     * One ordered part in a compiled model plan.
     *
     * @param id     stable part ID used for diagnostics
     * @param model  model resource
     * @param local  fixed local transform
     * @param motion optional per-host motion
     */
    public record Part(
            String id,
            ResourceLocation model,
            Transform local,
            @Nullable Motion motion
    ) {
        /**
         * Creates a defensive part snapshot.
         */
        public Part {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("model part id must not be blank");
            }
            model = Objects.requireNonNull(model, "model");
            local = Objects.requireNonNull(local, "local").copy();
        }

        @Override
        public Transform local() {
            return local.copy();
        }
    }

    /**
     * Host-specific binding strategy for one compiled motion profile.
     */
    @FunctionalInterface
    public interface Motion {
        /**
         * Binds one motion to the current host.
         *
         * @param partId      stable model part ID
         * @param wheelAngle  Create wheel angle source
         * @param frameDomain whether the host supplies frame-interpolated angles
         * @param context     shared per-model binding context
         * @return model pipeline motion
         */
        ModelPart.Motion bind(
                String partId,
                Supplier<Float> wheelAngle,
                boolean frameDomain,
                BindingContext context
        );
    }

    /**
     * Shared state for one model binding pass.
     */
    public static final class BindingContext {
        private final Map<ResourceLocation, FormulaTransformSampler> formulaSamplers =
                new HashMap<>();

        /**
         * Resolves or creates the sampler for one animation clip.
         *
         * @param clipId animation clip ID
         * @return shared formula sampler for this model binding
         */
        public FormulaTransformSampler formulaSampler(ResourceLocation clipId) {
            Objects.requireNonNull(clipId, "clipId");
            return formulaSamplers.computeIfAbsent(clipId, id -> {
                Id fsmId = Id.fromNamespaceAndPath(id.getNamespace(), id.getPath());
                FsmAnimationClips.Entry entry = FsmRegistries.GLOBAL.clips().get(fsmId);
                if (entry == null || !(entry.data() instanceof AnimationClip clip)) {
                    throw new IllegalStateException(
                            "Missing formula animation clip " + id
                    );
                }
                return new FormulaTransformSampler(clip);
            });
        }
    }
}
