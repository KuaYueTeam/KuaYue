package cn.kycraft.kuayue.parts.bogeys.core;

import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CommonBogeyBlockEntity extends AbstractBogeyBlockEntity {

    public CommonBogeyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CommonBogeyBlockEntity(BlockPos pos, BlockState state) {
        this(CommonBogeyBlock.GLOBAL_BE_TYPE.get(), pos, state);
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        if (getBlockState().getBlock() instanceof CommonBogeyBlock block)
            return block.getDefaultStyle();
        return AllBogeyStyles.STANDARD;
    }
}
