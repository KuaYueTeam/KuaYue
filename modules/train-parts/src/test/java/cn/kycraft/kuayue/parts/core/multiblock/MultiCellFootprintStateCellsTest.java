package cn.kycraft.kuayue.parts.core.multiblock;

import com.google.gson.JsonParser;
import cn.kycraft.kuayue.parts.core.factory.ShapeBindingTestFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultiCellFootprintStateCellsTest {
    @Test
    void exactStateCellsSelectOnlyTheCurrentOccupiedNeighbor() {
        MultiCellFootprint footprint = MultiCellFootprint.parse(JsonParser.parseString("""
                {
                  "multi_cell": {
                    "state_cells": [
                      {
                        "when": {"facing": "north", "open": "false"},
                        "cells": [[0, 0, 1]]
                      },
                      {
                        "when": {"facing": "north", "open": "true"},
                        "cells": [[0, 0, -1]]
                      },
                      {
                        "when": {"facing": "east", "open": "true"},
                        "cells": [[0, 0, -1]]
                      }
                    ]
                  }
                }
                """).getAsJsonObject());
        Block block = ShapeBindingTestFixtures.multiCellBlock;
        BlockState northClosed = block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.OPEN, false);
        BlockState northOpen = northClosed.setValue(BlockStateProperties.OPEN, true);
        BlockState eastOpen = northOpen
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(ShapeBindingTestFixtures.ACTIVE, true);

        footprint.validate(northClosed);
        assertEquals(List.of(new BlockPos(0, 0, 1)), footprint.relativeCells(northClosed));
        assertEquals(List.of(new BlockPos(0, 0, -1)), footprint.relativeCells(northOpen));
        assertEquals(List.of(new BlockPos(1, 0, 0)), footprint.relativeCells(eastOpen));
    }

    @Test
    void equallySpecificOverlappingRulesAreRejected() {
        MultiCellFootprint footprint = MultiCellFootprint.parse(JsonParser.parseString("""
                {
                  "multi_cell": {
                    "state_cells": [
                      {"when": {"open": "true"}, "cells": []},
                      {"when": {"facing": "north"}, "cells": [[0, 0, 1]]}
                    ]
                  }
                }
                """).getAsJsonObject());
        BlockState ambiguous = ShapeBindingTestFixtures.multiCellBlock.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.OPEN, true);

        assertThrows(IllegalArgumentException.class, () -> footprint.validate(ambiguous));
    }

}
