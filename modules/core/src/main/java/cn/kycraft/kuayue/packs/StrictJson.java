package cn.kycraft.kuayue.packs;

/**
 * Strict JSON support used by all K1 protocol files and reports.
 */
public final class StrictJson {
    private StrictJson() {
    }

    public static void validate(String json) {
        StrictJsonEngine.parse(json);
    }
}
