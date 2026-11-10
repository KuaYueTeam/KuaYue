package cn.kycraft.kuayue.parts.bogeys.core;

import com.simibubi.create.content.trains.bogey.BogeySizes;
import lib.kasuga.create.proxy.BogeyModels;
import lib.kasuga.create.proxy.BogeyProxy;
import lib.kasuga.create.proxy.BogeyRoots;
import lib.kasuga.rendering.models.mc.proxies.ModelPart;
import lib.kasuga.rendering.models.mc.proxies.ProxyIdentity;
import lib.kasuga.rendering.models.mc.proxies.RigidPartVisualStage;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.AnimationRenderPort;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.LogicalStepPolicy;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.RuntimeController;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyCommandPort;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyComposition;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyControllerPort;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyInputPort;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyVisualPort;
import lib.kasuga.rendering.models.uml.math.Transform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/** Session-native proxy composition for a KuaYue bogey placed in the world. */
public final class KuaYueBogeyProxyComposition {

    private static final LogicalStepPolicy POLICY = LogicalStepPolicy.fixed(1.0 / 20.0, 5);

    private KuaYueBogeyProxyComposition() {
    }

    public static ProxyComposition<CommonBogeyBlockEntity, PlacedInput, Void, Void, PlacedFrame>
    create(CommonBogeyBlockEntity element) {
        Level level = element.getLevel();
        if (level == null) {
            throw new IllegalStateException("placed KuaYue bogey proxy requires an attached level");
        }
        BogeySizes.BogeySize size = BogeyProxy.sizeOf(element);
        BogeyModels.Placement placement = BogeyModels.resolve(element.getStyle(), size);
        if (placement == null) {
            throw new IllegalStateException(
                    "placed KuaYue bogey has no 2.0 model mapping: "
                            + element.getStyle().id + " / " + (size == null ? "<none>" : size.id()));
        }

        FrozenTickInput input = new FrozenTickInput(element.getVirtualAngle(1f));
        List<ModelPart> parts = KuaYueBogeyMotion.bind(
                size.id(),
                placement.parts(),
                input::wheelAngle,
                false
        );
        ResourceLocation instance = ProxyIdentity.ofBlock(level, element.getBlockPos()).key();
        RigidPartVisualStage stage = new RigidPartVisualStage(
                instance,
                parts,
                () -> null,
                new Transform(),
                new Vector3f(0f, -placement.yOffset(), 0f)
        );
        return ProxyComposition.of(
                POLICY,
                placedInput(element, placement.yOffset()),
                controller(stage, input),
                (ignored, output) -> {
                },
                ProxyCommandPort.none(),
                new PlacedVisual(stage),
                stage.targetBindings()
        );
    }

    private static ProxyInputPort<CommonBogeyBlockEntity, PlacedInput> placedInput(
            CommonBogeyBlockEntity element,
            float yOffset
    ) {
        return ignored -> new PlacedInput(
                BogeyRoots.of(element, yOffset),
                element.getVirtualAngle(1f)
        );
    }

    private static ProxyControllerPort<PlacedInput, Void> controller(
            RigidPartVisualStage stage,
            FrozenTickInput input
    ) {
        return new ProxyControllerPort<>() {
            @Override
            public void prepare(PlacedInput value) {
                input.accept(value);
                stage.prepareLogicalStep(value.root());
            }

            @Override
            public RuntimeController<Void> controller() {
                return RuntimeController.none();
            }

            @Override
            public Void output() {
                return null;
            }
        };
    }

    public record PlacedInput(Transform root, float wheelAngle) {
    }

    public record PlacedFrame() {
    }

    private static final class FrozenTickInput {

        private float wheelAngle;

        private FrozenTickInput(float wheelAngle) {
            this.wheelAngle = wheelAngle;
        }

        private void accept(PlacedInput input) {
            wheelAngle = input.wheelAngle();
        }

        private float wheelAngle() {
            return wheelAngle;
        }
    }

    private static final class PlacedVisual
            implements ProxyVisualPort<CommonBogeyBlockEntity, PlacedFrame> {

        private final RigidPartVisualStage stage;
        private boolean closed;

        private PlacedVisual(RigidPartVisualStage stage) {
            this.stage = stage;
        }

        @Override
        public boolean isReady(CommonBogeyBlockEntity element) {
            return !closed;
        }

        @Override
        public void bind(AnimationRenderPort logicalRenderPort) {
            stage.bind(logicalRenderPort);
        }

        @Override
        public void tick(CommonBogeyBlockEntity element) {
            Level level = element.getLevel();
            if (!closed && level != null && level.isClientSide()) {
                stage.maintainVisuals();
            }
        }

        @Override
        public void publishFrame(PlacedFrame frame) {
        }

        @Override
        public void suspend() {
            stage.suspend();
        }

        @Override
        public void resume() {
            stage.resume();
        }

        @Override
        public boolean rebind(CommonBogeyBlockEntity element) {
            return ready(element);
        }

        private boolean ready(CommonBogeyBlockEntity element) {
            Level level = element.getLevel();
            if (closed) {
                return false;
            }
            return level == null || !level.isClientSide() || stage.rebind();
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            stage.close();
        }
    }
}
