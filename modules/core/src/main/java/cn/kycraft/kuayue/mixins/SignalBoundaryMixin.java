package cn.kycraft.kuayue.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SignalBoundary.class, remap = false)
public abstract class SignalBoundaryMixin {
    @WrapMethod(method = "blockEntityAdded")
    private void kuayue$onBlockEntityAdded(BlockEntity blockEntity, boolean front, Operation<Void> original) {
        if(blockEntity.getBlockState().is(Blocks.AIR/*@TODO to tag later*/)) {
            original.call(blockEntity, front);
            original.call(blockEntity, !front);
        }
        original.call(blockEntity, front);
    }
}
