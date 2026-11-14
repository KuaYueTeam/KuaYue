package cn.kycraft.kuayue.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 让合并进同一个 {@link SignalBoundary} 的方块实体，在「不是信号方块」时同时登记到两侧。
 *
 * <p>Create 在 {@code TrackTargetingBehaviour#createEdgePoint} 里遇到同类型边界会把新的方块实体
 * 交给 {@code blockEntityAdded(blockEntity, front)}，{@code front} 只表示这次放置朝向的一侧；
 * 单方块虚拟边界（方块状态为空气）需要两侧都能通行/显示，所以这里补登另一侧。</p>
 *
 * <p>TODO: 判定条件目前是「方块状态是空气」，后续换成 KuaYue 自己的方块 tag。
 * 该分支在 Create 的合并规则下只在虚拟/无方块状态时命中，对现有 Create 信号无行为改动。</p>
 */
@Mixin(value = SignalBoundary.class, remap = false)
public abstract class SignalBoundaryMixin {

    @WrapMethod(method = "blockEntityAdded")
    private void kuayue$onBlockEntityAdded(BlockEntity blockEntity, boolean front, Operation<Void> original) {
        if (!blockEntity.getBlockState().is(Blocks.AIR)) {
            original.call(blockEntity, front);
            return;
        }
        // 原来的实现把 front 侧调了两次，另一侧再调一次；这里每侧只登记一次
        original.call(blockEntity, true);
        original.call(blockEntity, false);
    }
}
