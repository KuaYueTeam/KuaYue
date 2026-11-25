package cn.kycraft.kuayue.overhead_line.init;

import cn.kycraft.kuayue.overhead_line.block.decorating.AllOverheadDirectionalBlock;
import cn.kycraft.kuayue.overhead_line.OverheadLineModule;
import cn.kycraft.kuayue.overhead_line.block.support.OverheadModelBlockEntity;
import cn.kycraft.kuayue.overhead_line.block.support.variants.ModelledOverheadSupportBlock;
import cn.kycraft.kuayue.overhead_line.client.OverheadModelRenderer;
import cn.kycraft.kuayue.overhead_line.wire.Allwires;
import cn.kycraft.kuayue.overhead_line.wire.OverheadLineWireKind;
import lib.kasuga.registration.Reg;
import lib.kasuga.registration.factory.FactoryRegistry;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.item.ItemReg;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityReg;
import lib.kasuga.registration.minecraft.block_entity.BlockEntityRendererReg;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;

public class OverheadLineFactoies {
    public static final String OVERHEAD_LINE_WIRE = "wire";
    public static final String OVERHEAD_LINE_SUPPORT = "overhead_line_support";
    public static final String ELECTRONIC_WIRE  = "electronic_wire";
    public static final String SUPPORT = "support";
    public static final String OVERHEAD_DIRECTIONAL = "overhead_directional";

    public static final BlockEntityReg<OverheadModelBlockEntity> MODEL_ENTITY =
            BlockEntityReg.of("overhead_model", OverheadModelBlockEntity::new)
                    .validBlocks((id, block) -> block instanceof ModelledOverheadSupportBlock)
                    .setParent(OverheadLineModule.REGISTRY_GROUP);

    private static final BlockEntityRendererReg<OverheadModelBlockEntity> MODEL_RENDERER =
            new BlockEntityRendererReg<>(() -> OverheadModelRenderer::new)
                    .withBlockEntity(MODEL_ENTITY::getEntry)
                    .setParent(OverheadLineModule.REGISTRY_GROUP);

    private OverheadLineFactoies(){}

    private static boolean registered;
    public static synchronized final void register(){
        if (registered){return;}
        FactoryRegistry.registerItem(OVERHEAD_LINE_WIRE,(id,params) -> {
            ItemReg<Allwires> overhead_line_wire_reg = ItemReg.of(id,(Function<Item.Properties, Allwires>) properties -> new Allwires(properties, OverheadLineWireKind.OVERHEAD_LINE_WIRE));
            return (Reg<ItemReg<?>, Item>)  (Reg<?,?>) overhead_line_wire_reg;
        });

        FactoryRegistry.registerItem(ELECTRONIC_WIRE , (id,params) -> {
            ItemReg<Allwires> electronic_wire_reg = ItemReg.of(id,(Function<Item.Properties, Allwires>) properties -> new Allwires(properties, OverheadLineWireKind.ELECTRONIC_WIRE));
            return (Reg<ItemReg<?>, Item>)  (Reg<?,?>) electronic_wire_reg;
        });

        FactoryRegistry.register(SUPPORT, (id,params) -> (Reg<BlockReg<?>, Block>) (Reg<?, ?>) AllOverheadDirectionalBlock.createSupportRegistration(id, params));

        FactoryRegistry.register(OVERHEAD_DIRECTIONAL, (id, params) ->
                (Reg<BlockReg<?>, Block>) (Reg<?, ?>) AllOverheadDirectionalBlock.createRegistration(id, params));
        registered = true;
    }
}
