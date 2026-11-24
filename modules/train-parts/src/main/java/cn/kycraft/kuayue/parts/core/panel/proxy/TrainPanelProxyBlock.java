package cn.kycraft.kuayue.parts.core.panel.proxy;

import cn.kycraft.kuayue.parts.core.multiblock.MultiCellFootprint;
import cn.kycraft.kuayue.parts.core.panel.TrainPanelBlock;
import cn.kycraft.kuayue.parts.core.panel.window.TrainOpenableWindowBlock;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelBlockEntity;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelInput;
import lib.kasuga.rendering.models.mc.proxies.ProxyModelProxy;
import lib.kasuga.rendering.models.mc.proxies.ProxyTickers;
import lib.kasuga.rendering.models.uml.dynamic.proxy.ProxyComposition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;

/**
 * Panel host that keeps TrainPanelBlock semantics while rendering through the proxy BE path.
 */
public class TrainPanelProxyBlock extends TrainPanelBlock implements EntityBlock {
    @Nullable
    private volatile BlockEntityType<?> blockEntityType;

    public TrainPanelProxyBlock(Properties properties, MultiCellFootprint footprint) {
        this(properties, Vec2.ZERO, Vec2.ONE, footprint);
    }

    public TrainPanelProxyBlock(
            Properties properties,
            Vec2 beginPos,
            Vec2 endPos,
            MultiCellFootprint footprint
    ) {
        super(properties, beginPos, endPos);
        configureMultiCell(footprint);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null && state.hasProperty(BlockStateProperties.DOOR_HINGE)) {
            state = state.setValue(
                    BlockStateProperties.DOOR_HINGE,
                    TrainOpenableWindowBlock.getHinge(context)
            );
        }
        return state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        if (mirror == Mirror.NONE) {
            return mirrored;
        }
        if (mirrored.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            mirrored = mirrored.setValue(
                    BlockStateProperties.HORIZONTAL_FACING,
                    mirror.mirror(mirrored.getValue(BlockStateProperties.HORIZONTAL_FACING))
            );
        }
        if (mirrored.hasProperty(BlockStateProperties.DOOR_HINGE)) {
            DoorHingeSide hinge = mirrored.getValue(BlockStateProperties.DOOR_HINGE);
            mirrored = mirrored.setValue(
                    BlockStateProperties.DOOR_HINGE,
                    hinge == DoorHingeSide.LEFT
                            ? DoorHingeSide.RIGHT
                            : DoorHingeSide.LEFT
            );
        }
        return mirrored;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntityType<?> type = blockEntityType;
        if (type != null && type.isValid(state)) {
            return type.create(pos, state);
        }
        for (BlockEntityType<?> candidate : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            if (!candidate.isValid(state)) {
                continue;
            }
            BlockEntity entity = candidate.create(pos, state);
            if (entity instanceof TrainPanelProxyBlockEntity) {
                blockEntityType = candidate;
                return entity;
            }
        }
        return null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        BlockEntityTicker<T> proxyTicker = ProxyTickers.forClientBlockEntity(type);
        return (tickLevel, pos, tickState, blockEntity) -> {
            proxyTicker.tick(tickLevel, pos, tickState, blockEntity);
            if (tickLevel instanceof ServerLevel serverLevel
                    && serverLevel.getGameTime() % 20L == 0L) {
                ensureMultiCell(serverLevel, pos, tickState);
            }
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public ProxyComposition<TrainPanelProxyBlockEntity, ProxyModelInput, Void, Void, Void>
    createProxyComposition(TrainPanelProxyBlockEntity element) {
        return ProxyModelProxy.defaultComposition(element);
    }
}
