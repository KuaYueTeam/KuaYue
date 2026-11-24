package cn.kycraft.kuayue.packs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure-JVM K1 entry point. Host bootstrapping supplies the embedded pack catalog. */
public final class PackDiscovery {
    private PackDiscovery() {
    }

    public static PackCatalog scan(Path packsDirectory, PackLimits limits) {
        return new PackScanner().scan(packsDirectory, limits);
    }

    public static PackDiscoveryResult discover(
            Path gameDirectory,
            PackRuntimeEnvironment environment,
            PackCatalog embeddedCatalog,
            PackLimits limits,
            VersionComparator modComparator
    ) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(embeddedCatalog, "embeddedCatalog");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(modComparator, "modComparator");
        Path configPath = gameDirectory.resolve("kuayue").resolve("config.json");
        Path packsDirectory = gameDirectory.resolve("kuayue").resolve("packs");

        PackConfigStore.LoadResult configResult = PackConfigStore.load(configPath, limits);
        PackCatalog externalCatalog = scan(packsDirectory, limits);
        PackCatalog catalog = PackCatalog.merge(embeddedCatalog, externalCatalog);
        List<PackDiagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(configResult.diagnostics());
        diagnostics.addAll(catalog.diagnostics());

        PackConfig completedConfig;
        PackSelection selection;
        if (configResult.hasBlockingErrors()) {
            completedConfig = configResult.document().config();
            selection = new PackSelection(List.of(), java.util.Set.of(), List.of());
        } else {
            Set<String> externalIds = installedIds(externalCatalog);
            Set<String> builtInIds = installedIds(embeddedCatalog);
            Set<String> requiredIds = catalog.packs().stream()
                    .filter(pack -> pack.manifest().required())
                    .map(DiscoveredPack::packId)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            PackConfig.ConfigCompletion completion =
                    configResult.document().config().complete(
                            externalIds,
                            builtInIds,
                            requiredIds
                    );
            completedConfig = completion.config();
            diagnostics.addAll(completion.diagnostics());
            selection = new PackDependencyResolver().resolve(
                    completedConfig, catalog, environment, modComparator);
            diagnostics.addAll(selection.diagnostics());
        }

        PackConfigDocument completedDocument =
                configResult.document().withConfig(completedConfig);
        PackStartupReport report = PackStartupReport.create(
                environment, completedConfig, catalog, selection, diagnostics);
        return new PackDiscoveryResult(completedDocument, catalog, selection, report);
    }

    public static PackDiscoveryResult discover(
            Path gameDirectory,
            PackRuntimeEnvironment environment
    ) {
        return discover(
                gameDirectory,
                environment,
                new PackCatalog(List.of(), List.of(), List.of()),
                PackLimits.defaults(),
                VersionComparator.semantic()
        );
    }

    public static PackDiscoveryResult discover(
            Path gameDirectory,
            PackRuntimeEnvironment environment,
            PackCatalog embeddedCatalog
    ) {
        return discover(
                gameDirectory,
                environment,
                embeddedCatalog,
                PackLimits.defaults(),
                VersionComparator.semantic()
        );
    }

    private static java.util.Set<String> installedIds(PackCatalog catalog) {
        return catalog.installations().stream()
                .map(PackInstallation::packId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
