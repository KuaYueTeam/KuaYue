package cn.kycraft.kuayue.packs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record PackSelection(
        List<ResolvedPack> effectivePacks,
        Set<String> blockedPackIds,
        List<PackDiagnostic> diagnostics
) implements Serializable {
    public PackSelection {
        effectivePacks = List.copyOf(new ArrayList<>(effectivePacks));
        blockedPackIds = Set.copyOf(new LinkedHashSet<>(blockedPackIds));
        diagnostics = List.copyOf(new ArrayList<>(diagnostics))
                .stream().sorted().toList();
    }
}
