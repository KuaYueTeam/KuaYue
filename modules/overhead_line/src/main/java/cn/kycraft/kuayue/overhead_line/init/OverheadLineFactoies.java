package cn.kycraft.kuayue.overhead_line.init;

import cn.kycraft.kuayue.overhead_line.block.support.AllOverheadLineSupportBlocks;
import cn.kycraft.kuayue.overhead_line.block.support.variants.NormalOverheadSupportBlock;
import cn.kycraft.kuayue.overhead_line.wire.Allwires;
import lib.kasuga.registration.Reg;
import lib.kasuga.registration.factory.FactoryRegistry;
import lib.kasuga.registration.minecraft.block.BlockReg;
import lib.kasuga.registration.minecraft.item.ItemReg;
import lib.kasuga.shader.ShaderIr;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class OverheadLineFactoies {
    public static final String OVERHEAD_LINE_WIRE = "wire";
    public static final String OVERHEAD_LINE_SUPPORT = "overhead_line_support";
    public static final String ELECTRONIC_WIRE  = "electronic_wire";
    public static final String SUPPORT = "support";

    private OverheadLineFactoies(){}

    private static boolean registered;
    public static synchronized final void register(){
        if (registered){return;}
        FactoryRegistry.registerItem(OVERHEAD_LINE_WIRE,(id,params) -> {
            ItemReg<Allwires> overhead_line_wire_reg = ItemReg.of(id,(Function<Item.Properties, Allwires>) Allwires :: new);
            return (Reg<ItemReg<?>, Item>)  (Reg<?,?>) overhead_line_wire_reg;
        });

        FactoryRegistry.registerItem(ELECTRONIC_WIRE , (id,params) -> {
            ItemReg<Allwires> electronic_wire_reg = ItemReg.of(id,(Function<Item.Properties, Allwires>) Allwires :: new);
            return (Reg<ItemReg<?>, Item>)  (Reg<?,?>) electronic_wire_reg;
        });

        FactoryRegistry.register(SUPPORT, (id,params) -> {
            BlockReg<NormalOverheadSupportBlock> supportBlocksBlockReg = BlockReg.of(id, (Function<BlockBehaviour.Properties, NormalOverheadSupportBlock>) NormalOverheadSupportBlock:: new);
            supportBlocksBlockReg.withBlockItem(id,() -> ((block, properties) -> new BlockItem(block,properties)));
            return (Reg<BlockReg<?>, Block>) (Reg<?, ?>) supportBlocksBlockReg;
        });
        registered = true;
    }
}
