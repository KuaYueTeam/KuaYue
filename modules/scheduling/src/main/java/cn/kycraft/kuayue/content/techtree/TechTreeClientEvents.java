package cn.kycraft.kuayue.content.techtree;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import cn.kycraft.kuayue.content.techtree.net.TechTreeRpc;

/**
 * Client glue: warms the snapshot, keeps an open board fresh and drops every mount on exit.
 *
 * <p>Both listeners are registered by hand in {@link #register()} from the module bootstrap. The
 * annotation below is kept for the events that do fire through the mod container's scan, but the
 * module ships in its own jar and that scan is not reliable for it, which is why the tick and login
 * paths are explicit.</p>
 */
@EventBusSubscriber(modid = "kuayue", value = Dist.CLIENT)
public final class TechTreeClientEvents {

    private static final int REFRESH_TICKS = 10;
    /**
     * Mount/prune cadence, in ticks.
     *
     * <p>Two seconds: tables do not move, and new ones arrive through {@code PENDING_MOUNTS} (chunk
     * loads) rather than from a search, so this pass only drains that list and validates live mounts.
     * The old ten-tick cadence existed because the pass used to walk a 129^3 block cube.</p>
     */
    private static final int MOUNT_TICKS = 40;

    private static int tickCounter;
    private static int refreshCounter;
    private static boolean registered;

    private TechTreeClientEvents() {
    }

    /** Registers the game-bus listeners; idempotent. */
    /**
     * Diagnostic switch: when {@code false}, every client hook of this module is skipped and the blueprint
     * table renders as a plain block.
     *
     * <p>Set from the JVM property {@code -Dkuayue.disableTechTree=true} so the A/B comparison needs a
     * restart but no code edit. With the hooks off there is no snapshot request, no nearby scan, no board
     * mount and no world-UI registration — anything that still stutters then is not coming from here.</p>
     */
    private static final boolean ENABLED = !Boolean.getBoolean("kuayue.disableTechTree");

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        if (!ENABLED) {
            TechTreeSystem.logWarn("Tech tree client hooks DISABLED (-Dkuayue.disableTechTree=true)");
            return;
        }
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(TechTreeClientEvents::onScratchTick);
        bus.addListener(TechTreeClientEvents::onScratchLogin);
        bus.addListener(TechTreeClientEvents::onScratchLogout);
        bus.addListener(TechTreeClientEvents::onChunkLoad);
        TechTreeSystem.logInfo("Tech tree client hooks registered on the game bus");
    }

    /**
     * Announces blueprint tables of a chunk that just loaded.
     *
     * <p>This is what replaced the 129^3 block scan: a table entering the client's view reports itself
     * once, and the throttled mount pass only has to drain that list.</p>
     */
    private static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
        if (!ENABLED) return;
        if (!(event.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel)) return;
        if (!(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) return;
        chunk.getBlockEntities().forEach((pos, entity) -> {
            if (entity instanceof BlueprintTableBlockEntity) TechTreeTableSurface.announce(pos);
        });
    }

    private static void onScratchLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        onClientLogin(event);
    }

    private static void onScratchLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        onClientLogout(event);
    }

    private static void onScratchTick(PlayerTickEvent.Post event) {
        onPlayerTick(event);
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!ENABLED) return;
        tickCounter = 0;
        refreshCounter = 0;
        // The board is permanent furniture, so every table already in the level has to be mounted on join
        // instead of on first use.
        Minecraft.getInstance().execute(TechTreeTableSurface::mountLoadedChunks);
        // Warm the tree cache on join: the board renders from ClientTreeState and has nothing to lay out
        // until the first snapshot arrives.
        TechTreeRpc.open().run(new TechTreeRpc.OpenPayload(false))
                .thenAccept(snapshot -> Minecraft.getInstance().execute(() -> {
                    ClientTreeState.apply(snapshot);
                    TechTreeTableSurface.invalidateVisibility();
                }));
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (!ENABLED) return;
        TechTreeTableSurface.closeAll();
        TechTreeScreen.clearMenuFocus();
        CarModelUiDemo.closeAll();
        ClientTreeState.reset();
    }

    /**
     * Throttled refresh of the open board.
     *
     * <p>Only an actually mounted surface asks for a snapshot, and the answer is compared with the one
     * already held: an unchanged tree does not repaint, so a player standing at a table costs one small
     * request every {@value #REFRESH_TICKS} ticks and no drawing at all.</p>
     */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!ENABLED) return;
        if (!event.getEntity().level().isClientSide()) return;
        if (event.getEntity() != Minecraft.getInstance().player) return;

        // The pointer screen is a vanilla screen; when it goes away (Escape, F1, a chat screen) the board
        // has to come down with it, which the surface checks one tick later.
        TechTreeTableSurface.schedulePointerClose();

        if (++tickCounter % MOUNT_TICKS != 0) return;
        CarModelUiDemo.pruneStale();
        TechTreeTableSurface.pruneStale();
        TechTreeTableSurface.mountNearby();

        // Walking back into range has to repaint once; the boards were skipped while nobody looked.
        TechTreeTableSurface.repaintOnApproach();

        // Refreshing the whole tree every 10 ticks costs a full snapshot round trip and a repaint per
        // answer, whether or not anything changed. It only matters when the board is actually being read,
        // so the poll is limited to a mounted board inside the draw distance.
        if (!TechTreeTableSurface.hasVisibleBoard()) return;
        if (++refreshCounter % REFRESH_TICKS != 0) return;
        TechTreeRpc.open().run(new TechTreeRpc.OpenPayload(false))
                .thenAccept(snapshot -> Minecraft.getInstance().execute(() -> {
                    if (ClientTreeState.applyIfChanged(snapshot)) {
                        TechTreeTableSurface.invalidateVisibility();
                    }
                }));
    }
}
