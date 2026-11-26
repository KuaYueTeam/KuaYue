package cn.kycraft.kuayue.overhead_line.wire;

import cn.kycraft.kuayue.overhead_line.block.line.OverheadLineConnection;
import cn.kycraft.kuayue.overhead_line.block.support.variants.NormalOverheadSupportBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class Allwires extends Item {
    private static final double MIN_HORIZONTAL_SPAN = 1.0e-4;
    private final OverheadLineWireKind kind;
    private final Map<ServerPlayer,OverheadLineEndpoint> firstSelections = new WeakHashMap<>();

    public Allwires(Properties properties,OverheadLineWireKind kind){super(properties);this.kind = kind;}

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState blockState = level.getBlockState(pos);

        if (context.getPlayer() == null || !(blockState.getBlock() instanceof NormalOverheadSupportBlock supportBlock)){return InteractionResult.PASS;}
        int index = nearestAnchor(supportBlock , blockState , pos , context.getClickLocation() );
        if (index < 0){return InteractionResult.PASS;}
        if (level instanceof ServerLevel serverLevel && context.getPlayer() instanceof ServerPlayer player) {
            selectEndPoint(serverLevel, player, new OverheadLineEndpoint(serverLevel.dimension(), pos, index));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static int nearestAnchor(NormalOverheadSupportBlock supportBlock , BlockState blockState ,BlockPos blockPos , Vec3 hit){
        int nearestAnchor = -1;
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0 ; i < supportBlock.getConnectionPointCount() ; i++){
            Vec3 anchor = supportBlock.resolveAnchor(blockPos , blockState , i).orElse(null);
            if (anchor == null) {continue;}
            double distance = anchor.distanceToSqr(hit);
            if (distance < best){
                best = distance;
                nearestAnchor = i;
            }
        }
        return nearestAnchor;
    }

    private void selectEndPoint(ServerLevel level, ServerPlayer player, OverheadLineEndpoint current) {
        OverheadLineEndpoint first = firstSelections.get(player);
        if (first == null) {
            firstSelections.put(player, current);
            player.sendSystemMessage(Component.literal(
                    "已选择第一个挂点：" + current.blockPos() + " #" + current.pointIndex()));
            return;
        }

        if (!first.dimension().equals(current.dimension())) {
            firstSelections.remove(player);
            player.sendSystemMessage(Component.literal("两个挂点必须在同一个维度，请重新选点"));
            return;
        }
        if (first.equals(current)) {
            player.sendSystemMessage(Component.literal("这是同一个挂点，请选择另外一个"));
            return;
        }

        Vec3 firstAnchor = resolveLoadedAnchor(level, first);
        if (firstAnchor == null) {
            firstSelections.remove(player);
            player.sendSystemMessage(Component.literal("第一个挂点已失效或区块未加载，请重新选点"));
            return;
        }
        Vec3 currentAnchor = resolveLoadedAnchor(level, current);
        if (currentAnchor == null) {
            player.sendSystemMessage(Component.literal("第二个挂点已失效或区块未加载，请另选挂点"));
            return;
        }
        double horizontalSpan = Math.hypot(currentAnchor.x - firstAnchor.x, currentAnchor.z - firstAnchor.z);
        if (horizontalSpan < MIN_HORIZONTAL_SPAN) {
            player.sendSystemMessage(Component.literal("两个挂点的水平距离太小，请另选挂点"));
            return;
        }

        OverheadLineConnection connection = new OverheadLineConnection(first, current, kind);
        boolean added = OverheadLineSync.add(level, connection);
        firstSelections.remove(player);
        if (!added) {
            player.sendSystemMessage(Component.literal("这条连接已经存在"));
            return;
        }
        player.sendSystemMessage(Component.literal(
                "已建立连接：#" + first.pointIndex() + " → #" + current.pointIndex()));
    }

    private static Vec3 resolveLoadedAnchor(ServerLevel level, OverheadLineEndpoint endpoint) {
        BlockPos pos = endpoint.blockPos();
        ChunkPos chunkPos = new ChunkPos(pos);
        var chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (chunk == null) {
            return null;
        }
        BlockState state = chunk.getBlockState(pos);
        if (!(state.getBlock() instanceof NormalOverheadSupportBlock supportBlock)) {
            return null;
        }
        return supportBlock.resolveAnchor(pos, state, endpoint.pointIndex()).orElse(null);
    }
}

