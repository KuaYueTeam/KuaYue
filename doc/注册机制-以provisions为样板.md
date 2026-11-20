# 注册机制：以 provisions 为样板

> 本文对照 `modules/provisions` 拆解 KasugaLibNext 的注册链路，并给出把它套到 `overhead_line.json` 的 wire 上的替换清单。
> 结论先放前面： **provisions 里找不到任何一行「注册物品」的代码**——没有 `DeferredRegister`、没有 `reg.register(...)`、也没有 `Registry.register(...)`。
> 物品能进游戏，靠「登记工厂 → 声明 JSON → 树形 dispatch → NeoForge 事件」四件事拼起来。

相关文档：`doc/数据驱动注册-快速上手指南.md`（JSON schema 全量）、`doc/轨道数据驱动Schema.md`。

---

## 一、参与的五个角色

| 角色 | 文件 | 干的活 |
| --- | --- | --- |
| 模块类 | `modules/provisions/src/main/java/cn/kycraft/kuayue/provisions/ProvisionsModule.java:20-38` | 在 Micronaut 构建期把工厂登记好 |
| 工厂表 | `modules/provisions/src/main/java/cn/kycraft/kuayue/provisions/init/ProvisionsFactories.java:46-102` | 登记 6 个 `type → 造 Reg 的 lambda` |
| JSON 索引 | `modules/provisions/resources/provisions/data/kuayue/kasuga_lib/data_driven/provisions.json` | 声明「去哪读内容文件」 |
| JSON 内容 | `modules/provisions/resources/provisions/data/kuayue/kasugalib/provisions.json` | 声明「注册哪些 id、用哪个 type、参数是什么」 |
| 分发器 | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/registration/core/DefaultRegisterContextDispatchers.java:23-42` | 把 NeoForge 事件换算成 `dispatchRegister` |

---

## 二、时序：三段

### 阶段 1 — 类加载（还没有任何注册行为）

```java
// ProvisionsModule.java:24-25
public static final RegistryGroup REGISTRY_GROUP = new RegistryGroup()
        .setParent(KuaYue.REGISTRY);
```

只是凭空建了一个**空组**挂到根上，里面一个 `Reg` 都没有。

### 阶段 2 — Micronaut 上下文构建（`@PostConstruct`，仍然没有注册行为）

```java
// ProvisionsModule.java:31-38
@PostConstruct
public void init() {
    ProvisionsFactories.register();   // 唯一做的事
}
```

```java
// ProvisionsFactories.java:46-57
public static synchronized void register() {
    if (registered) return;
    registered = true;

    FactoryRegistry.registerItem(SIMPLE_ITEM, (id, params) -> {
        ItemReg<HandHeldFoodItem> reg = ItemReg.of(id,
                (Function<Item.Properties, HandHeldFoodItem>) HandHeldFoodItem::new);
        applyFood(reg, params);
        // ItemReg<WT> 是 Reg<ItemReg<WT>, WT>，泛型不变，必须双转成 Reg<?, Item>
        return (Reg<ItemReg<?>, Item>) (Reg<?, ?>) reg;
    });
    // …另外 5 个 type
}
```

**这一步只是在静态表里放了一个「造物函数」**（`type 字符串 → (id, params) → Reg`），没有 `new` 任何物品。
注释里「工厂必须先于 data-driven 内容阶段注册」就是为这个——后面解析 JSON 时要用这张表。

### 阶段 3 — mod bus 起来后，NeoForge 事件触发注册

```java
// KuaYueApplication 的 @PostConstruct
KuaYue.REGISTRY.register(modEventBus);
//   → KasugaLib.afterRunning(modEventBus,
//         () -> RegisterContextRegistry.configure(COMMON, REGISTRY, modEventBus))
```

`configure` 干的事：

```java
// DefaultRegisterContextDispatchers.java:23-30
modEventBus.addListener(EventPriority.HIGHEST, RegisterEvent.class, (event) ->
    registry.dispatchRegister(new RegisterContext<>(
        RegistrationStage.REGISTER_EVENT, new RegisterStageContext(event))));
```

于是链条变成：

```
RegisterEvent(Registries.ITEM)
  └─ REGISTRY.dispatchRegister(REGISTER_EVENT)          // Reg.java:68-75
       ├─ this.register(ctx)                            // 根节点自己
       └─ for each child → child.dispatchRegister(ctx)  // 递归
            ├─ jsonRoot (kuayue:json_root)
            │    ├─ JsonRegistryGroup(kuayue:meals_food)
            │    │    └─ ItemReg(lunch_box).register(ctx)
            │    └─ ItemReg(kua_cola).register(ctx)
            └─ …
```

每个 `Reg.register(ctx)` 里只是 `ctx.onStage(REGISTER_EVENT, ...)`；真正落进 `Registries.ITEM` 的语句在
`KasugaLibNext/modules/core/src/main/java/lib/kasuga/registration/minecraft/common/MinecraftDeferRegistryReg.java`：

```java
ResourceLocation id = transform(ResourceLocationModifiers.ID,
        ResourceLocation.fromNamespaceAndPath("minecraft", name));
value = createObject(id);                 // ← 到这一刻才 new HandHeldFoodItem(props)
ctx.register(entryKey, id, () -> value);
```

**关键点**：`onStage` 是「阶段相等才执行」。同一个 `Reg` 会在 5 个不同事件里被反复 `dispatchRegister` 五次，每次只命中自己关心的那个阶段。
所以 `Reg` 天然可以同时承担「注册物品」「往 tab 里塞」「注册能力」多件事，互不干扰。

---

## 三、JSON 是怎么变成 `Reg` 的

内容文件由 `JsonTreeBuilder.buildForMod("kuayue")` 解析
（`KasugaLibNext/modules/data-driven/src/main/java/lib/kasuga/registration/data_driven/builder/JsonTreeBuilder.java:109-115`，
`Loaded N JSON entries across M types` 那行日志就是这里打的），然后按 `TypeHandler` 的 phase 排序逐条 `apply`：

```java
// ItemTypeHandler.java:56-60
protected Reg<?, ?> createRegistration(ItemDef definition, String path) {
    FactoryRegistry.ItemFactory factory = FactoryRegistry.getItemFactory(definition.type());
    if (factory == null) return null;                   // ← 静默跳过的地方
    return factory.create(path, definition.params());   // ← 调用 ProvisionsFactories 里登记的 lambda
}
```

拿到 `Reg` 后，由基类 `RegTypeHandler.apply`（`.../data_driven/handler/RegTypeHandler.java:63-106`）统一做四件事：

```java
// 1. 非 minecraft 命名空间 → 追加一次 id 变换
if (!namespace.equals("minecraft")) reg.withProperty(ResourceLocation.class,
        loc -> ResourceLocation.fromNamespaceAndPath(namespace, loc.getPath()));

// 2. 挂父节点：有 registry_group 就挂组，否则挂 json_root
reg.setParent(registryGroupId != null
        ? (group != null ? group : context.getRootGroup())
        : context.getRootGroup());

// 3. 创造模式标签页：properties.tab 优先，否则继承组的 item_properties.tab
reg.configure(CreativeTabModifiers.set(() -> tab));

// 4. 类型专有的 property（物品就是 Item.Properties 的那些）
configureTypeSpecific(definition, reg);
```

> `reg.setParent(...)` 才是「把它接进注册树」的动作。接进去之后不用你管，事件一到自动注册。

---

## 四、`lunch_box` 的完整链路

JSON：

```json
{
  "id": "kuayue:lunch_box",
  "type": "simple_item",
  "registry_group": "kuayue:meals_food",
  "properties": { "tab": "kuayue:provisions_tab" },
  "params": { "food": { "nutrition": 8, "saturation": 0.6 } }
}
```

```
① ItemTypeHandler.parse                → ItemDef(id, type, group, properties, params)
② FactoryRegistry.getItemFactory("simple_item")  → 阶段 2 登记的 lambda
③ lambda: ItemReg.of("lunch_box", HandHeldFoodItem::new)
          applyFood(reg, params)       → reg.isFood(true).foodNutrition(8).foodSaturationModifier(0.6f)
④ RegTypeHandler.apply:
          setParent(meals_food)
          configure(CreativeTabModifiers.set(() -> provisions_tab))
          withProperty(Item.Properties.class, …)   // 来自 properties
⑤ context.putReg("items", "kuayue:lunch_box", reg)
⑥ RegisterEvent 到达 → dispatchRegister 递归到它
          id 变换链（json_root 的 namespace + registry 的 withNamespace → kuayue:lunch_box）
          createObject(id) → new HandHeldFoodItem(new Item.Properties().food(...))
          ctx.register(Registries.ITEM, kuayue:lunch_box, () -> item)
⑦ BuildCreativeModeTabContentsEvent 到达 → CREATIVE_TAB_CONTENT_REGISTRATION
          ItemReg 取 cachedTab；tabKey 匹配 provisions_tab 就 event.accept(item)
```

`applyFood` 那段注释（`ProvisionsFactories.java:139-144`）值得注意：**故意不走 `Item.Properties#food`**，因为原版在物品声明了 crafting remainder 时会整段跳过食物处理，而且 `JsonItemParser` 不认 `properties.food`。这是踩过坑留下的注释。

---

## 五、provisions 里「不走 JSON」的那部分

同一棵树，两种接法：

| 内容 | 接法 | 例子 |
| --- | --- | --- |
| 方块、物品 | JSON `blocks` / `items` + 工厂 | 上面那条 |
| 流体、效果 | Java 静态 `Reg` + `setParent(自己的 RegistryGroup)` | `food/effect/AllEffects.java`、`food/fluid/FluidRegistration.java` |
| 纯常量（食物数值） | **不注册**，只是被 lambda 引用的 Java 常量 | `food/DrinkRegistration.java` 的 `KUA_COLA_FOOD` |

第三类最常被误解——`DrinkRegistration` 里没有任何注册代码，它只给 `DRINK` 工厂的 lambda 提供 `FoodProperties` 常量。

---

## 六、三条硬约束，以及「为什么」

1. **不能手动 `DeferredRegister`**
   会绕过这棵树，同一 id 两条管线 = 重复注册崩溃。provisions 的方块物品由 `BlockReg.withBlockItem` **内部**创建 `ItemReg`（见 `DrinkRegistration` 类注释），就是为了保证「一个 id 只有一条管线」。

2. **工厂必须先注册**
   否则 `createRegistration` 返回 `null`，`RegTypeHandler:71-74` 只打一行 **warn**：
   `No factory for type '{}' (id '{}')`，然后**静默跳过**。
   日志级别是 warn，极易忽略，表现就是「JSON 写了但游戏里没有」。

3. **注册期间不能解引用物品**
   `remainderItem()`（`ProvisionsFactories.java:119-125`）把 `params.remainder` 包成 `Supplier<Item>`，注释写明「注册期间不能解引用」。
   因为在 `RegisterEvent` 里 `BuiltInRegistries.ITEM.get(id)` 可能拿到 `null`，取决于事件到达顺序。

---

## 七、顺带一个坑：`REGISTRY_GROUP` 目前是死代码

grep 整个 `modules/provisions/src/main/java`，`ProvisionsModule.REGISTRY_GROUP` **只有声明处出现一次，没有任何引用**。

原因：provisions 的全部内容都走 JSON，而 JSON 条目被 `RegTypeHandler:84-87` 挂在 `kuayue:json_root` 下，根本不经过 `ProvisionsModule.REGISTRY_GROUP`。

所以「本模块的根注册组，挂在模组主注册树下面」那句注释目前只是**预留**。对 wire 的启示：

- 走 **Java 静态路**（`AllWires` 直接 `new ItemReg`）→ `REGISTRY_GROUP` 是必需的，`setParent(REGISTRY_GROUP)` 是唯一接入点；
- 走 **纯 JSON 路** → 不需要它（挂 `json_root` 即可），留着无害。

---

## 八、换成 wire，只需替换 4 处

| provisions | wire | 备注 |
| --- | --- | --- |
| `ProvisionsFactories.register()` 里 6 个 `registerItem/register` | 1 个 `registerItem("wire", lambda)` | lambda 内 `ItemReg.of(id, props -> new WireReg(props, …))` |
| `type: "simple_item"` 等 | `type: "wire"` | 名字随意，但常量与 JSON 必须一致 |
| `data/kuayue/kasuga_lib/data_driven/provisions.json` | `…/data_driven/overhead_line.json` | `"on_register": ["kuayue/overhead_line/wires.json"]` |
| `ProvisionsModule.@PostConstruct` | `OverheadLineModule.@PostConstruct` | 先 `Factories.register()`，再触发 `AllWires` 类初始化 |

其余（`KuaYueApplication` 里那句 `REGISTRY.register(modEventBus)`、事件监听、tab 阶段）**完全不用动**，那是 core 的事。

---

## 附录 A：`RegistrationStage` 全量（`KasugaLibNext/…/registration/stages/RegistrationStage.java`）

| 常量 | 触发事件 | 典型用途 |
| --- | --- | --- |
| `REGISTER_EVENT` | `RegisterEvent`（HIGHEST 优先级） | 真正把对象塞进注册表 |
| `CREATIVE_TAB_CONTENT_REGISTRATION` | `BuildCreativeModeTabContentsEvent` | 往创造模式标签页塞物品 |
| `REGISTER_CAPABILITIES` | `RegisterCapabilitiesEvent` | 注册 Capability |
| `PAYLOAD_REGISTRATION` | `RegisterPayloadHandlersEvent` | 注册网络包 |
| `NEW_REGISTRY` | `NewRegistryEvent` | 新建自定义 registry |
| `COMMON_SETUP` | —（列表中有，本项目未挂监听） | 通用初始化 |
| `MENU_SCREEN_BINDING` | `RegisterMenuScreensEvent`（client） | 菜单 GUI 绑定 |
| `BAKING_COMPLETE` | `ModelEvent.BakingCompleted`（client） | 模型烘焙完成 |

> **没有 `RegisterRenderers` 阶段**。block entity / 实体的 renderer 不能指望注册期，需另找挂接点（client 侧静态表，或 `BAKING_COMPLETE`）。

---

## 附录 B：调试日志关键字（`run/logs/debug.log`）

| 关键字 | 含义 |
| --- | --- |
| `Loaded N JSON entries across M types for mod 'kuayue'` | JSON 索引与内容解析成功 |
| `No data-driven index directory for mod 'kuayue'` | 索引目录不存在（debug 级） |
| `No factory for type 'xxx' (id 'yyy')` | **工厂没注册**，该条目被静默跳过 |
| `Duplicate id 'zzz' in field 'items'` | 重复 id，loader 未拦住 |
| `Posting deferred registry entry` | 某个 `Reg` 走到了真正的注册 |
| `Source file not found for mod 'kuayue': data/kuayue/…` | `on_register` 里的路径写错 |

---

## 附录 C：wire 落地骨架（方案 A，`WireReg extends Item`）

四个文件的最小骨架要点：

- `WireReg extends Item`，字段 `maxVoltage / maxCurrent / maxLength / rendererKey(String)`，`getType()` 惰性构造 `OverheadLineType`；
- `OverheadLineFactoies.register()` 里 `FactoryRegistry.registerItem("wire", (id, params) -> { … return (Reg<ItemReg<?>, Item>) (Reg<?, ?>) reg; })`；
- `AllWires` 用 `ItemReg.of("overhead_line_wire", props -> new WireReg(props, 1000f, 1000f, 1000f, "overhead_line_wire")).tabTo(TAB).setParent(REGISTRY_GROUP)`；
- **Java 静态路与 JSON 条目二选一**，同一 id 两边都写会重复注册崩溃；
- renderer 只存 **字符串 key**，客户端懒解析。`OverheadLineRenderer` 是 `@OnlyIn(Dist.CLIENT)`，不能出现在 common 侧字段类型里（NeoForge RuntimeDistCleaner 会在专用服务端剥掉它）。
