package cn.kycraft.kuayue.packs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure-JVM K1 entry point. The caller supplies host versions and the optional
 * corepack descriptor; no Minecraft or NeoForge bootstrap is required.
 */
public final class PackDiscovery {
    private PackDiscovery() {
    }

    public static PackCatalog scan(Path packsDirectory, PackLimits limits) {
        return new PackScanner().scan(packsDirectory, limits);
    }

    public static DiscoveredPack scanCoreRoot(Path root, PackLimits limits) {
        return new PackScanner().scanCoreRoot(root, limits);
    }

    public static PackDiscoveryResult discover(
            Path gameDirectory,
            PackRuntimeEnvironment environment,
            CorePackDescriptor corepack,
            PackLimits limits,
            VersionComparator modComparator
    ) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(modComparator, "modComparator");
        CorePackDescriptor actualCorepack = corepack == null
                ? CorePackDescriptor.forVersion(environment.version("kuayue"))
                : corepack;
        Path configPath = gameDirectory.resolve("kuayue").resolve("config.json");
        Path packsDirectory = gameDirectory.resolve("kuayue").resolve("packs");

        PackConfigStore.LoadResult configResult = PackConfigStore.load(configPath, limits);
        PackCatalog catalog = scan(packsDirectory, limits);
        List<PackDiagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(configResult.diagnostics());
        diagnostics.addAll(catalog.diagnostics());

        PackConfig completedConfig;
        PackSelection selection;
        if (configResult.hasBlockingErrors()) {
            completedConfig = configResult.document().config();
            selection = new PackSelection(List.of(), java.util.Set.of(), List.of());
        } else {
            PackConfig.ConfigCompletion completion =
                    configResult.document().config().complete(installedIds(catalog));
            completedConfig = completion.config();
            diagnostics.addAll(completion.diagnostics());
            selection = new PackDependencyResolver().resolve(
                    completedConfig, catalog, actualCorepack, environment, modComparator);
            diagnostics.addAll(selection.diagnostics());
        }

        PackConfigDocument completedDocument =
                configResult.document().withConfig(completedConfig);
        PackStartupReport report = PackStartupReport.create(
                environment, completedConfig, actualCorepack, catalog, selection, diagnostics);
        return new PackDiscoveryResult(completedDocument, catalog, selection, report);
    }

    public static PackDiscoveryResult discover(
            Path gameDirectory,
            PackRuntimeEnvironment environment
    ) {
        return discover(
                gameDirectory,
                environment,
                null,
                PackLimits.defaults(),
                VersionComparator.semantic());
    }

    public static PackDiscoveryResult discover(
            Path gameDirectory,
            PackRuntimeEnvironment environment,
            CorePackDescriptor corepack
    ) {
        return discover(
                gameDirectory,
                environment,
                corepack,
                PackLimits.defaults(),
                VersionComparator.semantic());
    }

    private static java.util.Set<String> installedIds(PackCatalog catalog) {
        return catalog.packs().stream()
                .map(DiscoveredPack::packId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
