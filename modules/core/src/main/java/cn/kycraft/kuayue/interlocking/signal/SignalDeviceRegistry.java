package cn.kycraft.kuayue.interlocking.signal;

import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import lib.kasuga.create.content.train.signal.CustomTrackSegment;
import lib.kasuga.create.registration.edge_point.TrackEdgePointReg;
import lib.kasuga.registration.Reg;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;
import java.util.function.Supplier;

public class SignalDeviceRegistry<B extends Block, E extends BlockEntity, P extends TrackEdgePoint> extends Reg<SignalDeviceRegistry<B, E, P>, Void> {

    private final BlockReg<B> block;
    private final BlockEntityReg<E> blockEntity;
    private final TrackEdgePointReg<P> edgePoint;

    public SignalDeviceRegistry(
            String boundaryName,
            Function<BlockBehaviour.Properties, B> blockSupplier,
            BlockEntityType.BlockEntitySupplier<E> blockEntitySupplier,
            Supplier<P> edgePointSupplier

    ) {
        this.edgePoint = new TrackEdgePointReg<>(boundaryName, edgePointSupplier).setParent(this);
        this.block = BlockReg.of(boundaryName, blockSupplier).withBlockItem(boundaryName, edgePoint::getBlockItemFactory).setParent(this);
        this.blockEntity = BlockEntityReg.of(boundaryName + "_tile", blockEntitySupplier).validBlocks(block::getEntry).setParent(this);
    }

    public BlockEntityReg<E> getBlockEntity() {
        return blockEntity;
    }

    public BlockReg<B> getBlock() {
        return block;
    }

    public TrackEdgePointReg<P> getEdgePoint() {
        return edgePoint;
    }

    @Override
    public Void getEntry() {
        return null;
    }
}
