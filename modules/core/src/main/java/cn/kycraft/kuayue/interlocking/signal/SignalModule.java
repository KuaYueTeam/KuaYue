package cn.kycraft.kuayue.interlocking.signal;

import cn.kycraft.kuayue.KuaYue;
import cn.kycraft.kuayue.interlocking.signal.circuit.*;
import cn.kycraft.kuayue.interlocking.signal.entry.StationEntry;
import cn.kycraft.kuayue.interlocking.signal.entry.StationEntryBlock;
import cn.kycraft.kuayue.interlocking.signal.entry.StationEntryBlockEntity;
import cn.kycraft.kuayue.interlocking.signal.platform.StationExit;
import cn.kycraft.kuayue.interlocking.signal.platform.StationExitBlock;
import cn.kycraft.kuayue.interlocking.signal.platform.StationExitBlockEntity;
import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lib.kasuga.create.content.train.device.TrainDeviceRegistry;
import lib.kasuga.create.content.train.device.TrainDeviceSystemType;
import lib.kasuga.create.content.train.signal.BoundarySegmentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Context()
public class SignalModule {
    public static SignalDeviceRegistry<StationEntryBlock, StationEntryBlockEntity, StationEntry> STATION_ENTRY =
            new SignalDeviceRegistry<>(
                    "station_entry",
                    StationEntryBlock::new,
                    StationEntryBlockEntity::new,
                    StationEntry::new
            ).setParent(KuaYue.REGISTRY);

    public static SignalDeviceRegistry<StationExitBlock, StationExitBlockEntity, StationExit> STATION_EXIT =
            new SignalDeviceRegistry<>(
                    "station_exit",
                    StationExitBlock::new,
                    StationExitBlockEntity::new,
                    StationExit::new
            ).setParent(KuaYue.REGISTRY);

    public static SignalDeviceRegistry<TrackInsulatorBlock, TrackInsulatorBlockEntity, TrackInsulator> INSULATOR =
            new SignalDeviceRegistry<>(
                    "track_insulator",
                    TrackInsulatorBlock::new,
                    TrackInsulatorBlockEntity::new,
                    TrackInsulator::new
            ).setParent(KuaYue.REGISTRY);

    public static ResourceLocation INSULATOR_SEGMENT_ID =  ResourceLocation.tryBuild(KuaYue.MODID, "track_circuit");

    @Inject() @Named("modEventBus")
    IEventBus eventBus;
    @PostConstruct
    public void init() {
        this.eventBus.addListener(this::onFMLCommonSetupEvent);
    }

    public TrainDeviceSystemType<BogeyCircuitConnectorDevice> BOGEY_CIRCUIT_CONNECTOR =
            new TrainDeviceSystemType<>(BogeyCircuitConnectorDevice::new);

    public void onFMLCommonSetupEvent(FMLCommonSetupEvent event) {
        BoundarySegmentRegistry.register(
                INSULATOR_SEGMENT_ID,
                INSULATOR.getEdgePoint().getEntry(),
                TrackCircuitSegment::new
        );

        TrainDeviceRegistry.register(
                ResourceLocation.tryBuild(KuaYue.MODID, "bogey_circuit_connector"),
                BOGEY_CIRCUIT_CONNECTOR,
                true
        );
    }

}
