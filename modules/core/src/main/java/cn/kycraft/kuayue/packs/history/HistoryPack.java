package cn.kycraft.kuayue.packs.history;

import cn.kycraft.kuayue.packs.SemanticVersion;

/**
 * Common identity shape for persisted history and the current startup snapshot.
 */
public interface HistoryPack {
    String id();

    SemanticVersion version();

    int formatVersion();
}
