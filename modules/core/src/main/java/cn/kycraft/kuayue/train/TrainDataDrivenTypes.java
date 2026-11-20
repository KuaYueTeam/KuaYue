package cn.kycraft.kuayue.train;

/**
 * Constants shared by KuaYue's train data-driven domain.
 */
public final class TrainDataDrivenTypes {
    public static final String HOST_MOD_ID = "kuayue";
    public static final String TRAINS = "trains";
    public static final String ID = "id";

    public static final String MEMBERS = "members";
    public static final String CATEGORY = "category";
    public static final String NAME_KEY = "name_key";
    public static final String DESCRIPTION_KEY = "description_key";
    public static final String CREATIVE = "creative";
    public static final String TAB = "tab";
    public static final String ICON_ITEM = "icon_item";
    public static final String BLOCKS = "blocks";
    public static final String ITEMS = "items";
    public static final String ITEM_TAGS = "item_tags";
    public static final String BOGEY_SIZES = "bogey_sizes";
    public static final String BOGEY_MOTIONS = "bogey_motions";
    public static final String BOGEY_MODELS = "bogey_models";
    public static final String BOGEY_BLOCKS = "bogey_blocks";
    public static final String BOGEY_STYLES = "bogey_styles";
    public static final String BOGEY_STYLE_VARIANTS = "bogey_style_variants";

    public static final int PHASE = 60;

    /**
     * Returns whether a member name references a Kasuga definition that the train handler must
     * validate.
     */
    public static boolean isHandlerReferenceField(String field) {
        return BLOCKS.equals(field) || ITEMS.equals(field) || field.startsWith("bogey_");
    }

    /**
     * Returns whether a member name contains namespaced resource locations.
     */
    public static boolean isResourceListField(String field) {
        return isHandlerReferenceField(field) || ITEM_TAGS.equals(field);
    }

    private TrainDataDrivenTypes() {
    }
}
