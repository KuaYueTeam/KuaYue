package cn.kycraft.kuayue.parts.bogeys.core;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class CommonBogeyBlock extends AbstractBogeyBlock<CommonBogeyBlockEntity>
    implements IBE<CommonBogeyBlockEntity>, ProperWaterloggedBlock {

    static Supplier<BlockEntityType<? extends CommonBogeyBlockEntity>> GLOBAL_BE_TYPE = () -> null;

    private final BogeyStyle defaultStyle;
    private final TrackMaterial.TrackType primaryType;
    private final Set<TrackMaterial.TrackType> validTypes;
    private final double wheelPointSpacing;
    private final Vec3 connectorAnchorOffset;
    private final boolean allowSingleBogeyCarriage;

    public static void setGlobalBlockEntityType(Supplier<BlockEntityType<? extends CommonBogeyBlockEntity>> supplier) {
        GLOBAL_BE_TYPE = supplier;
    }

    public CommonBogeyBlock(Properties props, BogeySizes.BogeySize size, BogeyStyle defaultStyle,
                            TrackMaterial.TrackType primaryType, Set<TrackMaterial.TrackType> validTypes,
                            double wheelPointSpacing, Vec3 connectorAnchorOffset) {
        this(props, size, defaultStyle, primaryType, validTypes, wheelPointSpacing, connectorAnchorOffset, true);
    }

    public CommonBogeyBlock(Properties props, BogeySizes.BogeySize size, BogeyStyle defaultStyle,
                            TrackMaterial.TrackType primaryType, Set<TrackMaterial.TrackType> validTypes,
                            double wheelPointSpacing, Vec3 connectorAnchorOffset,
                            boolean allowSingleBogeyCarriage) {
        super(props, size);
        this.defaultStyle = defaultStyle;
        this.primaryType = primaryType;
        this.validTypes = validTypes;
        this.wheelPointSpacing = wheelPointSpacing;
        this.connectorAnchorOffset = connectorAnchorOffset;
        this.allowSingleBogeyCarriage = allowSingleBogeyCarriage;
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    public TrackMaterial.TrackType getTrackType(BogeyStyle style) {
        return primaryType;
    }

    @Override
    public Set<TrackMaterial.TrackType> getValidPathfindingTypes(BogeyStyle style) {
        return validTypes;
    }

    @Override
    public boolean isOnIncompatibleTrack(Carriage carriage, boolean leading) {
        TravellingPoint point = leading ? carriage.getLeadingPoint() : carriage.getTrailingPoint();
        CarriageBogey bogey = leading ? carriage.leadingBogey() : carriage.trailingBogey();
        TrackEdge currentEdge = point.edge;
        if (currentEdge == null) return false;
        return !getValidPathfindingTypes(bogey.getStyle()).contains(currentEdge.getTrackMaterial().trackType);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public double getWheelPointSpacing() {
        return wheelPointSpacing;
    }

    @Override
    public double getWheelRadius() {
        return size.wheelRadius();
    }

    @Override
    public Vec3 getConnectorAnchorOffset() {
        return connectorAnchorOffset;
    }

    @Override
    public boolean allowsSingleBogeyCarriage() {
        return allowSingleBogeyCarriage;
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        return defaultStyle;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
                                       net.minecraft.world.level.LevelReader level, BlockPos pos,
                                       net.minecraft.world.entity.player.Player player) {
        return AllBlocks.RAILWAY_CASING.asStack();
    }

    @Override
    public Class<CommonBogeyBlockEntity> getBlockEntityClass() {
        return CommonBogeyBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CommonBogeyBlockEntity> getBlockEntityType() {
        return GLOBAL_BE_TYPE.get();
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        return state;
    }

    private final List<Property<?>> properties_to_copy = ImmutableList.<Property<?>>builder()
            .addAll(super.propertiesToCopy()).build();

    @Override
    public List<Property<?>> propertiesToCopy() {
        return properties_to_copy;
    }
}
