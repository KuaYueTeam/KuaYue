package cn.kycraft.kuayue.parts.core.multiblock;

import cn.kycraft.kuayue.utils.DirectionUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonical north-facing cell offsets occupied by a multi-cell block.
 *
 * <p>The origin is the anchor block and must not appear in any cell list. A footprint may declare a
 * default {@code cells} list and optional {@code cells_by_state} rules. A companion returns an empty
 * shape when the current parent state does not reach its cell.
 */
public record MultiCellFootprint(List<BlockPos> cells, List<StateRule> stateRules) {
    public static final int MAX_CELLS = 64;
    public static final int MAX_HORIZONTAL_DISTANCE = 16;
    public static final int MAX_VERTICAL_DISTANCE = 16;
    public static final MultiCellFootprint EMPTY = new MultiCellFootprint(List.of(), List.of());

    public MultiCellFootprint {
        cells = List.copyOf(cells);
        stateRules = List.copyOf(stateRules);
    }

    public record StateRule(String property, Map<String, List<BlockPos>> values) {
        public StateRule {
            values = values.entrySet().stream().collect(
                    LinkedHashMap::new,
                    (copy, entry) -> copy.put(entry.getKey(), List.copyOf(entry.getValue())),
                    LinkedHashMap::putAll
            );
            values = Map.copyOf(values);
        }
    }

    public static MultiCellFootprint parse(@Nullable JsonObject params) {
        if (params == null || !params.has("multi_cell")) {
            return EMPTY;
        }
        if (!params.get("multi_cell").isJsonObject()) {
            throw new IllegalArgumentException("params.multi_cell must be an object");
        }
        JsonObject multiCell = params.getAsJsonObject("multi_cell");
        List<BlockPos> defaultCells = multiCell.has("cells")
                ? parseCells(multiCell.get("cells"), "params.multi_cell.cells")
                : List.of();
        List<StateRule> stateRules = multiCell.has("cells_by_state")
                ? parseStateRules(multiCell.get("cells_by_state"))
                : List.of();
        if (defaultCells.isEmpty() && stateRules.isEmpty()) {
            throw new IllegalArgumentException(
                    "params.multi_cell requires cells or cells_by_state");
        }
        return new MultiCellFootprint(defaultCells, stateRules);
    }

    public boolean isEmpty() {
        return cells.isEmpty() && stateRules.isEmpty();
    }

    public void validate(BlockState state) {
        for (StateRule rule : stateRules) {
            Property<?> property = state.getProperties().stream()
                    .filter(candidate -> candidate.getName().equals(rule.property()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "params.multi_cell.cells_by_state references unknown property '"
                                    + rule.property() + "'"));
            validateRule(state, property, rule, cells.isEmpty());
        }
    }

    private static <T extends Comparable<T>> void validateRule(
            BlockState state,
            Property<T> property,
            StateRule rule,
            boolean requiresCompleteCoverage
    ) {
        Set<String> possibleValues = new HashSet<>();
        for (T possibleValue : property.getPossibleValues()) {
            possibleValues.add(property.getName(possibleValue));
        }
        for (String configuredValue : rule.values().keySet()) {
            if (!possibleValues.contains(configuredValue)) {
                throw new IllegalArgumentException(
                        "params.multi_cell.cells_by_state." + rule.property()
                                + " references unknown value '" + configuredValue + "'");
            }
        }
        if (requiresCompleteCoverage) {
            for (String possibleValue : possibleValues) {
                if (!rule.values().containsKey(possibleValue)) {
                    throw new IllegalArgumentException(
                            "params.multi_cell.cells_by_state." + rule.property()
                                    + " must define value '" + possibleValue
                                    + "' when multi_cell.cells is absent");
                }
            }
        }
    }

    private List<BlockPos> selectedCells(BlockState state) {
        for (StateRule rule : stateRules) {
            for (var property : state.getProperties()) {
                if (!property.getName().equals(rule.property())) {
                    continue;
                }
                String value = valueName(state, property);
                List<BlockPos> selected = rule.values().get(value);
                if (selected != null) {
                    return selected;
                }
            }
        }
        return cells;
    }

    public List<BlockPos> relativeCells(BlockState state) {
        List<BlockPos> selected = selectedCells(state);
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return selected;
        }
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        List<BlockPos> rotated = new ArrayList<>(selected.size());
        for (BlockPos cell : selected) {
            rotated.add(DirectionUtil.rotate(cell, Direction.NORTH, facing));
        }
        return List.copyOf(rotated);
    }

    public Set<BlockPos> allRelativeCells() {
        Set<BlockPos> canonical = new HashSet<>();
        canonical.addAll(cells);
        for (StateRule rule : stateRules) {
            for (List<BlockPos> values : rule.values().values()) {
                canonical.addAll(values);
            }
        }
        Set<BlockPos> all = new HashSet<>(canonical.size() * 4);
        for (Direction facing : new Direction[]{
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        }) {
            for (BlockPos cell : canonical) {
                all.add(DirectionUtil.rotate(cell, Direction.NORTH, facing));
            }
        }
        return Set.copyOf(all);
    }

    private static <T extends Comparable<T>> String valueName(
            BlockState state,
            Property<T> property
    ) {
        return property.getName(state.getValue(property));
    }

    private static List<StateRule> parseStateRules(JsonElement value) {
        if (!value.isJsonObject()) {
            throw new IllegalArgumentException("params.multi_cell.cells_by_state must be an object");
        }
        List<StateRule> rules = new ArrayList<>();
        for (var propertyEntry : value.getAsJsonObject().entrySet()) {
            if (!propertyEntry.getValue().isJsonObject()) {
                throw new IllegalArgumentException(
                        "params.multi_cell.cells_by_state."
                                + propertyEntry.getKey() + " must be an object");
            }
            Map<String, List<BlockPos>> values = new LinkedHashMap<>();
            for (var stateEntry : propertyEntry.getValue().getAsJsonObject().entrySet()) {
                values.put(
                        stateEntry.getKey(),
                        parseCells(
                                stateEntry.getValue(),
                                "params.multi_cell.cells_by_state."
                                        + propertyEntry.getKey() + "." + stateEntry.getKey()
                        )
                );
            }
            if (values.isEmpty()) {
                throw new IllegalArgumentException(
                        "params.multi_cell.cells_by_state."
                                + propertyEntry.getKey() + " must not be empty");
            }
            rules.add(new StateRule(propertyEntry.getKey(), values));
        }
        return List.copyOf(rules);
    }

    private static List<BlockPos> parseCells(JsonElement value, String path) {
        if (!value.isJsonArray()) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        JsonArray values = value.getAsJsonArray();
        if (values.isEmpty() || values.size() > MAX_CELLS) {
            throw new IllegalArgumentException(
                    path + " must contain between 1 and " + MAX_CELLS + " offsets");
        }
        List<BlockPos> parsed = new ArrayList<>(values.size());
        Set<BlockPos> unique = new HashSet<>();
        for (JsonElement cellValue : values) {
            BlockPos offset = parseOffset(cellValue);
            if (BlockPos.ZERO.equals(offset)) {
                throw new IllegalArgumentException(
                        path + " must not contain the anchor [0,0,0]");
            }
            if (Math.abs(offset.getX()) > MAX_HORIZONTAL_DISTANCE
                    || Math.abs(offset.getZ()) > MAX_HORIZONTAL_DISTANCE
                    || Math.abs(offset.getY()) > MAX_VERTICAL_DISTANCE) {
                throw new IllegalArgumentException(
                        path + " offset is outside the supported local range");
            }
            if (!unique.add(offset)) {
                throw new IllegalArgumentException(
                        path + " contains duplicate offset " + offset.toShortString());
            }
            parsed.add(offset);
        }
        return List.copyOf(parsed);
    }

    private static BlockPos parseOffset(JsonElement value) {
        if (!value.isJsonArray() || value.getAsJsonArray().size() != 3) {
            throw new IllegalArgumentException(
                    "each params.multi_cell.cells entry must be [x,y,z]");
        }
        JsonArray tuple = value.getAsJsonArray();
        return new BlockPos(integer(tuple.get(0)), integer(tuple.get(1)), integer(tuple.get(2)));
    }

    private static int integer(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("multi_cell.cells values must be integers");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number != Math.rint(number)
                || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("multi_cell.cells values must be integers");
        }
        return (int) number;
    }
}
