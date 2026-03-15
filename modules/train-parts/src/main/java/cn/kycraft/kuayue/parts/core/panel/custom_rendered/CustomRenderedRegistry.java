package cn.kycraft.kuayue.parts.core.panel.custom_rendered;

import cn.kycraft.kuayue.parts.KuaYueTrainPanelModule;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.renderer.CustomRenderedDoorRenderer;
import cn.kycraft.kuayue.parts.core.panel.custom_rendered.renderer.CustomRenderedEndFaceRenderer;
import io.micronaut.context.annotation.Context;
import lib.kasuga.registration.RegistryGroup;
import lib.kasuga.registration.minecraft.block.BlockRegConfigurations;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityRendererReg;
import net.minecraft.world.level.block.SoundType;

@Context
public class CustomRenderedRegistry {
    public static final RegistryGroup CUSTOM_RENDERED_REGISTRY = new RegistryGroup()
            .configure(BlockRegConfigurations.adopt(block -> block
                    .noOcclusion()
                    .strength(1.5f, 3f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)))
            .setParent(KuaYueTrainPanelModule.REGISTRY_GROUP);

    public static final BlockEntityReg<CustomRenderedDoorEntity> CUSTOM_RENDERED_DOOR_ENTITY =
            BlockEntityReg.of("custom_rendered_door_entity", CustomRenderedDoorEntity::new)
                    .validBlocks(((resourceLocation, block) -> block instanceof CustomRenderedDoorBlock))
                    .setParent(CUSTOM_RENDERED_REGISTRY);

    public static final BlockEntityRendererReg<CustomRenderedDoorEntity> CUSTOM_RENDERED_DOOR_RENDERER =
            new BlockEntityRendererReg<>(() -> CustomRenderedDoorRenderer::new)
                    .withBlockEntity(CUSTOM_RENDERED_DOOR_ENTITY::getEntry)
                    .setParent(CUSTOM_RENDERED_REGISTRY);

    public static final BlockEntityReg<CustomRenderedEndFaceEntity> CUSTOM_RENDERED_END_FACE_ENTITY =
            BlockEntityReg.of("custom_rendered_endface_entity", CustomRenderedEndFaceEntity::new)
                    .validBlocks(((resourceLocation, block) -> block instanceof CustomRenderedEndFaceBlock))
                    .setParent(CUSTOM_RENDERED_REGISTRY);

    public static final BlockEntityRendererReg<CustomRenderedEndFaceEntity> CUSTOM_RENDERED_END_FACE_RENDERER =
            new BlockEntityRendererReg<>(() -> CustomRenderedEndFaceRenderer::new)
                    .withBlockEntity(CUSTOM_RENDERED_END_FACE_ENTITY::getEntry)
                    .setParent(CUSTOM_RENDERED_REGISTRY);
}
