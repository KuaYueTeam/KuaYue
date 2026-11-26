package cn.kycraft.kuayue.overhead_line.wire;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.overhead_line.block.line.OverheadLineConnection;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = KuaYue.MODID)
public class OverheadLineSync {
    private static final WeakHashMap<ServerLevel, Set<OverheadLineConnection>> SERVER = new WeakHashMap<>();
    private static final Set<OverheadLineConnection> CONNECTIONS = new HashSet<>();

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event){
        event.registrar("1").playToClient(OverheadLineConnection.TYPE,OverheadLineConnection.STREAM_CODEC,(link,context) -> CONNECTIONS.add(link));
    }

    public static boolean add(ServerLevel level , OverheadLineConnection link){
        if (!SERVER.computeIfAbsent(level,ignore -> new HashSet<>()).add(link))
        {return false;}
        PacketDistributor.sendToPlayersInDimension(level , link);
        return true;
    }

    public static final Set<OverheadLineConnection> clientConnections(){
        return Collections.unmodifiableSet(CONNECTIONS);
    }

    public static void clearClient(){
        CONNECTIONS.clear();
    }
}
