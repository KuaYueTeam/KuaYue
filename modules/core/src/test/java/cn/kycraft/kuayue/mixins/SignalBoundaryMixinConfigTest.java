package cn.kycraft.kuayue.mixins;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SignalBoundaryMixin} 的接线检查：以前类写了但没写进 {@code kuayue.mixins.json}，
 * 编译能过、运行不加载。这里把「清单里有它、目标类型/目标方法对」钉住。
 *
 * <p>测试类路径里没有 Minecraft 运行时，所以不对 Create 的类做反射加载，直接检查编译产物：
 * mixin 类字节码里必须出现 {@code @Mixin} 的目标类型与 {@code @WrapMethod} 的目标方法名。</p>
 */
class SignalBoundaryMixinConfigTest {

    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/kuayue.mixins.json");
    private static final Path MIXIN_CLASS = Path.of(
            "build/classes/java/main/cn/kycraft/kuayue/mixins/SignalBoundaryMixin.class");

    @Test
    void mixinConfigRegistersSignalBoundaryMixin() throws IOException {
        String config = Files.readString(MIXIN_CONFIG);

        assertTrue(config.contains("\"package\": \"cn.kycraft.kuayue.mixins\""));
        assertTrue(config.contains("\"required\": true"));
        assertTrue(config.contains("\"SignalBoundaryMixin\""),
                "mixin 不在 kuayue.mixins.json 的 mixins 里就不会被加载: " + config);
    }

    @Test
    void compiledMixinTargetsSignalBoundaryAndBlockEntityAdded() throws IOException {
        assertTrue(Files.isRegularFile(MIXIN_CLASS), "mixin 类没有编译进产物: " + MIXIN_CLASS);
        String bytecode = new String(Files.readAllBytes(MIXIN_CLASS), StandardCharsets.ISO_8859_1);

        assertTrue(bytecode.contains("com/simibubi/create/content/trains/signal/SignalBoundary"),
                "@Mixin(SignalBoundary.class) 没编进 mixin 类");
        assertTrue(bytecode.contains("blockEntityAdded"),
                "@WrapMethod 的目标方法名没编进 mixin 类");
    }
}
