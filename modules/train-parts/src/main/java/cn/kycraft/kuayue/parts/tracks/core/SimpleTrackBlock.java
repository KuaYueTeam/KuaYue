package cn.kycraft.kuayue.parts.tracks.core;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Supplier;

/**
 * Track block that resolves a configured default bogey when a new bogey anchor is requested.
 */
public class SimpleTrackBlock extends TrackBlock {

    private final Supplier<? extends AbstractBogeyBlock<?>> defaultBogeySupplier;

    /**
     * Creates a simple track block with a lazy default bogey supplier.
     *
     * @param properties            block properties
     * @param material              resolved Create track material
     * @param defaultBogeySupplier supplier used when a bogey anchor is created
     */
    public SimpleTrackBlock(BlockBehaviour.Properties properties, TrackMaterial material,
                            Supplier<? extends AbstractBogeyBlock<?>> defaultBogeySupplier) {
        super(properties, material);
        this.defaultBogeySupplier = defaultBogeySupplier;
    }

    /**
     * Creates the bogey block state for a newly placed bogey anchor.
     *
     * <p>If the configured bogey cannot be resolved, Create's small bogey is used as a fallback.</p>
     *
     * @param world level used for anchor creation
     * @param pos   anchor position
     * @param state current track block state
     * @return the default bogey block state with the track axis applied
     */
    @Override
    public BlockState getBogeyAnchor(BlockGetter world, BlockPos pos, BlockState state) {
        AbstractBogeyBlock<?> bogey = defaultBogeySupplier.get();
        if (bogey == null)
            return AllBlocks.SMALL_BOGEY.getDefaultState()
                    .setValue(BlockStateProperties.HORIZONTAL_AXIS,
                            state.getValue(SHAPE) == TrackShape.XO ? Direction.Axis.X : Direction.Axis.Z);
        return bogey.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_AXIS,
                        state.getValue(SHAPE) == TrackShape.XO ? Direction.Axis.X : Direction.Axis.Z);
    }
}
