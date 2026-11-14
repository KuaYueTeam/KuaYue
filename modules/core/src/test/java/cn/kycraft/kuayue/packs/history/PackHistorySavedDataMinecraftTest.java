package cn.kycraft.kuayue.packs.history;

import cn.kycraft.kuayue.packs.SemanticVersion;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PackHistorySavedDataMinecraftTest {
    private static final String HISTORY_CLASS =
            "cn.kycraft.kuayue.packs.history.PackHistorySavedData";
    private static final String ENTRY_CLASS =
            "cn.kycraft.kuayue.packs.history.PackHistoryEntry";

    @Test
    void minecraftNbtRoundTripsInitializedEmptyHistory() throws Exception {
        MinecraftApi minecraft = minecraftApi();
        Class<?> historyClass = Class.forName(HISTORY_CLASS);
        Object history = historyClass.getMethod("initialized", Collection.class)
                .invoke(null, List.of());

        Object scratch = minecraft.newCompound();
        minecraft.invoke(scratch, "putString", "enabled", "leak");
        minecraft.invoke(scratch, "putString", "order", "leak");
        minecraft.invoke(scratch, "putString", "hash", "leak");
        Method save = historyClass.getMethod(
                "save", minecraft.compoundTag(), minecraft.provider());
        Object payload = save.invoke(history, scratch, minecraft.emptyRegistries());
        assertEquals(Set.of("schema_version", "initialized", "packs"),
                invoke(payload, "getAllKeys"));
        assertFalse(invokeBoolean(payload, "contains", "enabled"));
        assertFalse(invokeBoolean(payload, "contains", "order"));
        assertFalse(invokeBoolean(payload, "contains", "hash"));

        Object loaded = invokeLoad(
                minecraft, historyClass, minecraft.invoke(payload, "copy"));
        assertTrue((boolean) invoke(loaded, "initialized"));
        assertEquals(List.of(), invoke(loaded, "packs"));
    }

    @Test
    void minecraftNbtRoundTripsPackIdentityAndMarksRealChangesDirty() throws Exception {
        MinecraftApi minecraft = minecraftApi();
        Class<?> semanticVersion = Class.forName("cn.kycraft.kuayue.packs.SemanticVersion");
        Class<?> entryClass = Class.forName(ENTRY_CLASS);
        Object version = semanticVersion.getMethod("parse", String.class)
                .invoke(null, "0.2.0");
        Object entry = entryClass
                .getConstructor(String.class, semanticVersion, int.class)
                .newInstance("example-railway", version, 1);
        Class<?> historyClass = Class.forName(HISTORY_CLASS);
        Object history = historyClass.getMethod("initialized", Collection.class)
                .invoke(null, List.of(entry));

        Object payload = invokeSave(minecraft, historyClass, history);
        Object packs = minecraft.invoke(payload, "getList", "packs", 10);
        Object first = minecraft.invoke(packs, "getCompound", 0);
        assertEquals(Set.of("id", "version", "format_version"),
                minecraft.invoke(first, "getAllKeys"));

        Object loaded = invokeLoad(
                minecraft, historyClass, minecraft.invoke(payload, "copy"));
        assertEquals(List.of(entry), minecraft.invoke(loaded, "packs"));

        Object changed = historyClass.getMethod("initialize", Collection.class)
                .invoke(loaded, List.of());
        assertTrue((boolean) changed);
        assertTrue((boolean) minecraft.invoke(loaded, "isDirty"));
    }

    @Test
    void minecraftLoaderRejectsCorruptAndUnsupportedPayloads() throws Exception {
        MinecraftApi minecraft = minecraftApi();
        Class<?> historyClass = Class.forName(HISTORY_CLASS);

        Object missingInitialized = minecraft.newCompound();
        minecraft.invoke(missingInitialized, "putInt", "schema_version", 1);
        minecraft.invoke(
                missingInitialized,
                "put",
                "packs",
                minecraft.compoundList().getConstructor().newInstance());
        assertLoadReason(
                minecraft,
                historyClass,
                missingInitialized,
                PackHistoryLoadException.Reason.CORRUPT);

        Object unsupported = minecraft.newCompound();
        minecraft.invoke(unsupported, "putInt", "schema_version", 2);
        minecraft.invoke(unsupported, "putBoolean", "initialized", true);
        minecraft.invoke(
                unsupported,
                "put",
                "packs",
                minecraft.compoundList().getConstructor().newInstance());
        assertLoadReason(
                minecraft,
                historyClass,
                unsupported,
                PackHistoryLoadException.Reason.UNSUPPORTED_SCHEMA);
    }

    private static MinecraftApi minecraftApi() throws ClassNotFoundException {
        try {
            Class<?> compoundTag = Class.forName("net.minecraft.nbt.CompoundTag");
            Class<?> listTag = Class.forName("net.minecraft.nbt.ListTag");
            Class<?> provider = Class.forName("net.minecraft.core.HolderLookup$Provider");
            Object emptyRegistries = Class.forName("net.minecraft.core.RegistryAccess")
                    .getField("EMPTY")
                    .get(null);
            return new MinecraftApi(compoundTag, listTag, provider, emptyRegistries);
        } catch (ClassNotFoundException exception) {
            assumeTrue(false, "Minecraft API is unavailable on the unit-test classpath");
            throw exception;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot initialize Minecraft NBT test API", exception);
        }
    }

    private static Object invokeSave(
            MinecraftApi minecraft,
            Class<?> historyClass,
            Object history
    ) throws Exception {
        Method save = historyClass.getMethod(
                "save", minecraft.compoundTag(), minecraft.provider());
        return save.invoke(history, minecraft.newCompound(), minecraft.emptyRegistries());
    }

    private static Object invokeLoad(
            MinecraftApi minecraft,
            Class<?> historyClass,
            Object payload
    ) throws Exception {
        Method load = historyClass.getMethod(
                "load", minecraft.compoundTag(), minecraft.provider());
        return load.invoke(null, payload, minecraft.emptyRegistries());
    }

    private static void assertLoadReason(
            MinecraftApi minecraft,
            Class<?> historyClass,
            Object payload,
            PackHistoryLoadException.Reason reason
    ) throws Exception {
        InvocationTargetException thrown = assertThrows(
                InvocationTargetException.class,
                () -> invokeLoad(minecraft, historyClass, payload));
        assertTrue(thrown.getCause() instanceof PackHistoryLoadException);
        assertEquals(reason, ((PackHistoryLoadException) thrown.getCause()).reason());
    }

    private static Object invoke(Object target, String method, Object... args) throws Exception {
        for (Method candidate : target.getClass().getMethods()) {
            if (candidate.getName().equals(method)
                    && candidate.getParameterCount() == args.length) {
                return candidate.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + method);
    }

    @SuppressWarnings("unchecked")
    private static boolean invokeBoolean(Object target, String method, Object... args)
            throws Exception {
        return (boolean) invoke(target, method, args);
    }

    private record MinecraftApi(
            Class<?> compoundTag,
            Class<?> listTag,
            Class<?> provider,
            Object emptyRegistries
    ) {
        private Object newCompound() throws ReflectiveOperationException {
            return compoundTag.getConstructor().newInstance();
        }

        private Class<?> compoundList() {
            return listTag;
        }

        private Object invoke(Object target, String method, Object... args) throws Exception {
            return PackHistorySavedDataMinecraftTest.invoke(target, method, args);
        }
    }
}
