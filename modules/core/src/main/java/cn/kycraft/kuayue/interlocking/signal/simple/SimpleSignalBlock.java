package cn.kycraft.kuayue.interlocking.signal.simple;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SimpleSignalBlock extends Block implements IBE<SimpleSignalBlockEntity> {
    public SimpleSignalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<SimpleSignalBlockEntity> getBlockEntityClass() {
        return SimpleSignalBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SimpleSignalBlockEntity> getBlockEntityType() {
        return null;
    }
}
