package cn.kycraft.kuayue.provisions.init;

/**
 * 本模块用到的 data-driven 工厂类型名。
 *
 * <p>Kept as plain constants so {@code provisions.json} and the factory registration can never
 * drift apart.</p>
 */
public final class ProvisionsFactories {

    public static final String SIMPLE_ITEM = "simple_item";
    public static final String DIET_BOX = "diet_box";
    public static final String DRINK = "drink";
    public static final String SOAKED_NOODLES = "soaked_noodles";
    public static final String PLACEMENT_FOOD = "placement_food";

    private ProvisionsFactories() {
    }
}
