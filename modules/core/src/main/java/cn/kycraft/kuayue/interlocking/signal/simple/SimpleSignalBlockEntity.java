package cn.kycraft.kuayue.interlocking.signal.simple;

import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SimpleSignalBlockEntity extends SignalBlockEntity {
    public SimpleSignalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean getReportedPower() {
        return false;
    }
}
