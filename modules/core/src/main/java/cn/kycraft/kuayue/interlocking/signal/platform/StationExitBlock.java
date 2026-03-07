package cn.kycraft.kuayue.interlocking.signal.platform;

import cn.kycraft.kuayue.interlocking.signal.SignalModule;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class StationExitBlock extends Block implements IBE<StationExitBlockEntity> {
    public StationExitBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<StationExitBlockEntity> getBlockEntityClass() {
        return StationExitBlockEntity.class;
    }

    @Override
    public BlockEntityType<StationExitBlockEntity> getBlockEntityType() {
        return SignalModule.STATION_EXIT.getBlockEntity().getEntry();
    }
}
