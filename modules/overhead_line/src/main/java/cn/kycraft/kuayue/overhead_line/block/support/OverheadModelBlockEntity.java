package cn.kycraft.kuayue.overhead_line.block.support;

import cn.kycraft.kuayue.overhead_line.init.OverheadLineFactoies;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OverheadModelBlockEntity extends BlockEntity {
    private Runnable releaseClientModel;

    public OverheadModelBlockEntity(BlockPos pos, BlockState state) {
        super(OverheadLineFactoies.MODEL_ENTITY.getEntry(), pos, state);
    }

    public void onClientModelRelease(Runnable callback) {
        releaseClientModel = callback;
    }

    private void releaseModel() {
        Runnable release = releaseClientModel;
        releaseClientModel = null;
        if (release != null) release.run();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        releaseModel();
    }

    @Override
    public void onChunkUnloaded() {
        releaseModel();
    }
}
