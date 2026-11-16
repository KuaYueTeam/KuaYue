package cn.kycraft.kuayue.packs.history;

import cn.kycraft.kuayue.packs.PackManifest;
import cn.kycraft.kuayue.packs.SemanticVersion;
import cn.kycraft.kuayue.packs.bootstrap.KuaYuePackBootstrap;
import cn.kycraft.kuayue.packs.bootstrap.StartupPackSnapshot;
import cn.kycraft.kuayue.packs.state.PackRuntimeState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Client-side read-only preflight for the vanilla-style single-player confirmation. */
public final class PackHistoryPreflight {
    private static final long MAX_RECORD_BYTES = 1L << 20;
    private static final long MAX_NBT_BYTES = 4L << 20;
    public enum Status {
        ABSENT,
        COMPATIBLE,
        CONFIRMATION_REQUIRED,
        ERROR
    }

    public record Result(Status status, List<String> messages) {
        public Result {
            messages = List.copyOf(messages);
        }
    }

    private PackHistoryPreflight() {
    }

    public static Result inspect(Path worldRoot) {
        Path dataDirectory = worldRoot.resolve("data");
        Path file = dataDirectory.resolve(PackHistorySavedData.DATA_NAME + ".dat");
        try {
            BasicFileAttributes dataAttributes = Files.readAttributes(
                    dataDirectory,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );
            if (dataAttributes.isSymbolicLink() || !dataAttributes.isDirectory()) {
                return new Result(
                        Status.ERROR,
                        List.of("KuaYue world data path is not a real directory")
                );
            }
            BasicFileAttributes attributes = Files.readAttributes(
                    file,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );
            if (!attributes.isRegularFile()) {
                return new Result(
                        Status.ERROR,
                        List.of("KuaYue world pack record is not a regular file")
                );
            }
        } catch (NoSuchFileException ignored) {
            return new Result(Status.ABSENT, List.of());
        } catch (IOException exception) {
            return new Result(
                    Status.ERROR,
                    List.of("Unable to inspect KuaYue world pack record: "
                            + exception.getMessage())
            );
        }
        try {
            PackHistorySavedData history = readSavedData(file);
            PackHistoryCompatibility.Result comparison =
                    PackHistoryCompatibility.compare(history, currentPacks());
            if (comparison.outcome() == PackHistoryCompatibility.Outcome.INCOMPATIBLE) {
                return new Result(
                        Status.CONFIRMATION_REQUIRED,
                        comparison.diagnostics().stream()
                                .map(diagnostic -> diagnostic.message())
                                .toList()
                );
            }
            return new Result(Status.COMPATIBLE, List.of());
        } catch (IOException | RuntimeException failure) {
            return new Result(
                    Status.ERROR,
                    List.of("Unable to read KuaYue world pack record: " + failure.getMessage())
            );
        }
    }

    static PackHistorySavedData readSavedData(Path file) throws IOException {
        if (Files.size(file) > MAX_RECORD_BYTES) {
            throw new IOException("KuaYue world pack record exceeds the supported size");
        }
        try (var input = Files.newInputStream(file);
             var probe = new PushbackInputStream(input, 2)) {
            int first = probe.read();
            int second = probe.read();
            if (second >= 0) probe.unread(second);
            if (first >= 0) probe.unread(first);
            CompoundTag root;
            NbtAccounter accounter = NbtAccounter.create(MAX_NBT_BYTES);
            if (first == 0x1f && second == 0x8b) {
                root = NbtIo.readCompressed(probe, accounter);
            } else {
                root = NbtIo.read(new DataInputStream(probe), accounter);
            }
            if (!root.contains("data", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                throw new PackHistoryLoadException(
                        PackHistoryLoadException.Reason.CORRUPT,
                        "KuaYue world pack record is missing its data envelope"
                );
            }
            return PackHistorySavedData.loadPreflight(root.getCompound("data"));
        }
    }

    private static List<PackHistoryCompatibility.CurrentPack> currentPacks() {
        if (!PackRuntimeState.isRegistrationSelectionFinalized()) {
            throw new IllegalStateException("Pack registration selection is not finalized");
        }
        StartupPackSnapshot snapshot = KuaYuePackBootstrap.snapshot()
                .orElseThrow(() -> new IllegalStateException("Pack snapshot is not initialized"));
        Set<String> active = PackRuntimeState.registeredPackIds();
        List<PackHistoryCompatibility.CurrentPack> packs = new ArrayList<>();
        for (String id : active) {
            PackManifest manifest = snapshot.knownManifests().get(id);
            if (manifest == null) {
                throw new IllegalStateException("No manifest for active pack " + id);
            }
            packs.add(PackHistoryCompatibility.CurrentPack.from(manifest));
        }
        return packs;
    }
}
