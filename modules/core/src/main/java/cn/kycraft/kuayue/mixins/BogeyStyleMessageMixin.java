package cn.kycraft.kuayue.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.ITrackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StationBlockEntity.class, remap = false)
public abstract class BogeyStyleMessageMixin {

    @Inject(
            method = "trackClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/bogey/AbstractBogeyBlockEntity;setBogeyData(Lnet/minecraft/nbt/CompoundTag;)V"
            ),
            remap = false
    )
    private void kuayue$showBogeyStyle(
            Player player,
            InteractionHand hand,
            ITrackBlock track,
            BlockState state,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir,
            @Local(ordinal = 2) BlockState newBlock
    ) {
        Level level = ((StationBlockEntity) (Object) this).getLevel();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(newBlock.getBlock());
        if (level == null || !blockId.getNamespace().equals("kuayue")) {
            return;
        }
        player.displayClientMessage(
                Component.translatable(
                        "msg.bogey.style.changed." + newBlock.getBlock().getDescriptionId()
                ),
                true
        );
    }
}
