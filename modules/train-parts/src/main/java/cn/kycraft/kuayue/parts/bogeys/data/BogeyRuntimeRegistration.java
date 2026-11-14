package cn.kycraft.kuayue.parts.bogeys.data;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlock;
import cn.kycraft.kuayue.parts.bogeys.core.CommonBogeyBlockEntity;
import cn.kycraft.kuayue.parts.bogeys.core.KuaYueBogeyProxyComposition;
import lib.kasuga.create.registration.bogey.BogeyBlockReg;
import lib.kasuga.registration.data_driven.context.RegBuildContext;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;
import lib.kasuga.registration.minecraft.block.BlockSupplier;
import lib.kasuga.rendering.models.mc.proxies.BlockEntityProxyDefinitions;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Creates the shared bogey block entity registration after all data-driven blocks are known.
 */
final class BogeyRuntimeRegistration {
    private BogeyRuntimeRegistration() {
    }

    static void ensure(RegBuildContext context) {
        if (context.getMeta(BogeyDataDrivenTypes.RUNTIME,
                BogeyDataDrivenTypes.BLOCK_ENTITY) != null) {
            return;
        }
        List<BogeyBlockReg<CommonBogeyBlock>> registrations =
                context.getMeta(BogeyDataDrivenTypes.RUNTIME,
                        BogeyDataDrivenTypes.BLOCK_REGISTRATIONS);
        if (registrations == null || registrations.isEmpty()) {
            return;
        }

        BlockEntityReg<CommonBogeyBlockEntity> blockEntity =
                new BlockEntityReg<CommonBogeyBlockEntity>(
                "loco_bogey_entity",
                reg -> (pos, state) -> new CommonBogeyBlockEntity(reg.getEntry(), pos, state)
        );
        BlockSupplier[] validBlocks = registrations.stream()
                .map(registration -> (BlockSupplier) registration::getEntry)
                .toArray(BlockSupplier[]::new);
        blockEntity.validBlocks(validBlocks);
        blockEntity.onRegistered(type -> BlockEntityProxyDefinitions.register(
                type,
                ResourceLocation.fromNamespaceAndPath("kuayue", "loco_bogey"),
                KuaYueBogeyProxyComposition::create
        )).setParent(KuaYueTrainPanelModule.RUNTIME_REGISTRY_GROUP);
        CommonBogeyBlock.setGlobalBlockEntityType(blockEntity::getEntry);
        context.putMeta(BogeyDataDrivenTypes.RUNTIME,
                BogeyDataDrivenTypes.BLOCK_ENTITY, blockEntity);
    }
}
