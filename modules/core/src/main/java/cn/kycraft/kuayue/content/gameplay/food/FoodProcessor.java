package cn.kycraft.kuayue.content.gameplay.food;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 加工点接口：干泡面等物品右键实现了该接口的方块时，会把物品转换为加工产物。
 *
 * <p> replaces the 1.0 hard-coded {@code instanceof TeaBoilerBlock} check, so a tea boiler (or
 * anything else) can act as a processing station without the item knowing about it.</p>
 */
public interface FoodProcessor {

    /** 该加工点能否处理这个物品栈。 */
    boolean canProcess(ItemStack stack);

    /** 执行加工，返回产物、音效与粒子。调用方负责消费/替换玩家手中的物品栈。 */
    ProcessResult process(Level level, BlockPos pos, Player player, ItemStack stack);

    /**
     * @param output   加工后的物品栈
     * @param sound    加工音效
     * @param particle 加工粒子
     */
    record ProcessResult(ItemStack output, net.minecraft.sounds.SoundEvent sound,
                         net.minecraft.core.particles.ParticleOptions particle) {
    }
}
