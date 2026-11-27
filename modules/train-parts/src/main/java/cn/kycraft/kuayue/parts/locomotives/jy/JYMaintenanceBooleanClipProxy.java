package cn.kycraft.kuayue.parts.locomotives.jy;

import cn.kycraft.kuayue.parts.core.panel.proxy.TrainPanelProxyBlockEntity;
import lib.kasuga.rendering.models.mc.proxies.ModelClipPoseEvaluator;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelInput;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelProxy;
import lib.kasuga.rendering.models.mc.typo.bbmodel.BbModelAnimation;
import lib.kasuga.rendering.models.uml.dynamic.ModelInstance;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.ControlEvaluationRequest;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.ControlFrame;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.ControllerCheckpoint;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.PlaybackBinding;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.PlaybackBoundaryPolicy;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.PlaybackCommand;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.PlaybackCommandApplierFactory;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.PlaybackInstanceId;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.PlaybackTimeResolution;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.PlaybackView;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.RuntimeController;
import lib.kasuga.rendering.models.uml.dynamic.animation.scope.ScopedPlaybackCommandApplier;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyComposition;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyControllerPort;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Shared Java playback policy for a vehicle part driven by one boolean state and one clip. */
final class JYMaintenanceBooleanClipProxy {
    private JYMaintenanceBooleanClipProxy() {
    }

    static ProxyComposition<TrainPanelProxyBlockEntity, ProxyModelInput, Void, Void, Void> create(
            TrainPanelProxyBlockEntity element,
            BooleanProperty property,
            String clipName,
            String idPrefix
    ) {
        ProxyModelProxy.CompositionBuilder builder = ProxyModelProxy.builder(element);
        Supplier<@Nullable ModelInstance> model = builder.modelInstance();
        PlaybackPolicy playback = playback(model, property, clipName, idPrefix);
        return builder
                .controller(playback.controller())
                .commandApplier(playback.commands())
                .poseEvaluator(new ModelClipPoseEvaluator(model, playback.clips()))
                .build();
    }

    /** The controller and playback policy are independent of visual mounting and host placement. */
    static PlaybackPolicy playback(
            Supplier<@Nullable ModelInstance> model,
            BooleanProperty property,
            String clipName,
            String idPrefix
    ) {
        ClipIds ids = new ClipIds(
                "kuayue:" + idPrefix + "_off",
                "kuayue:" + idPrefix + "_on",
                "kuayue:" + idPrefix + "_turning_on",
                "kuayue:" + idPrefix + "_turning_off"
        );
        Controller controller = new Controller(model, property, clipName, ids);
        ProxyControllerPort<ProxyModelInput, Void> port = new ProxyControllerPort<>() {
            @Override
            public void prepare(ProxyModelInput input) {
                controller.prepare(input);
            }

            @Override
            public RuntimeController<Void> controller() {
                return controller;
            }

            @Override
            public Void output() {
                return null;
            }
        };
        Map<String, String> clips = Map.of(
                ids.off(), clipName,
                ids.on(), clipName,
                ids.turningOn(), clipName,
                ids.turningOff(), clipName
        );
        return new PlaybackPolicy(port, ScopedPlaybackCommandApplier.factory(
                id -> binding(id, ids, motionDuration(model, clipName))), clips);
    }

    record PlaybackPolicy(
            ProxyControllerPort<ProxyModelInput, Void> controller,
            PlaybackCommandApplierFactory commands,
            Map<String, String> clips
    ) {
    }

    private static @Nullable PlaybackBinding binding(
            String definitionId,
            ClipIds ids,
            float duration
    ) {
        if (!Float.isFinite(duration) || duration <= 0f) {
            return null;
        }
        double length = duration;
        if (definitionId.equals(ids.off())) {
            return new PlaybackBinding(
                    0.0, length, 0.0, length, 0.0, PlaybackBoundaryPolicy.HOLD_LAST);
        }
        if (definitionId.equals(ids.on())) {
            return new PlaybackBinding(
                    0.0, length, length, length, 0.0, PlaybackBoundaryPolicy.HOLD_LAST);
        }
        if (definitionId.equals(ids.turningOn())) {
            return new PlaybackBinding(
                    0.0, length, 0.0, length, 1.0, PlaybackBoundaryPolicy.HOLD_LAST);
        }
        if (definitionId.equals(ids.turningOff())) {
            return new PlaybackBinding(
                    0.0, length * 2.0, 0.0, length, 1.0,
                    PlaybackBoundaryPolicy.PING_PONG);
        }
        return null;
    }

    private static float motionDuration(
            Supplier<@Nullable ModelInstance> model,
            String clipName
    ) {
        ModelInstance instance = model.get();
        if (instance == null) {
            return -1f;
        }
        var clip = instance.getModel().getAnimations().get(clipName);
        if (clip == null) {
            return -1f;
        }
        if (!(clip.data() instanceof BbModelAnimation animation)) {
            return clip.duration();
        }
        float lastKeyframe = 0f;
        for (BbModelAnimation.BoneAnim bone : animation.bones()) {
            for (var keyframes : bone.channels().values()) {
                for (BbModelAnimation.Keyframe keyframe : keyframes) {
                    lastKeyframe = Math.max(lastKeyframe, keyframe.time());
                }
            }
        }
        return lastKeyframe > 0f
                ? Math.min(lastKeyframe, clip.duration())
                : clip.duration();
    }

    private record ClipIds(String off, String on, String turningOn, String turningOff) {
    }

    private static final class Controller implements RuntimeController<Void> {
        private final Supplier<@Nullable ModelInstance> model;
        private final BooleanProperty property;
        private final String clipName;
        private final ClipIds ids;
        private boolean requestedOn;
        @Nullable
        private Boolean issuedState;
        @Nullable
        private PlaybackInstanceId currentPlayback;

        private Controller(
                Supplier<@Nullable ModelInstance> model,
                BooleanProperty property,
                String clipName,
                ClipIds ids
        ) {
            this.model = model;
            this.property = property;
            this.clipName = clipName;
            this.ids = ids;
        }

        private void prepare(ProxyModelInput input) {
            requestedOn = input.state().getValue(property);
        }

        @Override
        public ControllerCheckpoint beginStep() {
            Boolean checkpointIssued = issuedState;
            PlaybackInstanceId checkpointPlayback = currentPlayback;
            return new ControllerCheckpoint() {
                @Override
                public void rollback() {
                    issuedState = checkpointIssued;
                    currentPlayback = checkpointPlayback;
                }

                @Override
                public void commit() {
                }
            };
        }

        @Override
        public ControlFrame<Void> evaluate(ControlEvaluationRequest<Void> request) {
            updateFromObservedPlaybacks(request);
            float duration = motionDuration(model, clipName);
            if (!Float.isFinite(duration) || duration <= 0f) {
                return frame();
            }
            if (issuedState == null) {
                issuedState = requestedOn;
                return frame(new PlaybackCommand.Play(
                        requestedOn ? ids.on() : ids.off(),
                        Optional.empty(),
                        Optional.empty(),
                        0.0
                ));
            }
            if (issuedState == requestedOn) {
                return frame();
            }

            double sourceTime = currentSourceTime(request, duration);
            double placementStart = requestedOn
                    ? request.timeStep().currentTime() - sourceTime
                    : request.timeStep().currentTime() - (duration * 2.0 - sourceTime);
            List<PlaybackCommand> commands = new ArrayList<>(2);
            if (currentPlayback != null) {
                commands.add(new PlaybackCommand.Stop(currentPlayback));
            }
            commands.add(new PlaybackCommand.Play(
                    requestedOn ? ids.turningOn() : ids.turningOff(),
                    Optional.empty(),
                    Optional.of(placementStart),
                    1.0
            ));
            issuedState = requestedOn;
            currentPlayback = null;
            return new ControlFrame<>(null, commands, List.of());
        }

        private void updateFromObservedPlaybacks(ControlEvaluationRequest<Void> request) {
            for (PlaybackView view : request.activePlaybacks()) {
                if (known(view.definitionId())) {
                    currentPlayback = view.id();
                }
            }
        }

        private double currentSourceTime(
                ControlEvaluationRequest<Void> request,
                float duration
        ) {
            if (currentPlayback != null) {
                for (PlaybackTimeResolution time : request.playbackTimes()) {
                    if (time.instanceId().equals(currentPlayback)) {
                        return Math.clamp(time.currentSourceLocalTime(), 0.0, duration);
                    }
                }
            }
            return Boolean.TRUE.equals(issuedState) ? duration : 0.0;
        }

        private boolean known(String definitionId) {
            return definitionId.equals(ids.off())
                    || definitionId.equals(ids.on())
                    || definitionId.equals(ids.turningOn())
                    || definitionId.equals(ids.turningOff());
        }

        private static ControlFrame<Void> frame() {
            return new ControlFrame<>(null, List.of(), List.of());
        }

        private static ControlFrame<Void> frame(PlaybackCommand command) {
            return new ControlFrame<>(null, List.of(command), List.of());
        }
    }
}
