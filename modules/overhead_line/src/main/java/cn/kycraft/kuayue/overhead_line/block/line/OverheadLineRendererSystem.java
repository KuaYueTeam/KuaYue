//package cn.kycraft.kuayue.overhead_line.block.line;
//
//import cn.kycraft.kuayue.KuaYue;
//import cn.kycraft.kuayue.overhead_line.block.support.OverheadLineSupportBlockEntity;
//import cn.kycraft.kuayue.overhead_line.render.RenderCurve;
//import cn.kycraft.kuayue.overhead_line.types.OverheadLineType;
//import lib.kasuga.structure.Pair;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.culling.Frustum;
//import net.minecraft.client.renderer.texture.OverlayTexture;
//import net.minecraft.core.BlockPos;
//import net.minecraft.resources.ResourceKey;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.phys.AABB;
//import net.minecraft.world.phys.Vec3;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//import java.util.function.Supplier;
//
//@EventBusSubscriber(modid = KuaYue.MODID, value = Dist.CLIENT)
//public final class OverheadLineRendererSystem {
//    public static final Map<OverheadLineType, OverheadLineRenderer> RENDERERS = new HashMap<>();
//    public static final Map<OverheadLineLocator, Rendering> OVERHEAD_LINES = new HashMap<>();
//    public static final Map<OverheadLineLocator, Pair<Boolean, Boolean>> LOCK = new HashMap<>();
//
//    private OverheadLineRendererSystem() {
//    }
//
//    public static void registerRenderer(
//            OverheadLineType wireType,
//            Supplier<Supplier<OverheadLineRenderer>> renderer
//    ) {
//        Objects.requireNonNull(wireType, "wireType");
//        Objects.requireNonNull(renderer, "renderer");
//        RENDERERS.put(wireType, Objects.requireNonNull(renderer.get().get(), "renderer instance"));
//    }
//
//    public static OverheadLineRenderer getRendererFor(OverheadLineType type) {
//        OverheadLineRenderer renderer = RENDERERS.get(type);
//        if (renderer == null) {
//            throw new IllegalArgumentException("Unknown wire type: " + type);
//        }
//        return renderer;
//    }
//
//    public record OverheadLineLocator(
//            ResourceKey<Level> level,
//            BlockPos fromPosition,
//            BlockPos toPosition,
//            int fromIndex,
//            int toIndex
//    ) {
//        public OverheadLineLocator {
//            Objects.requireNonNull(level, "level");
//            Objects.requireNonNull(fromPosition, "fromPosition");
//            Objects.requireNonNull(toPosition, "toPosition");
//        }
//
//        public static OverheadLineLocator createFromConnection(
//                ResourceKey<Level> level,
//                BlockPos fromPosition,
//                OverheadLineSupportBlockEntity.Connection connection
//        ) {
//            Objects.requireNonNull(connection, "connection");
//            BlockPos targetPosition = connection.absolutePos();
//            int fromIndex = connection.connectionIndex();
//            int targetIndex = connection.targetIndex();
//
//            int comparison = compareEndpoint(fromPosition, fromIndex, targetPosition, targetIndex);
//            if (comparison <= 0) {
//                return new OverheadLineLocator(
//                        level, fromPosition.immutable(), targetPosition.immutable(), fromIndex, targetIndex
//                );
//            }
//            return new OverheadLineLocator(
//                    level, targetPosition.immutable(), fromPosition.immutable(), targetIndex, fromIndex
//            );
//        }
//
//        private static int compareEndpoint(BlockPos firstPos, int firstIndex, BlockPos secondPos, int secondIndex) {
//            int positionComparison = PositionComparator.compareBlockPosition(firstPos, secondPos);
//            return positionComparison != 0 ? positionComparison : Integer.compare(firstIndex, secondIndex);
//        }
//
//        public boolean isFromEndpoint(BlockPos position, int index) {
//            return fromIndex == index && fromPosition.equals(position);
//        }
//    }
//
//    public record Rendering(RenderCurve curve, OverheadLineRenderer renderer, AABB boundingBox) {
//        public Rendering {
//            Objects.requireNonNull(curve, "curve");
//            Objects.requireNonNull(renderer, "renderer");
//        }
//    }
//
//    private static Pair<Boolean, Boolean> replace(
//            Pair<Boolean, Boolean> source,
//            boolean first,
//            boolean value
//    ) {
//        Pair<Boolean, Boolean> actual = source == null ? Pair.of(false, false) : source;
//        return first
//                ? Pair.of(value, actual.getSecond())
//                : Pair.of(actual.getFirst(), value);
//    }
//
//    public static void registerOverheadLine(
//            OverheadLineSupportBlockEntity blockEntity,
//            OverheadLineSupportBlockEntity.Connection connection
//    ) {
//        Level level = blockEntity.getLevel();
//        if (level == null || !level.isClientSide()) {
//            return;
//        }
//
//        OverheadLineLocator locator = OverheadLineLocator.createFromConnection(
//                level.dimension(), blockEntity.getBlockPos(), connection
//        );
//        boolean firstEndpoint = locator.isFromEndpoint(
//                blockEntity.getBlockPos(), connection.connectionIndex()
//        );
//        Pair<Boolean, Boolean> oldLock = LOCK.get(locator);
//        LOCK.put(locator, replace(oldLock, firstEndpoint, true));
//
//        if (oldLock == null) {
//            return;
//        }
//
//        OverheadLineRenderer renderer = getRendererFor(connection.type());
//        RenderCurve curve = renderer.getRenderCurveFor(
//                level,
//                blockEntity.getConnectionPointByIndex(connection.connectionIndex()),
//                new Vec3(connection.toPosition())
//        );
//        OVERHEAD_LINES.put(locator, new Rendering(curve, renderer, curve.getMatrix().getBoundingBox()));
//    }
//
//    public static void removeOverheadLine(
//            OverheadLineSupportBlockEntity blockEntity,
//            OverheadLineSupportBlockEntity.Connection connection
//    ) {
//        Level level = blockEntity.getLevel();
//        if (level == null) {
//            return;
//        }
//
//        OverheadLineLocator locator = OverheadLineLocator.createFromConnection(
//                level.dimension(), blockEntity.getBlockPos(), connection
//        );
//        Pair<Boolean, Boolean> oldLock = LOCK.get(locator);
//        if (oldLock == null) {
//            OVERHEAD_LINES.remove(locator);
//            return;
//        }
//
//        boolean firstEndpoint = locator.isFromEndpoint(
//                blockEntity.getBlockPos(), connection.connectionIndex()
//        );
//        Pair<Boolean, Boolean> newLock = replace(oldLock, firstEndpoint, false);
//        if (!newLock.getFirst() && !newLock.getSecond()) {
//            OVERHEAD_LINES.remove(locator);
//            LOCK.remove(locator);
//            return;
//        }
//        LOCK.put(locator, newLock);
//    }
//
//    @SubscribeEvent
//    public static void onRenderLevelLast(RenderLevelStageEvent event) {
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
//            return;
//        }
//
//        Minecraft minecraft = Minecraft.getInstance();
//        ClientLevel level = minecraft.level;
//        if (level == null) {
//            return;
//        }
//
//        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
//        Frustum frustum = event.getFrustum();
//        Vec3 cameraPosition = event.getCamera().getPosition();
//
//        event.getPoseStack().pushPose();
//        event.getPoseStack().translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
//        try {
//            OVERHEAD_LINES.forEach((locator, rendering) -> {
//                if (!locator.level().equals(level.dimension())) {
//                    return;
//                }
//                AABB boundingBox = rendering.boundingBox();
//                if (boundingBox != null && !frustum.isVisible(boundingBox)) {
//                    return;
//                }
//
//                event.getPoseStack().pushPose();
//                try {
//                    rendering.renderer().render(
//                            rendering.curve(),
//                            event.getPoseStack(),
//                            bufferSource,
//                            OverlayTexture.NO_OVERLAY
//                    );
//                } finally {
//                    event.getPoseStack().popPose();
//                }
//            });
//        } finally {
//            event.getPoseStack().popPose();
//        }
//    }
//
//    public static void clearCache() {
//        OVERHEAD_LINES.clear();
//        LOCK.clear();
//        OverheadLineRendererBridge.REGISTERED.clear();
//    }
//}
