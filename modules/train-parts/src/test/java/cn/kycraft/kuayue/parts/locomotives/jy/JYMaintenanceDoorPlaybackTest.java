package cn.kycraft.kuayue.parts.locomotives.jy;

import lib.kasuga.rendering.models.mc.proxies.ModelClipPoseEvaluator;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelInput;
import lib.kasuga.rendering.models.mc.typo.bbmodel.BbModelAnimation;
import lib.kasuga.rendering.models.mc.typo.bbmodel.BbModelSampler;
import lib.kasuga.rendering.models.uml.dynamic.ModelInstance;
import lib.kasuga.rendering.models.uml.dynamic.animation.AnimationSampler;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.AnimationRenderPort;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.AnimationTarget;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.AnimationTargetRef;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.FrameInputSnapshot;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.LogicalStepPolicy;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.RenderFrameToken;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.RenderSampleContext;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.TargetBinding;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.TargetPublication;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.TargetPublicationFactory;
import lib.kasuga.rendering.models.uml.dynamic.fsm.Pose;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyAttachToken;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyCommandPort;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyComposition;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyDefinition;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyKey;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyRuntime;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxySession;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyVisualPort;
import lib.kasuga.rendering.models.uml.math.Transform;
import lib.kasuga.rendering.models.uml.structure.Model;
import lib.kasuga.rendering.models.uml.structure.basic.Mesh;
import lib.kasuga.rendering.models.uml.structure.basic.Vertex;
import lib.kasuga.rendering.models.uml.structure.material.Material;
import lib.kasuga.rendering.models.uml.structure.material.MaterialSet;
import lib.kasuga.rendering.models.uml.structure.material.MaterialSetInstance;
import lib.kasuga.rendering.models.uml.structure.material.Texture;
import lib.kasuga.rendering.models.uml.structure.skeleton.Anchor;
import lib.kasuga.rendering.models.uml.structure.skeleton.Bone;
import lib.kasuga.rendering.models.uml.structure.skeleton.Skeleton;
import lib.kasuga.rendering.models.uml.util.MeshMode;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JYMaintenanceDoorPlaybackTest {
    @Test
    void doorAndEndfaceReachBothEndpointsAndHoldAfterTheLastAuthoredKeyframe() {
        for (String part : List.of("door", "endface")) {
            try (var fixture = new PlaybackFixture(part, false, false)) {
                fixture.tick();
                assertEquals(0, fixture.sample.get().time(), 1e-6);
                fixture.request(true);
                fixture.tick();
                fixture.ticks(5);
                assertEquals(0.25, fixture.sample.get().time(), 1e-6,
                        "The authored motion ends before the clip's declared 0.5-second tail");
                assertEquals(-1, fixture.sample.get().direction().z, 1e-5);
                fixture.ticks(12);
                assertEquals(0.25, fixture.sample.get().time(), 1e-6);
                fixture.request(false);
                fixture.tick();
                fixture.ticks(5);
                assertEquals(0, fixture.sample.get().time(), 1e-6);
                assertEquals(1, fixture.sample.get().direction().x, 1e-5);
                fixture.ticks(12);
                assertEquals(0, fixture.sample.get().time(), 1e-6);
            }
        }
    }

    @Test
    void midwayReversalsKeepTheCurrentClipTimeAndTheSameSession() {
        try (var fixture = new PlaybackFixture("door", false, false)) {
            var session = fixture.session;
            fixture.tick();
            fixture.request(true);
            fixture.tick();
            fixture.ticks(2);
            double before = fixture.sample.get().time();
            assertEquals(0.1, before, 1e-6);
            fixture.request(false);
            fixture.tick();
            double reversed = fixture.sample.get().time();
            assertTrue(Math.abs(reversed - before) <= 0.050001);
            assertTrue(reversed > 0 && reversed < 0.25, "Reversal must not restart at either endpoint");
            fixture.tick();
            assertTrue(fixture.sample.get().time() < reversed);
            before = fixture.sample.get().time();
            fixture.request(true);
            fixture.tick();
            double secondReverse = fixture.sample.get().time();
            assertTrue(Math.abs(secondReverse - before) <= 0.050001);
            fixture.tick();
            assertTrue(fixture.sample.get().time() > secondReverse);
            fixture.ticks(6);
            assertEquals(0.25, fixture.sample.get().time(), 1e-6);
            assertSame(session, fixture.session);
        }
    }

    @Test
    void renderFramesAndDuplicateGameTicksDoNotAdvanceOrResampleTheDoorClip() {
        try (var fixture = new PlaybackFixture("endface", false, false)) {
            fixture.tick();
            fixture.request(true);
            fixture.tick();
            fixture.ticks(2);
            long revision = fixture.session.revision();
            int samples = fixture.poseSamples.get();
            double time = fixture.sample.get().time();
            for (int frame = 0; frame < 100; frame++) {
                assertTrue(fixture.session.publishFrame(null));
            }
            assertFalse(fixture.session.tick(fixture.gameTime, 0.05).advanced());
            assertEquals(revision, fixture.session.revision());
            assertEquals(samples, fixture.poseSamples.get());
            assertEquals(time, fixture.sample.get().time(), 0);
            assertEquals(100, fixture.renderSamples.get());
        }
    }

    @Test
    void anInitiallyOpenDoorUsesItsOpenPoseWhenTheModelBecomesAvailable() {
        try (var fixture = new PlaybackFixture("door", true, true)) {
            fixture.tick();
            assertNull(fixture.sample.get());
            fixture.model.set(fixture.preparedModel);
            fixture.tick();
            assertEquals(0.25, fixture.sample.get().time(), 1e-6);
            assertEquals(-1, fixture.sample.get().direction().z, 1e-5);
        }
    }

    record Sample(double time, Vector3f direction) {}

    static final class PlaybackFixture implements AutoCloseable {
        final AtomicInteger poseSamples = new AtomicInteger();
        final AtomicInteger renderSamples = new AtomicInteger();
        final AtomicReference<Sample> sample = new AtomicReference<>();
        final AtomicReference<BlockState> state;
        final ModelInstance preparedModel;
        final BooleanProperty property;
        final AtomicReference<ModelInstance> model = new AtomicReference<>();
        final ProxySession<AtomicReference<BlockState>, ProxyModelInput, Void, Void, Void> session;
        long gameTime;

        PlaybackFixture(String part, boolean open, boolean delayedModel) {
            this((part.equals("door")
                    ? JYContraptionDoorFixtures.sideDoor : JYContraptionDoorFixtures.endface)
                    .defaultBlockState().setValue(BlockStateProperties.OPEN, open),
                    BlockStateProperties.OPEN, "door_open", part, delayedModel);
        }

        PlaybackFixture(BlockState initialState, BooleanProperty property, String clipName,
                        String part, boolean delayedModel) {
            this.property = property;
            state = new AtomicReference<>(initialState);
            preparedModel = model(poseSamples, clipName);
            if (!delayedModel) model.set(preparedModel);
            var policy = JYMaintenanceBooleanClipProxy.playback(
                    model::get, property, clipName, part);
            var poseEvaluator = new ModelClipPoseEvaluator(model::get, policy.clips());
            var target = new AnimationTarget<Double>() {
                @Override public AnimationTargetRef ref() { return AnimationTargetRef.property("door-time"); }
                @Override public void publish(TargetPublication<Double> publication) {}
                @Override public void sample(TargetPublication<Double> publication, RenderSampleContext context) {
                    renderSamples.incrementAndGet();
                }
            };
            var visual = new ProxyVisualPort<AtomicReference<BlockState>, Void>() {
                private AnimationRenderPort port;
                @Override public boolean isReady(AtomicReference<BlockState> host) { return true; }
                @Override public void bind(AnimationRenderPort renderPort) { port = renderPort; }
                @Override public void publishFrame(Void frame) {
                    port.sample(new RenderSampleContext(new RenderFrameToken(new Object()),
                            0.5, FrameInputSnapshot.empty()));
                }
            };
            ProxyComposition<AtomicReference<BlockState>, ProxyModelInput, Void, Void, Void> composition =
                    ProxyComposition.of(LogicalStepPolicy.fixed(0.05, 1),
                            host -> new ProxyModelInput(host.get()), policy.controller(),
                            (host, output) -> {}, ProxyCommandPort.none(), visual,
                            List.of(new TargetBinding<>(target, context -> {
                                var output = poseEvaluator.evaluate(context);
                                if (output == null) return null;
                                double time = context.playbackTimes().getLast().currentSourceLocalTime();
                                Vector3f direction = output.pose().bones().get("door").transform()
                                        .applyDirection(new Vector3f(1, 0, 0));
                                sample.set(new Sample(time, direction));
                                return time;
                            }, new TargetPublicationFactory<>())), List.of(), policy.commands());
            var runtime = new ProxyRuntime();
            var definition = new ProxyDefinition<AtomicReference<BlockState>, ProxyModelInput, Void, Void, Void>(
                    ProxyKey.external("test", "jy-" + part + "-playback"), ignored -> composition);
            runtime.register(definition);
            session = runtime.attach(definition, state, ProxyAttachToken.random()).orElseThrow();
        }

        void request(boolean active) { state.set(state.get().setValue(property, active)); }
        void tick() {
            var outcome = session.tick(++gameTime, 0.05);
            assertFalse(outcome.aborted(), () -> String.valueOf(outcome.failure()));
            assertTrue(outcome.advanced());
        }
        void ticks(int count) { for (int tick = 0; tick < count; tick++) tick(); }
        @Override public void close() { session.close(); }
    }

    private static ModelInstance model(AtomicInteger samples, String clipName) {
        Bone root = new Bone("door", new Transform(), null);
        Skeleton skeleton = new Skeleton(new Bone[]{root}, root, new Anchor[0], null, new Transform());
        Texture texture = new Texture("test", 1f, 1f, null);
        Material material = new Material(new Texture[]{texture}, null);
        MaterialSet materials = new MaterialSet(texture, material);
        Model model = new Model(new Vertex[0], new Mesh[0], new Bone[]{root}, skeleton, materials,
                MeshMode.TRIANGLES, null, null);
        var animation = new BbModelAnimation(clipName, BbModelAnimation.Loop.HOLD, 0.5f, 24, "",
                List.of(new BbModelAnimation.BoneAnim("door", Map.of(BbModelAnimation.Channel.ROTATION,
                        List.of(keyframe(0, 0), keyframe(0.25f, 90))))));
        model.getAnimations().register(clipName, new AnimationSampler<BbModelAnimation>() {
            @Override public float duration(BbModelAnimation data) { return BbModelSampler.INSTANCE.duration(data); }
            @Override public Pose sample(BbModelAnimation data, float time) {
                samples.incrementAndGet();
                return BbModelSampler.INSTANCE.sample(data, time);
            }
        }, animation);
        return new ModelInstance(model, null, null, null, new MaterialSetInstance(materials), null);
    }

    private static BbModelAnimation.Keyframe keyframe(float time, float yaw) {
        var point = new BbModelAnimation.DataPoint(new Vector3f(0, yaw, 0), "0", String.valueOf(yaw), "0");
        return new BbModelAnimation.Keyframe(time, BbModelAnimation.Interpolation.LINEAR,
                point, point, null, null);
    }
}
