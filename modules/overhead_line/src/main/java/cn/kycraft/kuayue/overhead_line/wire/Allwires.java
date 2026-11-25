package cn.kycraft.kuayue.overhead_line.wire;

import cn.kycraft.kuayue.overhead_line.block.support.variants.NormalOverheadSupportBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Allwires extends Item {
    public Allwires(Properties properties, OverheadLineWireKind electronicWire){super(properties);}

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

    }

    public InteractionResult useOn(UseOnContext context){
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();

        if (player == null){return InteractionResult.PASS;}

        Vec3 anchor = findAchor(level,pos,0);
        if (anchor == null){return InteractionResult.PASS;}
        if (!level.isClientSide){
            selectEndPoint(level,player,pos,anchor);
            player.sendSystemMessage(Component.literal("选中第一个挂点" + anchor));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Vec3 findAchor(Level level , BlockPos blockPos , int pointIndex){
        BlockState state = level.getBlockState(blockPos);
        if (!(state.getBlock() instanceof NormalOverheadSupportBlock supportBlock)){
            return null;
        }
        return supportBlock.resolveAnchor(blockPos,state,pointIndex).orElse(null);
    }

    private void selectEndPoint(Level level , Player player , BlockPos blockPos , Vec3 currectAnchor){
        UUID playerId = player.getUUID();
        OverheadLineEndpoint current = new OverheadLineEndpoint(level.dimension() , blockPos.immutable() , 0);
        OverheadLineEndpoint first = OverheadLineFirstElections.get(playerId);

        if (first == null){
            OverheadLineFirstElections.put(playerId,current);
            player.sendSystemMessage(Component.literal("已选择第一个挂点:" + blockPos));
            return;
        }

        if (!first.dimension().equals(current.dimension())){
            OverheadLineFirstElections.remove(playerId);
            player.sendSystemMessage(Component.literal("两个挂点必须在同一个维度，请重新选点"));
            return;
        }
        if (first.equals(current)) {
            player.sendSystemMessage(Component.literal("这是同一个挂点，请选择另外一个"));
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)){return;}
        ChunkPos firstChunk = new ChunkPos(first.blockPos());
        if (serverLevel.getChunkSource().getChunkNow(firstChunk.x, firstChunk.z) == null) {
            OverheadLineFirstElections.remove(playerId);
            player.sendSystemMessage(Component.literal("第一个挂点区块未加载"));
            return;
        }
        Vec3 firstAnchor = findAchor(serverLevel,first.blockPos() , first.pointIndex());
        if (firstAnchor == null){
            OverheadLineFirstElections.remove(playerId);
            player.sendSystemMessage(Component.literal("第一个挂点已失效"));
            return;
        }
        OverheadLineFirstElections.remove(playerId);
        player.sendSystemMessage(Component.literal("A:"+ firstAnchor + ", B:" + current));

    }

    public final Map<UUID , OverheadLineEndpoint> OverheadLineFirstElections = new HashMap<>();
}

