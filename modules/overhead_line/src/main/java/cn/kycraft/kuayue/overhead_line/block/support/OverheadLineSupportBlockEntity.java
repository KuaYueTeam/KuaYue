//package cn.kycraft.kuayue.overhead_line.block.support;
//
//import cn.kycraft.kuayue.overhead_line.block.support.variants.OverheadLineBlockDynamicConfiguration;
//import cn.kycraft.kuayue.overhead_line.types.OverheadLineType;
//import com.simibubi.create.content.equipment.clipboard.ClipboardValueSettingsHandler;
//import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.MenuProvider;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.entity.BlockEntityType;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.phys.Vec3;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.api.distmarker.OnlyIn;
//import org.joml.Vector3f;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.function.Supplier;
//
//public class OverheadLineSupportBlockEntity extends SmartBlockEntity implements MenuProvider, ClipboardValueSettingsHandler {
//    public record Connection(
//            BlockPos absolutePos,
//            BlockPos relativePos,
//            OverheadLineType type,
//            int connectionIndex,
//            int targetIndex,
//            Vector3f toPosition) {
//        public Connection(
//                BlockPos absolutePos,
//                BlockPos relativePos,
//                OverheadLineType type,
//                int connectionIndex,
//                int targetIndex,
//                Vector3f toPosition
//        ) {
//            this.absolutePos = absolutePos;
//            this.relativePos = relativePos;
//            this.type = type;
//            this.connectionIndex = connectionIndex;
//            this.targetIndex = targetIndex;
//            this.toPosition = toPosition;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj) {
//                return true;
//            }
//            if (!(obj instanceof Connection that)) {
//                return false;
//            }
//            return connectionIndex == that.connectionIndex && Objects.equals(absolutePos, that.absolutePos) && Objects.equals(relativePos, that.relativePos) && Objects.equals(type, that.type);
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(absolutePos, relativePos, type, connectionIndex);
//        }
//
//        public Connection withBlockEntityPosition(BlockPos bePosition, Vector3f toPosition) {
//            return new Connection(
//                    bePosition.offset(relativePos),
//                    relativePos,
//                    type,
//                    connectionIndex,
//                    targetIndex,
//                    toPosition
//            );
//        }
//    }
//            public static final HashMap<Supplier<Block>, OverheadLineBlockDynamicConfiguration> map = new HashMap<>();
//
//            private static final HashMap<Block, OverheadLineBlockDynamicConfiguration> CONNECTION_POINTS_SUPPLIERS = new HashMap<>();
//            public static void registerPoint(Supplier<Block> block,OverheadLineBlockDynamicConfiguration configuration){
//                map.put(block, configuration);
//            }
//
//            public static void applyRegisteration(){
//                for (Map.Entry<Supplier<Block>,OverheadLineBlockDynamicConfiguration> entry : map.entrySet()){
//                    Supplier<Block> block = entry.getKey();
//                    OverheadLineBlockDynamicConfiguration configuration = entry.getValue();
//                    CONNECTION_POINTS_SUPPLIERS.put(block.get(),configuration);
//                }
//            }
//
//            protected final OverheadLineBlockDynamicConfiguration configuration;
//
//            public OverheadLineSupportBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state){
//                super(type,pos,state);
//                configuration = CONNECTION_POINTS_SUPPLIERS.get(this.getBlockState().getBlock());
//            }
//
//            protected List<Vec3> connectionPoints = List.of();
//
//            public void initialize(){
//                super.initialize();
//                this.connectionPoints = configuration.connectionPoints().get(this.level,this.getBlockPos(),this.getBlockState());
//            }
//
//            public Vec3 getConnectionPointByIndex(int index){
//                return getConnectionPointByIndex(index);
//            }
//
////            @OnlyIn(Dist.CLIENT)
////            private void clearBlockEntityCache(){
////                if (OverheadLineRen)
////            }
////
////            public void onConnectionModification(){
////                if (this.level == null){
////                    return;
////                }
////
////                if (this.level.isClientSide){
////
////                }
////            }
//}
