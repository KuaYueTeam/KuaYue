package cn.kycraft.kuayue.interlocking.signal.circuit;

import cn.kycraft.kuayue.interlocking.signal.SignalModule;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TrackInsulatorBlock extends Block implements IBE<TrackInsulatorBlockEntity> {
    public TrackInsulatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<TrackInsulatorBlockEntity> getBlockEntityClass() {
        return TrackInsulatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TrackInsulatorBlockEntity> getBlockEntityType() {
        return SignalModule.INSULATOR.getBlockEntity().getEntry();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }
}
