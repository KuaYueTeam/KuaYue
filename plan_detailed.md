# KuaYue 车包系统详细方案（讨论稿）

> 本文是一份可独立阅读的方案，包含项目背景、长期愿景、v1 约束、实现边界及跨项目改动。已确认的产品规则直接写入对应章节，阅读和实施时不依赖单独的决策清单。原始设想记录在 `plan.md`，仅作为来源参考。第 20–26 节补充面向 agent 开发的契约、任务与验收门槛；技术建议与尚待验证的实现时机仍分别标注，本文不代表功能已经实现。

## 项目背景与长期愿景

### 三个项目如何协作

| 项目 | 当前定位 | 本方案中的职责 |
|---|---|---|
| **KuaYue（跨越）** | 基于 Create 和 KasugaLibNext 的 Minecraft 铁路模组，提供车厢部件、转向架、轨道等内容 | 定义车包协议、车型内容与依赖规则，管理车包并调用通用数据驱动管线 |
| **KasugaLibNext** | 为消费端模组提供注册、模型、动画、渲染及数据驱动等通用能力的基础库 | 扩展内容来源、统一构建、校验和资源适配能力，不负责车包业务 |
| **KasugaLib-BlockBenchPlugin** | 在 Blockbench 中编辑模型附加组件的独立插件，目前可编辑 IK、物理和粒子等配置 | 后续扩展为内容创作工具：读取消费端 mod 发布的 schema，补全内容信息并导出可安装产物 |

Create 是 KuaYue 铁路内容所依赖的游戏机制与集成基础；Blockbench 是作者制作模型和动画的编辑器。本文主要讨论 KasugaLibNext 和 KuaYue 的运行时改造，插件导出放在运行时协议稳定之后。

### 为什么需要车包

现有 KuaYue 已把部分内容定义从 Java 注册代码移到 JSON，但注册期数据主要随模组 JAR 分发。内容作者仍需要把文件纳入项目资源和构建流程，玩家不能仅靠安装一个外部内容目录就新增车型。

车包系统的目标，是把**内容制作与模组代码发布解耦**：开发者先提供可复用的行为工厂、handler 和 schema，作者再通过数据定义和资源组合出不同车型，玩家通过安装车包使用这些内容，而不必为每个车型修改或重新构建模组。

### 原始设想中的内容组织

一个“车包”称为 **pack**，包含多个车型；一个车型相关的一组内容称为 **parcel**。完整愿景下的内容包括：

| 层级 | 内容 |
|---|---|
| **Parcel / 车型** | 车型名称、介绍和其他元数据；方块及其模型、纹理；所有文本的本地化映射；声音；UI 主题；tag、loot table、recipe 等常规资源；未来可能需要的蓝图及子结构 |
| **Pack / 车包** | 包名称、版本、协议/格式信息；parcel 清单；依赖 pack、依赖 mod 及版本、KuaYue 与游戏版本范围；包级共享资源与本地化；未来可能需要的包级蓝图和联动描述 |

这些是长期内容范围，不表示 v1 必须实现全部消费者。尤其是蓝图、UI 主题和科技树，不能仅因文件可被打包，就声称游戏已支持其运行时行为。

KuaYue 随模组 JAR 提供内置包 **`kuayue-core`**（下文也称 corepack），将自带内容纳入车包协议。它默认启用，允许在游戏实例级配置中整包禁用，重启后生效。资源 namespace 保持 `kuayue`，原有游戏内容 ID 不变。禁用只关闭包内容，不卸载 KuaYue 代码或通用能力，也不安全删除已有世界内容。

### 本文中“数据驱动”的含义

**Schema** 是内容文件的结构契约，描述字段、类型、约束和引用。KasugaLibNext 提供通用 schema 及其解析/注册能力；消费端 mod 可以提供自己的 schema 和对应 handler。例如 KuaYue 已提供 bogey（转向架）和 track（轨道）相关定义。

- **数据描述内容：**注册哪些 ID、使用哪个已有行为类型、有哪些参数、引用哪些模型和动作。
- **代码提供行为：**工厂创建具体对象，handler 解析与校验数据并接入注册/运行时。
- **资源提供表现与配套数据：**模型、纹理、声音、本地化、配方等沿用各自格式。

数据驱动不等同于新增 Minecraft 注册项。`on_register` 表示启动期 handler 处理域，也能只生成配置或元数据；`on_reload` 可解码状态机、动画片段等非注册型运行时定义。原生资源无需经过 Kasuga 自定义 schema。新 JSON Schema 文件只描述格式，真正加载仍需 Java handler 与消费者；调查依据见第 27 节。

因此，“完全数据驱动”指在已有 schema 和行为能力范围内，新增车型无需修改 Java；不是用任意 JSON 创造全新的游戏行为，也不是运行中动态增加 Minecraft 注册项。

### 作者与玩家的最终链路

长期链路是：

```text
作者在 Blockbench 建模、制作动画并补全车型信息
→ 插件依据 KuaYue 发布的 schema 导出车包
→ 玩家把 pack 目录或根内直接含 pack.json 的 ZIP 放入 <game-dir>/kuayue/packs/
→ 在统一配置/游戏界面设置整包开关与资源覆盖顺序
→ 启动或重启游戏，校验并注册全局启用的 pack，同时加载配套资源
→ 在游戏中使用新增车型内容
```

插件的长期定位不局限于 KuaYue：家具等其他消费端 mod 也可以发布 schema，让插件导出相应内容定义与资源。但 v1 优先验证 KuaYue 的手写车包加载闭环，通用编辑器表单和其他 mod 的导出支持不是首阶段前提。

### 长期愿景与 v1 的区别

- 长期愿景包含由 pack/parcel 元数据派生内容浏览、物品栏组织，乃至科技树等系统；**v1 明确不包含科技树、研究或解锁语义**。
- 玩家可以通过外部纯数据车包新增内容，但**安装、启用、禁用车包后必须重启**；普通资源重载不能替代启动期注册。
- **只有 pack 可以启用/禁用**，parcel 是车型组织单位，没有独立开关。加载是游戏实例级别，所有存档共用本次启动的有效包集合；打开世界或连接服务器不自动切换包。
- 在插件实现之前，手写车包必须已经可以完成安装和使用，避免加载协议依赖某个编辑器。

### 阅读导航

- 第 0–4 节：v1 范围概览、目标、现状、概念和文件布局。
- 第 5–10 节：加载流程、接口方向、依赖/资源规则、兼容性与 corepack。
- 第 11–14 节：阶段路线、验证矩阵、风险与现有内容基线。
- 第 15–19 节：KasugaLibNext / KuaYue 分工、底层改动清单与跨项目验收。
- 第 20–26 节：执行契约的优先级、文件/API/生命周期约定、corepack 可禁用实现、任务拆分与开发就绪检查。
- 第 27 节：经 subagent 调查确认的非注册型数据驱动能力、纯资源包契约、现有限制及补充任务。

## 0. v1 范围概览

v1 建立游戏实例级车包系统：支持目录和 ZIP 直接安装，一个 pack 是安装与启禁用的单位；资源允许跨包、跨 namespace 覆盖，玩家可在游戏内调整包括 `kuayue-core` 在内的优先级，重启后生效。依赖均为必需依赖；包的新版本声明可升级的自身历史版本。所有存档共用启动集合，不设存档开关；科技树不属于 v1。具体规则分别落实在第 4、7、8、21、24 节。

本节只提供阅读概览，不作为覆盖后文的补丁。各业务章节必须独立遵守上述规则；技术方案中仍标为建议的部分，不因产品规则已确认而自动视为已获确认。

## 1. 目标、非目标与成功标准

### 目标

玩家将目录车包或 ZIP 车包放入 `<game-dir>/kuayue/packs/`，启动或重启后使用新增内容，无须先解压 ZIP。整包开关 `packs.enabled` 和资源优先级 `packs.order` 位于 `<game-dir>/kuayue/config.json`，作用于整个游戏实例。游戏内调整顺序只保存下次启动设置，不即时改变本次资源栈。内置 `kuayue-core` 可禁用和排序；所有存档共用同一启动集合与覆盖顺序。

### 加载与兼容规则

- v1 扫描并直接读取目录和 ZIP；ZIP 根直接是包内内容，不包含外层文件夹。
- 资源同路径冲突按玩家指定顺序解决，包可包含多个 namespace，也可提供其他包 namespace 下的覆盖资源。依赖只决定能否有效启动，不强制资源优先级。
- 所有依赖都是必需依赖，缺失、禁用或版本不满足则阻断依赖者；不实现 optional 或条件联动。
- 升级存档仅依据声明的包版本兼容规则。同包同版本即使内容不同也不限制、不强制增加版本号；作者自行承担修改后果。平台不以文件 hash 做存档或多人连接阻断。
- 注册 ID 冲突与普通资源覆盖分开：**v1 不考虑注册同名方块/物品**，重复创建同一 registry ID 按注册冲突拒绝；资源优先级不授权同名注册。边界见第 7、22 节。
- 启动期非注册配置允许按资源优先级覆盖；所有车包使用相同策略，不识别“纯资源包”类别给予特殊兼容豁免。
- 外部新包默认禁用；首次启动前玩家可手动编辑 `<game-dir>/kuayue/config.json` 按稳定 pack ID 启用。内置 `kuayue-core` 的既定默认启用不变。
- 内容引用仅限本包和显式声明的依赖，不接受隐式传递引用；多人车包下载/分发同步不由 KuaYue 提供，由作者/服主管理。

### v1 非目标

- 运行时安装/卸载、热注册方块或修改 pack 启用状态后即时生效。
- parcel 独立开关、存档专属开关、进入世界或连接服务器时自动切换车包。
- 科技树、科技解锁或研究进度。
- 通过 JSON 编写全新的方块行为、任意 Java/JS 脚本或任意新 handler。
- 插件自动理解任何 mod 的 Java 类型。自定义 schema 必须由对应 mod 提供机器可读的 schema 描述。
- 重复创建既有 registry ID、动态替换已注册对象。普通资源覆盖是 v1 功能，不属于此非目标。
- 自动下载依赖 pack、在线市场或云端同步。

### 成功标准

1. 不改 KuaYue Java 代码、不重新构建 KuaYue，仅添加外部 pack，即可在启动后看到一个新车型及其预期方块/转向架。
2. 禁用该 pack 并重启后，其注册内容不再出现；其余已启用且不依赖该包的 pack 仍可加载。corepack 也遵守这一规则。
3. 无效 pack 能指出 pack ID、文件路径、字段/引用和错误原因，不造成静默缺内容或部分车型注册。
4. KuaYue 自带内容与第三方内容使用同一份 pack 协议和同一条注册管线。
5. 禁用 `kuayue-core` 且不加载外部包时仍可启动；仅加载不依赖它的外部包时通用能力仍可用。
6. 切换存档或连接服务器不改变本次包集合；实例开关、覆盖顺序和安装清单存于 `kuayue/config.json`，世界版本历史由 Minecraft SavedData 存入该存档的 `data/kuayue_packs.dat`。
7. 目录与 ZIP 形式的同内容车包加载结果一致；玩家在游戏内调整资源顺序并重启后，最高优先级资源生效，corepack 参与同一排序。
8. 同包多个 namespace 和多包共享 namespace 可加载；历史世界按新包的自身版本兼容声明升级，不以版本变化本身拒绝。
9. 纯原生资源包无需伪造注册索引；只含 `on_reload` 的 schema 包无需创建方块/物品。两者的目录与 ZIP 形式都必须可加载，并遵守相同开关、顺序、依赖和存档规则。

## 2. 当前实现边界与设计影响

源码调查已确认当前存在非注册型扩展：FSM 的 `state_machines` / `animation_clips` 经 ReloadHandler 写入运行时 bucket，KuaYue 的 motions/models 也只编译配置。但外部纯资源 pack/parcel 尚未实现，不能把已有扩展点当作外部车包已能安装的证据。

- KasugaLibNext 的 `JsonTreeBuilder` 当前按模组 ID 查找模组 JAR（`ModList` / `IModFile`），从 `data/<modid>/kasuga_lib/data_driven/` 找索引，再从对应 JAR 读取内容。它还不是任意磁盘目录加载器。
- KuaYue 的 `KuaYueApplication` 在模组初始化阶段分发 `KuaYue.REGISTRY`。因此游戏实例级 pack 清单、注册内容和注册 ID 必须在这次分发前准备好，不能延迟到某个存档载入时选择车包。
- KasugaLibNext 的 `TypeHandler`、`RegBuildContext` 已提供注册类型扩展点；KuaYue 已在此基础上定义 bogey、track 等类型。新增来源应复用现有 handler，而不是为 pack 复制一套注册实现。
- `ResourceSystem` 管理的是游戏运行时客户端/服务器的资源管理器。它有助于普通资源接入，但不能单独解决启动期方块注册。
- KuaYue 的车厢资源经过 resource compiler 聚合进模组资源；现有车厢、转向架、轨道与餐食数据可作为 corepack 迁移和回归基线。

**设计约束：**外部文件系统 pack 和 JAR 内置 pack 必须在注册前汇入可验证的数据来源集合；客户端资源和服务器数据资源也必须使用同一份启用 pack 清单。不能只把目录加到模型资源搜索路径，就认为 pack 已完整加载。

## 3. 核心概念

### Pack

安装、启用/禁用、依赖解析、版本管理和世界兼容性检查的最小单位。所有启用状态只记录 pack ID，不记录 parcel 的启用状态。加载范围是整个游戏实例；不同存档不能单独启用或禁用任何 pack。

### Parcel

Pack 中的一组车型内容，用于组织名称、介绍、图标、车型分类和内容索引。Parcel 不拥有独立启用状态。v1 可以通过 parcel 列表展示车型，但不可把 parcel 当成独立安装包。

### Corepack

随 KuaYue JAR 分发的内置 pack，名称及 pack ID 为 **`kuayue-core`**。它提供当前 KuaYue 的默认内容，并和外部 pack 使用相同格式/加载流程。默认启用，**v1 允许禁用**；运行代码、通用工厂、handler 和基础设施仍留在模组代码中。没有依赖 corepack 的外部包不能因 corepack 被禁用而失效。

### 内容 schema

KasugaLibNext 提供通用注册与非注册型处理扩展；KuaYue 等 mod 可定义自己的 schema。新增格式必须由 mod 提供解码 handler 与实际消费者，仅 JSON Schema 文档不能接入运行时。加载阶段和是否产生 registry claim 是两个独立维度。

## 4. 安装、分发与元数据

安装目录为 `<game-dir>/kuayue/packs/`。一个直接子文件夹或一个直接子 ZIP 文件对应一个 pack；目录和 ZIP 根都直接包含 `pack.json`。ZIP 打包的是车包文件夹**内的内容**，不带外层车包文件夹，并由游戏直接读取，无须玩家先解压。安装文件夹名和 ZIP 文件名均不决定 pack ID。

以下是目录概览；完整字段与逻辑资源映射见第 21 节。安装目录、ZIP 根结构和统一配置位置是明确约束，内部资源布局及尚未确认的技术建议按各自契约实施。

```text
<game-dir>/kuayue/packs/<车包文件夹>/
├── pack.json
├── assets/<namespace>/...
├── data/<namespace>/...
├── parcels/
│   └── <parcel-folder>/
│       ├── parcel.json
│       ├── assets/<namespace>/...
│       └── data/<namespace>/
│           └── kasuga_lib/data_driven/<index>.json
```

`pack.json` 的字段摘要如下；必填性与校验规则按第 21.2 节的执行基线处理：

| 字段 | 用途 |
|---|---|
| `format_version` | 车包格式版本；v1 固定为 `1`，与 pack 自身版本分离 |
| `id` | 稳定、全局唯一的 pack ID；改显示名称不能改变它 |
| `version` | pack 内容版本，建议采用 SemVer |
| `namespaces` | 本包提供资源的命名空间集合；可包含多个值，不要求跨包唯一 |
| `name_key` / `description_key` | 名称与介绍的本地化键 |
| `parcels` | 本 pack 内的 parcel 清单 |
| `dependencies` | 游戏、NeoForge、KuaYue、依赖 mod 和依赖 pack 的版本范围 |
| `save_compatibility` | 当前版本允许从哪些自身历史版本的存档升级 |

`parcel.json` 描述稳定 `id`、名称/介绍本地化键、图标资源 ID 和索引列表，没有独立启用状态、版本开关或科技树字段。注册内容继续使用 Kasuga 的 `on_register` / `on_reload` 索引结构，不另造一套方块和物品 schema。

启禁用、资源覆盖顺序和车包管理清单不放进 pack/parcel manifest，统一写入 `<game-dir>/kuayue/config.json`。游戏内顺序界面修改 `packs.order`，重启后生效；ZIP 来源是 v1 必需能力。

### 路径和 ID 规则

- `pack.json` 的 parcel 路径相对 pack 根；`parcel.json` 的索引路径相对 parcel 根；Kasuga 索引条目相对该逻辑资源根下的 `data/<namespace>/`。三种基准不可混用；具体映射见第 21 节。拒绝绝对路径、`..` 和符号链接。
- pack/parcel ID 与 Minecraft `ResourceLocation` 是不同概念；不能未经验证就把 pack ID 当作资源 namespace。
- 定义及资源 ID 必须明确带 namespace，不能把 source owner 自动推断为 namespace；同一包可声明多个 namespace，多包可共享 namespace。
- 允许提供 `kuayue`、`minecraft` 等 namespace 下的普通覆盖资源，不因 namespace 归属阻断。此许可不授权重复注册已有 block/item 等 registry ID。
- 对注册定义的引用根据定义的真实 owner 校验必需依赖，不能通过 namespace 唯一性推断依赖。普通资源覆盖是否依赖目标包，由作者显式声明；不强制覆盖包必须依赖被覆盖包。

## 5. 启动加载生命周期

启动阶段从统一配置和安装来源建立游戏实例级有效集合，之后固定为 `StartupPackSnapshot`，供注册与资源消费共同使用。运行中安装、移除或修改开关均不改变本次集合，必须重启生效；任何存档或连接事件都无权重新选包。技术管线按“发现 → 校验/解析 → 构建注册对象 → 注册提交 → 资源加载”组织，具体事件时机仍由 E0 验证。

1. **读取启用配置**：从 `<game-dir>/kuayue/config.json` 读取实例级 `packs.enabled` 和 `packs.order`；`kuayue-core` 默认启用、可禁用、可排序，不作为外部目录包管理。
2. **发现 pack**：扫描 `<game-dir>/kuayue/packs/` 的直接子目录、直接子 ZIP 文件及内置 `kuayue-core`；读取 manifest，检查格式、ID、namespaces、文件大小和路径安全。
3. **检查兼容性**：验证 Minecraft、NeoForge、KuaYue、依赖 mod/pack 的版本范围；检查依赖缺失和依赖环。
4. **收集来源**：仅组合请求启用且有效的来源；`kuayue-core` 被禁用时不加入。记录每个文件来自哪个 pack/parcel，供错误报告和冲突诊断使用。
5. **解析全部定义**：解析 Kasuga 索引、通用类型与 KuaYue 自定义类型；检查 schema、ID 唯一性及跨文件/跨 pack 引用。
6. **构建注册树**：按 handler 生命周期阶段生成 deferred registrations；registry groups 等依赖型定义按父引用顺序处理。
7. **统一提交注册**：只将通过预检的内容加入最终注册分发。不要为每个 pack 单独触发一轮 Minecraft registry 注册。
8. **挂载资源**：为客户端和服务器加载同一有效集合，按冻结的 `packs.order` 构建资源栈；同路径覆盖和类型特定合并遵循第 21 节。
9. **记录加载元信息**：输出有效 pack 列表、声明版本、格式版本和校验结果，清单保存在统一配置；内部缓存摘要可选，不作兼容门槛。世界历史不反向影响启用集。

### 错误隔离建议

- 单个 pack 的格式错误、依赖缺失或内部引用错误：拒绝整个 pack，并拒绝依赖它的 pack；其他已启用且独立的有效包继续。禁止只关闭错误 parcel 让半个包加载。
- `kuayue-core` 被禁用是正常配置，不自动重启用；依赖其内容的包被阻断。无有效包时保持 runtime 可启动，不回退加载默认内容。
- 重复 registry ID 仍按注册冲突处理；同 namespace 和同普通资源路径不属于阻断冲突，按资源优先级覆盖，corepack 无固定最高优先级。
- 任意 pack 在注册提交阶段仍可能失败：应视为启动错误，而不是假装 pack 已部分成功。注册一旦产生副作用，运行时“回滚 pack”并不可靠。
- diagnostics 至少给出 pack ID、parcel ID、源文件、JSON 路径、handler 类型、错误类别和建议修复。

## 6. 内容来源和 schema 接口方向

### KasugaLibNext 侧

建议增加与物理来源无关的内容源抽象。来源至少能提供：

- 稳定的 `sourceId`、所有者/namespace、pack/parcel 元信息。
- 按路径读取索引与内容文件的能力。
- 当前支持的资源类型或文件分类。
- 内容来源诊断上下文。

JAR、外部目录和 ZIP 都是 v1 内容来源，使用同一接口提供逻辑文件；Kasuga 的注册解析、handler、属性解析与冲突检查不应关心物理容器。

需要特别检查当前 `RegBuildContext` 中按 mod ID 归一化 ID、保存 registry group/registration/metadata 的设计。第三方 namespace 的 ID、跨 pack 引用及 KuaYue 的 bogey/track phase metadata 都要有明确 owner 与作用域，不能简单把所有外部内容伪装成 `kuayue` 模组自身的文件。

### KuaYue 侧

- 保留 Java 中的 `BogeyDataDrivenModule`、`TrackDataDrivenModule`、工厂和自定义 parser/handler；它们属于 runtime，不随 `kuayue-core` 开关关闭。
- 继续使用 `bogey_sizes`、`bogey_motions`、`bogey_models`、`bogey_blocks`、`bogey_styles`、`tracks` 等已有类型。
- 外部 pack 只能使用运行中的 KuaYue 已注册的 schema 和工厂；pack 不能动态上传 handler。
- 新的 KuaYue schema 必须有机器可读的版本化 schema 文档，并与 Java parser 的允许字段、引用校验同步。

### 插件侧（后续阶段）

插件不能靠 Java 反射自动理解任意 mod 的 `TypeHandler`。对应 mod 应发布静态 schema 描述（建议 JSON Schema 加 editor hints），至少包含字段类型、默认值、资源 ID 类型、跨字段约束和编辑器显示信息。插件按 `format_version` 与 schema 版本导出，并先写到暂存目录、校验通过后再发布为车包。

BlockBench 模型组件 JSON 中的 `kasuga_lib` 扩展与注册索引 `data/<namespace>/kasuga_lib/data_driven/` 是两种不同机制；插件可以生成被游戏模型 loader 消费的模型扩展，但不能把它当成车包注册表实现。

## 7. ID、依赖、优先级和资源合并

### 注册 ID 和冲突

- 外部定义和 corepack 定义都参与同一全局冲突检测；冲突检查基于最终 registry ID，而非仅基于 JSON 类型名。
- 不同 handler 仍可能写入同一个底层 registry（例如 blocks 和 bogey block registrations），应由 handler 声明目标注册表或声明有效 ID 域。
- v1 无隐式 last-wins 注册覆盖，不支持 `override: true`。
- 内容跨 pack 引用必须声明 pack dependency，并且依赖目标必须通过 schema 验证。

**资源覆盖与注册替换是两条不同机制。** 普通资源、reload 定义和启动期非注册配置都允许按资源优先级覆盖。`on_register` 不能整体被等同为不可覆盖：handler 必须声明是否产生注册副作用；`bogey_models` / `bogey_motions` 等非注册配置先选择有效定义，再解析引用和构建。创建方块/物品的启动定义仍分别参与 registry claims 检查，不能因资源优先级静默替换另一包的注册项。**v1 不考虑同名方块/物品注册，也不通过覆盖其启动定义绕过此限制。** 混合文件按类型路由，不能整文件覆盖时一起丢掉注册项。

### 依赖解析

- 依赖图按 pack ID 建立；检测缺失、禁用、版本不匹配和环。namespace 重复是合法状态，不作为依赖错误。
- 启用 pack 的必需依赖必须在有效加载集中；玩家禁用了必需依赖时，依赖它的 pack 一并标记为不可加载，并解释原因。
- “依赖”统一指必需项：缺少任何声明项就不能正常启动该包。v1 不实现可选依赖、可选联动或缺项安全降级分支。
- 内容引用仅允许本包及 manifest 显式声明的依赖。A 依赖 B、B 依赖 C 不代表 A 可以引用 C；A 必须直接声明 C。共享 namespace 不能作为绕过依赖校验的依据。
- 不要仅以“mod ID 存在”表示兼容；为 Create、KuaYue 及提供自定义 schema 的 mod 声明可校验的版本范围。

### 普通资源

- 模型、纹理、声音、语言、UI 主题、配方、tag、loot table 等优先使用 Minecraft/NeoForge 的标准资源路径和资源重载机制。
- 一个包可提供多个 namespace，多个包可提供同 namespace；同逻辑路径按显式资源栈顺序覆盖，包括对 corepack 的覆盖。
- 玩家可在游戏内调整包覆盖顺序，写入全局 `packs.order`，重启后生效。依赖拓扑顺序不覆盖玩家资源优先级。
- 普通单资源读取取最高优先级定义；语言、tags 等需要堆栈合并的类型沿用 Minecraft 消费者语义，不对任意 JSON 发明深合并。

## 8. 启用状态、保存兼容和多人

### pack 级启用配置

统一配置 `<game-dir>/kuayue/config.json` 的 `packs` 子域保存实例级启禁用、覆盖顺序和安装清单，不另建开关/管理配置文件。**世界历史不属于全局配置，使用该存档的 SavedData `.dat` 文件**，见第 24.4 节。游戏内顺序设置提示重启生效，不能产生 parcel/存档开关或即时改栈。

`packs.enabled` 是当前游戏实例唯一的用户开关来源。所有存档共享一次启动所确定的有效包集合；禁止为世界添加开关、读取存档专属包配置或在切换存档时变更包集合。专用服务器同样在整个服务器进程层面固定包集合。

建议区分：

- **已安装**：目录存在且能读到 `pack.json`。
- **启用请求**：玩家配置希望加载该 pack。
- **有效加载**：格式、依赖、namespace 和内容全部通过验证。

启动报告显示三者差异，避免“目录存在但没有生效”造成误判。

### 世界兼容

- 世界记录存于主世界的 `SavedData`，包含历史包 ID、声明版本和格式版本，不含开关/hash。文件随存档移动/复制，无需用全局路径或世界 UUID 查找历史；首次缺少记录的处理仍待明确。
- 世界历史只是与全局有效包集合进行比较的兼容性记录，不是存档启用配置；检查失败只能阻止载入并提示用户调整全局配置、重启，不能修改本次包集合。
- 打开世界时检查缺包及同包升级；新版本通过 `save_compatibility` 声明可升级的历史版本。合法升级不要求与旧版本 hash 相等，不自动串联多个版本的兼容声明。
- 有明确历史版本时使用作者的 `save_compatibility` 声明。同版本内容变化不限制；缺包和无历史的存档处理策略尚未确认，不将早期严格阻止建议视为用户决策。
- 禁用 pack 不会自动清除世界里已经放置的该 pack 方块；用户界面必须对此做清晰提示。

### 多人

- 车包分发由作者/服主管理，客户端和服务器自行安装，KuaYue 不提供多人车包下载、文件同步或自动换包服务。
- 双方可交换 ID、声明版本和格式元信息供诊断及基本环境检查；不对同版本的内容差异作限制，不用 hash 做强一致性连接阻断。连接时缺包/声明版本差异如何处理，应与游戏既有 registry 协议和后续兼容规则对齐，而非实现字节同步。
- 连接服务器不自动改客户端开关、加载来源或存档设置；需要不同集合时，用户修改实例级配置并重启后再连接。
- v1 不从服务器自动下载 ZIP，也不允许客户端只加载部分 parcel。

## 9. Corepack 迁移策略

1. 先盘点当前 KuaYue 的模组内置定义与资源，将其视为 `kuayue-core` 的逻辑内容；资源 namespace 保持 `kuayue`。
2. 作者资源可以继续分布在现有 Gradle resource-compiler 源模块中；最终运行产物必须把 `kuayue-core` 专属资源隔离，不能让 JAR 根自动暴露已禁用的包内容。内嵌资源根方案见第 23.4 节，其输出方式由 E0 验证。
3. 通过统一的内容来源接口把这些 JAR 资源注册为内置 pack，确保内置与外部来源经过相同 schema、ID 和引用验证。
4. 保持现有 `kuayue:*` 内容 ID，避免破坏存档和其他模组引用。
5. 当 corepack 路径可用后，分批验证外部车包：先 M1/简单车型，再一个简单 bogey/track，最后 QJ 等复杂动画模型。
6. `kuayue-core` 的开关与覆盖优先级属于实例级配置，默认启用，可禁用、可在游戏内移动顺序，重启后生效。禁用时保留 runtime；依赖其内容的包被阻断，不自动重启用。自身版本升级也使用历史版本兼容声明。
7. 不能仅跳过 corepack 的注册索引：其模型、配方、标签、语言和动作必须从默认资源视图中隔离，避免 JAR 自动资源发现绕过开关。具体资产归属和验收见第 23 节。

## 10. 数据与蓝图范围

v1 文件格式与运行时都不包含科技树、科技解锁条件或科技节点生成；pack/parcel manifest 不提供这些字段，开发任务与验收不纳入它们。

长期愿景中的“蓝图”需要与科技树分开定义：最初设想它是可拼装的树形数据结构及子区块，也可能用于科技树；这两种用途不能直接等同。

- 如果蓝图是车型/部件的物理拼装描述，它属于车型内容模型，可独立于科技树设计；只有 KuaYue 已有实际消费者并能定义稳定运行时行为时，才纳入 v1 必需内容。
- 如果蓝图仅是科技树节点/解锁内容，则整体延期。
- 不要因为未来可能自动生成物品栏、目录或科技树，而把这些 UI 派生结果写成 pack 的必填字段。

车型名称、简介、分类和图标可以在 v1 用于浏览/展示，但它们只是元数据，不引入解锁语义。

## 11. 分阶段实施路线

### 阶段 A：协议与现状基线

- 固定 pack/parcel ID、namespace、`format_version`、依赖及错误行为。
- 固定 v1 内容类型清单、多 namespace 规则、资源覆盖顺序和注册替换边界。
- 用当前 KuaYue JSON 做迁移清单和 golden tests。
- 将测试和运行日志计数从“文档示例”变为对实际资源来源的可重复核验。

**完成标准：**协议文档、JSON schema、解析器测试对格式边界没有歧义；明确重启生效，且没有 parcel 启用字段或科技树字段。

### 阶段 B：Kasuga 内容来源抽象

- 将 `JsonTreeBuilder` 对 `IModFile` 的直接读取抽象为内容来源。
- 保持当前 mod JAR 加载行为不变。
- 增加可在测试中使用的目录来源，验证相同索引和内容可被解析。
- 保留现有 handler 顺序、EffectiveId、diagnostics 和旧内容兼容行为。

**完成标准：**所有现有 Kasuga 单测通过；同一 fixtures 从 JAR 来源和目录来源得到相同解析结果。

### 阶段 C：KuaYue 外部 pack 扫描与预检

- 从 `<game-dir>/kuayue/packs/` 发现整包，并实现 `<game-dir>/kuayue/config.json` 的游戏实例级 `packs.enabled` 和管理清单；无 parcel/存档开关。
- 增加元数据和依赖检查、schema 校验、全局注册 ID 检查、内容引用检查。
- 建立加载报告及 `/kasuga_data` 或 KuaYue 专用诊断的职责边界。
- v1 同时处理目录和 ZIP；增加游戏内资源顺序设置，不实现自动下载或即时改包。

**完成标准：**缺字段、重复 ID、路径越界、缺依赖、格式版本不支持等错误都有稳定测试和用户可读报告；配置只有一个实例级开关入口，ZIP 样例根直接包含包内内容。

### 阶段 D：统一注册与资源挂载

- 在 KuaYue 注册分发前汇入已启用的 `kuayue-core` 与有效外部 pack，形成整个游戏实例共用的启动快照；默认包禁用时不能回退原 JAR 扫描。
- 注册内容统一经过 handlers，保留显式 namespace 和真实 owner；普通资源按玩家覆盖顺序组成栈，不以 namespace 唯一性拒绝包。
- 为客户端和服务器资源管理器提供同一启用 pack 集的普通资源。
- 验证一个 pack 的内容错误不会造成未说明的部分注册。

**完成标准：**单一测试 pack 可新增一个完整车型；禁用它并重启后注册内容和资源都消失；与 corepack 冲突时有稳定拒绝行为。

### 阶段 E：迁移与向后兼容

- 将现有内置车型作为 corepack 经过同一管线加载。
- 逐批比较旧注册与新注册的 ID、属性、模型部件、动作和外部资源引用。
- 保持存档使用的 `kuayue:*` ID 不变。

**完成标准：**corepack 测试覆盖现有代表性车型、bogey、track 和餐食；已有内容数与迁移清单一致。

### 阶段 F：世界和多人一致性

- 使用主世界 SavedData 在 `data/kuayue_packs.dat` 记录声明版本历史，不用全局世界索引、不保存内容 hash；首次缺记录策略确认后接入初始化。
- 验证作者声明允许/拒绝的版本升级、同版本改内容不阻断、全局开关和顺序的行为；多人不提供下载/同步。缺包及无历史存档策略需在确认后补测试。
- 验证服务器/客户端 pack 清单比较及错误消息。

**完成标准：**不能无提示地用缺失 pack 的环境载入依赖其注册 ID 的世界；切换存档和多人连接不修改当前有效包集合。

### 阶段 G：BlockBench 插件导出

- 插件读取 pack manifest 和 mod 发布的静态 schema 描述。
- 能创建/选择 pack 和 parcel、填写元数据、编辑模型组件和内容字段；parcel 仅用于组织，不输出启用开关或科技树。
- 导出到暂存目录，运行相同验证器，成功后原子发布完整目录。
- 导出 ZIP 时根直接包含 `pack.json` 和包内资源，不打入外层车包文件夹；安装目录名不写成 pack ID。
- 插件不直接修改 Gradle 源码树或游戏正在运行的 registry。

**完成标准：**插件生成的包和手写示例包可由同一加载器加载；导出失败不留下半成品 pack。

## 12. 验证矩阵

至少覆盖以下用例：

| 场景 | 预期结果 |
|---|---|
| 无外部 pack | 与当前 corepack 行为一致 |
| 一个有效 pack | 注册其内容并加载其资源 |
| 同内容的目录包与 ZIP 包 | 加载结果与逻辑内容指纹一致；无需解压 ZIP |
| `indexes: []` 的纯原生资源 parcel | 资源可见，不要求 on_register，不产生 registry claims |
| 只有 on_reload 的自定义资源 parcel | 解码已提供的 schema，运行时 bucket 更新，不创建方块/物品 |
| 游戏内调整覆盖顺序（含 corepack） | 本次快照不变；重启后高优先级资源生效 |
| 多包共用 namespace / 同包多 namespace | 合法加载；资源按显式顺序覆盖 |
| pack 被禁用 | 下次启动不注册、不挂载其资源 |
| 修改启用配置但不重启 | 当前运行时不变，并明确提示重启 |
| 在同一游戏实例切换存档 | 有效包集合不变；只执行对应世界的历史兼容性检查 |
| 存档历史要求不同包集合 | 不自动切换、不局部启禁用；不兼容时阻止载入 |
| 不支持的 `format_version` | 拒绝整个 pack，报告支持范围 |
| 依赖缺失或版本不符 | 拒绝该 pack 和依赖它的 pack |
| pack 依赖环 | 拒绝环中相关 pack，报告依赖路径 |
| 重复 pack ID / registry ID | 按身份/注册冲突处理；与普通资源覆盖分开 |
| 世界使用旧包版本，新包声明兼容 | 允许升级，成功保存后更新历史声明版本 |
| 同包同版本、文件内容不同 | 不限制存档载入或多人连接，作者承担兼容责任 |
| 安装新外部包但未显式启用 | 默认不加载；玩家可在首次启动前手动写 enabled 配置 |
| 新包未声明兼容旧版本或尝试降级 | 阻止正常载入，不自动改写历史 |
| 内容引用不存在 ID | 预检拒绝，错误指向引用文件和字段 |
| `../`、绝对路径或越界符号链接 | 拒绝访问，不读取 pack 根目录外内容 |
| 一个坏 pack 加一个好 pack | 好 pack 仍按策略加载；坏 pack 无部分注册 |
| corepack 与现有内容对照 | ID、属性、bogey/track 关联及资源结果一致 |
| corepack 禁用且无外部包 | 正常启动并能创建空内容测试世界，无默认内容泄漏 |
| corepack 禁用且有独立外部包 | 通用工厂、创造栏、模型与共享 BE 仍可用 |
| corepack 禁用且有依赖它的外部包 | 外部包被阻断，不隐式重启用 corepack |
| 世界缺少历史包或没有历史记录 | 处理策略待进一步确认；不按 hash 判断，不擅自删内容或改变全局集合 |
| 多人车包内容不同或没有分发副本 | 不触发 KuaYue 下载/文件同步，不按 hash 阻断；提供必要元信息诊断 |

## 13. 主要风险和待讨论项

1. **注册原子性：**现有 handler 的 `apply` 可能创建 deferred registrations 或写 metadata。必须厘清可在哪个边界预检和提交，不能承诺无法实现的运行时回滚。
2. **注册来源身份：**第三方 pack 内容不能全部以 `kuayue` 作为 ID owner，否则默认 ID 归一化会造成命名空间冲突。
3. **引用与覆盖实现：**仅显式依赖可引用，启动期非注册配置可覆盖；按 handler 的定义类型选择候选并保留真实 owner，不能因文件混合包含注册项而误替换 registry 定义。
4. **资源分发：**确认 NeoForge 的启动/资源包发现时序，确保磁盘目录在相应客户端/服务器资源 reload 前加入；普通资源的可见性不等同于启动期注册可用性。
5. **存档风险：**禁用 pack 可能令世界中现有方块缺失。pack 管理界面必须警告并提供世界兼容保护。
6. **Schema 发布：**KuaYue handler parser 与插件 schema 可能漂移；需要 schema version、回归测试或由同一描述生成两侧实现。
7. **蓝图含义：**在无科技树的 v1 中确认 blueprint 是否有独立的列车拼装运行时用途，否则延后。
8. **运行环境实现：**专用服务器入口与基本环境诊断仍需技术验证；包分发明确由作者/服主负责，平台不实现下载同步和内容 hash 强一致性检查。

## 14. 当前实现作为迁移基线

当前源码树已经有可复用的 KuaYue 数据定义：约 10 个车厢索引、bogey 索引、track 索引和 provisions 索引；文件清点包括 201 条车厢方块定义、5 条轨道，以及 53 组 bogey size/block/model 等数据。这里是源码树清点，不等同于已经验证过的最终运行时加载数量。

已有回归测试集中在 `modules/train-parts/src/test/...` 的 bogey、model/motion 和 track schema 测试。实施前应先跑这些测试并保存构建/游戏启动基线；迁移后再比较同一批 ID 和行为，避免只验证“JSON 能 parse”而漏掉实际注册和资源装配。

## 15. v1 两个项目的职责边界

### KasugaLibNext 提供通用平台能力

KasugaLibNext 不定义车包格式，也不认识车型、parcel、corepack 或 pack 启用配置。它负责让已有数据驱动 handler 能安全地从不同来源构建注册内容：

- 把 JAR、外部目录等抽象为内容来源。
- 在一个构建会话中合并多个来源，统一解析、预检、排序和生成注册树。
- 把来源身份、资源 namespace、宿主 mod 身份区分开。
- 提供无副作用验证阶段、跨 handler 的最终注册 ID 冲突检测和结构化诊断。
- 为需要全量数据的消费者提供一次性的构建完成钩子。
- 保持客户端/服务器资源管理器消费这些来源所提供资源的能力。

### KuaYue 提供车包业务

KuaYue 负责具体的车包语义与启用策略：

- 定义和校验 `pack.json`、`parcel.json`、版本、namespace 和依赖关系。
- 扫描 `<game-dir>/kuayue/packs/`，从 `<game-dir>/kuayue/config.json` 读取实例级整包开关，并在启动注册前固定本次有效清单；不读取 parcel/存档专属开关。
- 选择 corepack 和外部 pack 的内容来源，调用 Kasuga 的统一构建 API。
- 实现 bogey、track、车型等 KuaYue 专属 handler 与 cross-reference 校验。
- 定义车包冲突、错误隔离、存档保护及服务器/客户端 pack 指纹策略。
- 将同一份有效 pack 集合接入普通客户端资源和服务器数据资源。

### v1 明确不拆给 Kasuga 的职责

- Kasuga 不扫描 KuaYue 目录、不读取 KuaYue 的启用配置、不递归解析 pack 依赖。
- Kasuga 不负责下载、安装、启用/禁用车包。
- Kasuga 不替 KuaYue 决定冲突时禁用哪些包；Kasuga 提供冲突事实与诊断，KuaYue 决定策略。
- 插件暂不参与启动注册；后续由 KuaYue/Kasuga 发布机器可读 schema，插件按 schema 生成数据。

## 16. KasugaLibNext v1 必需底层改动清单

以下项目是支持 KuaYue v1 车包所需的最小通用能力；API 名称仅为方案示意。

### 16.1 内容来源 API

把 `JsonTreeBuilder` 直接依赖 `ModList` / `IModFile` 的文件读取改为来源接口。建议概念接口包含：

```java
interface ContentSource {
    SourceDescriptor descriptor();
    Optional<InputStream> open(LogicalPath path);
    boolean exists(LogicalPath path);
}
```

`SourceDescriptor` 至少携带来源 ID、owner、namespace 集合和诊断元信息。v1 必须实现 `ModJarContentSource`、`DirectoryContentSource` 和 `ZipContentSource`；网络来源延期。

来源接口的路径解析必须拒绝绝对路径、`..`、非法分隔和逃出来源根目录的路径。原有 mod JAR 路径行为应由 JAR adapter 保持，不要求调用者改写所有已有内容。

### 16.2 多来源构建会话

提供面向多个来源的统一构建入口。设计上区分“准备”和“构建”：

```java
PreparedContent prepare(Collection<ContentSource> sources, BuildOptions options);
BuildResult build(PreparedContent content, RegistrationTarget target);
```

其中 `prepare` 负责发现索引、解析、校验和汇总冲突；`build` 只处理准备成功的数据，并按 handler phase 构建注册树。

- 全部定义进入同一构建会话，不能为每个 pack 单独触发注册。
- 全局按 handler phase 排序，父分组、内容、内嵌定义仍按既有生命周期执行。
- 保留来源顺序和每条定义的来源元数据，避免只剩一个合并后的 JSON 树。
- 现有 `buildForMod(String)` 保留为兼容包装，内部适配到新入口；现有消费者不应被迫立刻迁移。
- KuaYue 传入 source 集合与冲突策略；Kasuga 不读取车包配置。

### 16.3 身份与构建上下文

当前 `BuildContext.getModId()` 不能同时代表宿主模组、内容来源和注册 namespace。新会话需分别表达：

- `hostModId`：例如 `kuayue`，用于模组生命周期及宿主关联。
- `sourceId` / owner：例如某个外部 pack，用于诊断与冲突归属。
- `namespaces`：来源提供的资源 namespace 集合，不是 owner；当前定义的 namespace 从显式 ID/入口路径解析，不再依靠单包默认值猜测。
- 当前 handler/type 与内容文件定位。

第三方内容 ID 必须使用显式 namespace。构建上下文允许 handler 读取定义 owner、查找跨文件 metadata，并将结果记录到正确来源；不得把所有包都伪装成 `kuayue` 模组本身。

### 16.4 无副作用的 handler 预检

当前 `TypeHandler` 有 `parse`、`apply`、`resolveIdentity`，引用错误可能直到 `apply()` 才出现；部分通用 `RegTypeHandler.apply()` 会捕获异常并只记录 WARN。pack 需要整包校验，因此建议增加兼容性的预检扩展点：

```java
default void validate(T definition, ValidationContext context) {}
```

- `parse` 只解析并验证单条字段形状。
- `validate` 在注册树和 deferred registration 产生前验证引用、ID 和约束。
- `apply` 仍创建既有注册对象，但不承担“这是唯一一次验证”的职责。
- 通用 handler 校验 factory/type、属性、registry group；KuaYue handler 校验 bogey 尺寸、style/model/motion、track material 等业务引用。
- 聚合诊断应关联源文件与 JSON 字段路径；允许一个文件内的独立错误继续诊断，但错误 pack 不得进入提交集合。

Kasuga v1 不需要承诺任意注册副作用的运行时回滚。正确边界是尽可能在副作用前完成预检；若最终提交阶段发生无法隔离的内部错误，应中止启动或拒绝最终注册，而不是报告该包“部分成功”。

### 16.5 跨 handler 注册占用声明

现有按 `handler type + identity` 去重不足以发现底层 Minecraft registry 冲突：`blocks` 与 KuaYue 的 `bogey_blocks` 等不同顶层类型可能同时产生 Block。

建议允许 handler 声明每个定义将占用的注册目标：

```text
registry key + effective ResourceLocation + owner/source
```

单个定义可声明多个占用项，例如 block 和随 block 创建的 item。Kasuga 全局检查 registry claims，KuaYue 按注册策略处理重复；资源优先级已在 v1 实现，但不自动赋予 registry claim 的 last-wins 语义。

### 16.6 构建完成钩子与批次聚合

提供一次性的 `afterBuild` / build-finalizer 机制，让消费端在全部相关 handler 完成后处理跨定义共享对象。

这对现有 bogey 场景很重要：`BogeyRuntimeRegistration.ensure()` 需要收集所有 `BogeyBlockReg`，只创建一个共享 block entity registration，并设置完整的 `validBlocks`。逐 pack 独立构建可能造成共享对象重复创建或有效方块列表不完整。

Kasuga 只提供生命周期 hook 和统一 `BuildContext`；共享 Bogey BE 的具体构建仍归 KuaYue。该 hook 必须只调用一次，并能报告 finalize 失败的来源。

### 16.7 冲突策略、结果对象与诊断

Kasuga 负责产生稳定的检查结果，不固定车包策略。建议结果可以表达：

- 成功解析/预检的定义及来源。
- 无效定义和关联诊断。
- 按最终 registry ID 汇总的冲突及其参与来源。
- 可以继续构建的内容集合和阻断原因。

诊断至少包含 `sourceId`、pack/parcel（由调用方提供）、文件路径、JSON 路径、handler 类型、错误类别和建议。已有 `/kasuga_data errors` 可复用为输出设施，但是否把外部包错误并入同一个命令，应明确 owner、过滤维度和生命周期。

车包策略：内部阻断错误按整包拒绝；重复注册 ID 按当前注册冲突策略处理；普通资源允许覆盖 corepack 或其他包，重复 namespace/资源路径本身不是包错误。诊断分别给出注册冲突与资源覆盖来源，不能混为一类。

### 16.8 普通资源的消费适配

Kasuga v1 不另写模型/纹理/配方加载器，但要验证其已有消费者能从外部目录提供的资源中读取文件。

- 优先复用 NeoForge 原生 resource/data pack 发现和 reload 流程。
- 若外部目录通过自定义 `PackResources` 或组合来源进入，确保 `ScopedResourceManager` 可适配该类型，且 reload 后 listener 能重新读取。
- `ReloadOrchestrator` 继续从正常 `ResourceManager` 读取 `on_reload` 内容；它不直接扫描 KuaYue 的 pack 根目录。
- 注册期资源输入仍由 `ContentSource` 读取，不能假设 `ResourceSystem` 的运行时资源监听能代替它。

## 17. KuaYue v1 必需业务改动清单

1. **Pack manager：**扫描直接子目录和 ZIP，读取整包开关与资源顺序，生成不可变快照；实现游戏内顺序调整，切换存档和服务器不重新选包。
2. **协议与依赖：**实现 manifests、多 namespace、必需依赖图和自身存档升级兼容声明，及两种来源的安全校验。
3. **来源适配：**将 corepack JAR 资源和有效外部目录转换为 Kasuga `ContentSource`，并提供 parcel、pack 元信息供诊断。
4. **业务校验：**为 bogey、track 及车型补足无副作用 cross-reference 校验；定义各 handler 占用的底层 registry key。
5. **共享注册聚合：**改造 `BogeyRuntimeRegistration`，在完整 KuaYue 数据构建后统一创建共享 block entity 和有效 block 集合。
6. **注册与资源同源：**注册内容、客户端模型/纹理/语言和服务器 data 资源必须由同一个 effective pack set 派生。
7. **资源优先级：**按用户顺序覆盖资源，corepack 可排序；注册冲突单独报告，不用资源覆盖权限绕过重复注册。
8. **版本兼容平台：**通过主世界 SavedData 读取该存档的声明版本历史，不判同版本字节差异、不区分包类别。无需全局世界身份映射；首次缺记录策略待明确，检查不改启用集。
9. **多人环境：**下载和文件分发同步由作者/服主管理；不以 hash 强制一致。同版本修改的责任归作者。
10. **corepack 迁移：**内置名/ID 为 `kuayue-core`，保留现有 `kuayue:*` 内容 ID；支持实例级整包禁用并隔离资源，确保只有一条注册管线。

## 18. v1 延后能力

- 网络来源和在线安装器。ZIP adapter 和直接扫描 ZIP 属于 v1，不延期。
- Kasuga 通用 pack manager、通用依赖求解器或通用启用配置系统。
- 注册提交后的全面 rollback、运行中热注册/卸载。
- parcel 或存档专属启禁用、切换世界/连接服务器时自动改包。这些是 v1 明确禁止项，不是尚未实现的开关入口。
- 重复 registry ID 的替换/迁移机制。外部包资源覆盖 corepack、优先级界面和纯资源覆盖包属于 v1。
- KuaYue handler 的任意 Java/JS 代码上传。
- 任意 mod 的自动 schema 反射发现；改为后续静态 schema 描述与插件支持。
- 科技树、解锁条件、研究进度和科技节点生成。
- 蓝图若没有独立的现有运行时消费者，也不作为 v1 pack 加载阻塞项。
- 服务器自动传输 pack、云市场、在线校验服务。

## 19. 建议的实现顺序与跨项目验收

1. **先做 Kasuga `ContentSource` + JAR adapter，不改默认行为。** 验收：现有 JAR fixtures 的解析结果、diagnostics 和注册结果不变。
2. **做统一会话与目录/ZIP adapters。** 验收：同一 fixture 从三种容器得到相同解析结果；ZIP/目录逻辑 hash 一致，多来源 registry claims 仍能发现重复。
3. **做 handler 预检、占用声明、构建完成 hook。** 验收：错引用在 apply 前被捕获；不同 handler 的同 registry 冲突被识别；hook 只执行一次。
4. **KuaYue 做 pack manager 和 manifest validator。** 验收：安装扫描和 ZIP 根结构符合第 4 节；开关以 pack ID 保存到 `kuayue/config.json`，实例级加载且重启生效，没有 parcel/存档 toggle 或科技树字段。
5. **接入外部 M1 测试 pack。** 验收：新增一组方块和资源、pack 禁用后消失、无效 pack 不造成其他有效包部分加载。
6. **接入 bogey 与 track pack 内容。** 验收：共享 BE 一次注册，跨 pack dependency 引用、模型/动作/轨道引用均被验证。
7. **做优先级界面和 corepack 接入。** 验收：内置包默认启用、可禁用、可重排，四种启动/覆盖环境有效；registry ID 不变，资源按冻结顺序选择。
8. **做升级和多人测试。** 验收：SavedData 随存档持久化，复制/改名不丢历史；声明升级正确，无 hash/顺序阻断、无多人下载同步。首次无历史策略另行确认。

插件 schema 和导出排在上述运行时闭环稳定之后，不应阻塞手写测试 pack 的 v1 验收。

## 20. 面向 agent 的执行规则与决策状态

### 20.1 契约优先级

- 已确认规则还包括启动期非注册配置覆盖、仅显式依赖可引用、外部新包默认禁用、同版本内容变化不限制、所有车包统一策略和不提供多人下载同步。agent 遵守对应章节，不重新标为待确认。
- 第 21–25 节提供**建议执行基线**，补齐格式、SPI、失败处理和任务细节。确定规则与技术建议不能互相混淆；前后重复内容必须保持一致，不能依赖后文覆盖前文矛盾。采用本基线时，agent 不得自行发明另一套格式。
- 世界历史存储已确定使用存档自身的 Minecraft SavedData，不需要全局世界 ID/路径映射。尚未确认的是首次缺历史的处理及基本缺包/版本诊断入口。全局配置不记录世界历史；hash 不作兼容门槛。
- 第 26 节列出的技术验证是实施任务，不允许用猜测替代。尚未验证的 NeoForge 时机不能作为“已完成”的依据。
- 后文的包名和类型名为计划新增项，不代表当前代码已经存在。允许调整内部实现命名；公开文件格式或 SPI 若需变更，必须同步契约、schema、示例和测试。

### 20.2 开发方式

每个任务必须包含输入、输出、依赖、修改范围、测试和完成证据。先实现纯解析/校验，随后接入注册和资源生命周期；禁止以“能编译”替代真实加载验收。

本工作目录有独立的 KasugaLibNext、KuaYue 和插件仓库。不得假设父目录是一个 Git 仓库。默认只修改当前任务所属仓库；跨仓库任务要分别记录变更。插件不属于运行时 v1 的首批修改范围。

## 21. 文件格式与资源映射契约（建议执行基线）

### 21.1 安装与配置

- 外部安装目录：`<game-dir>/kuayue/packs/`。一个直接子目录或一个直接子 ZIP 文件就是一个 pack；按扩展名不区分大小写发现 ZIP，不递归寻找嵌套安装包、不运行 Gradle resource compiler。
- ZIP 根直接包含 `pack.json`、`parcels/`、`assets/`、`data/` 等实际内容，不加外层车包文件夹。游戏直接读取 ZIP，不解压到作者目录；文件夹名/ZIP 文件名不决定 pack ID。目录与 ZIP 同时声明同 pack ID 时为重复安装，不能用优先级代替身份冲突检查。
- 实例配置入口为 `<game-dir>/kuayue/config.json`，采用 JSON，`packs` 子域只保存实例级开关、资源顺序和安装清单。世界版本历史单独用 Minecraft SavedData 保存在 `<world>/data/kuayue_packs.dat`，不保存到全局 `packs.worlds`，也不新建手工世界 JSON。
- K1 实现实例配置，S1 实现世界 SavedData。E0 验证配置读写和服务器主世界数据存储的获取/保存时机；存储归属及方式不再待选。
- corepack 名称/ID 为 `kuayue-core`，从 JAR 发现，不复制到安装目录；和外部包共同出现在可调整的资源优先级列表中，无强制最高优先级。
- 默认配置：外部包缺省禁用，只有 `packs.enabled[id] == true` 才请求加载；缺省 `kuayue-core` 启用。首次发现包不能自动启用依赖。玩家可在安装新包后的首次启动前按 manifest ID 手动编辑配置；扫描只补安装清单，不悄悄改用户开关。
- `kuayue/config.json` 缺失或 `packs` 子域缺失时使用缺省值；已有文件 JSON 损坏、`packs` 类型或格式版本错误时阻止本次启动，不能静默回退并意外启用被禁用的包，也不能覆盖损坏文件。

```json
{
  "packs": {
    "format_version": 1,
    "enabled": {
      "kuayue-core": false,
      "example-railway": false
    },
    "order": ["kuayue-core", "example-railway"],
    "inventory": []
  }
}
```

上例是实例配置片段，顶层无关子域保留。`enabled`、`order` 是用户设置，`inventory` 是安装清单；世界历史不放进此文件。未知安装 ID 保留并警告，不自动下载，保存清单不覆盖用户开关。

`packs.order` 从低到高优先级，界面顶部显示最高层。技术补全默认值仍为 corepack 最低，其余 ID 稳定补齐；**排序中出现某 ID 不代表启用**，未显式启用的新包仍禁用。不存在/禁用 ID 可保留但不进入有效栈；依赖求解不能重排用户优先级或自动启用依赖。

v1 必须提供游戏内顺序设置界面：列出内置/外部包及状态，支持上移/下移，保存到统一配置，并标明“重启后生效”。当前快照与待生效配置分别显示；界面不得直接执行 reload 或重新注册。

`packs.enabled` 只有一份实例级映射。SavedData 世界历史不允许包含 `enabled`、`disabled` 或覆盖顺序，不参与构建启用集合。不能把它当作进入该世界时要加载的包设置，也不能用世界级/server 配置覆盖实例开关。

ZIP 直接读取为 v1 必需实现，与目录来源提供相同逻辑路径；ZIP 生命周期、重复 entry、压缩炸弹和路径安全测试不能延期。

### 21.2 Pack manifest

计划发布 `KuaYue/doc/schemas/packs/v1/pack.schema.json` 和配套 `parcel.schema.json`、`kuayue/config.json` 的 `packs` 子域 schema/约束说明；这些是开发文档，不是额外的运行时配置文件。Java parser 与 schema 必须接受/拒绝同一批 fixtures。

```json
{
  "format_version": 1,
  "id": "example-railway",
  "version": "1.0.0",
  "namespaces": ["example_railway", "kuayue"],
  "name_key": "pack.example_railway.name",
  "description_key": "pack.example_railway.description",
  "dependencies": {
    "mods": {
      "minecraft": "1.21.1",
      "neoforge": "[21.1.203,22.0.0)",
      "kuayue": "[2.0.0,3.0.0)",
      "kasuga_lib": "[1.0.0,2.0.0)",
      "create": "[6.0.6,7.0.0)"
    },
    "packs": {}
  },
  "parcels": ["parcels/m1/parcel.json"],
  "save_compatibility": {
    "minimum_version": "0.2.0"
  }
}
```

字段契约：

- `format_version` 必填，整数且等于 `1`；未知版本拒绝，不能尝试按 v1 猜读。
- `id` 必填，采用稳定包名字符串，建议匹配 `[a-z][a-z0-9_-]*`；与资源 namespace 独立。内置 ID 固定为 `kuayue-core`，不允许外部包声明。此规则取代早期的 ResourceLocation 形状 pack ID，parcel/游戏内容仍按各自 ResourceLocation 规则处理。
- `version` 必填，pack 采用 SemVer 2.0.0；v1 的发布包只接受无 prerelease/build suffix 的 `major.minor.patch`。corepack 版本按同样规则生成。
- `namespaces` 必填，是非空且包内无重复的合法资源 namespace 数组（匹配 `[a-z0-9_.-]+`）。允许多个值，也允许不同包共享同一个值；`kuayue`、`minecraft` 等可用于普通资源覆盖，不存在排他 namespace 所有权。corepack 提供 `kuayue`；外部包提供同 namespace 不改变 corepack 身份。
- `name_key` 必填、`description_key` 可选，均为非空翻译键。客户端缺少翻译时回退为原键，诊断警告，不阻止专用服务器启动。
- `dependencies` 必填，包含 `mods`、`packs` 两个映射；v1 只支持必需依赖，不提供 optional 或条件联动字段。
- 外部包必须显式声明上述五个基础 mod 的兼容范围；版本使用运行时元数据值，不使用文件名。corepack 使用内置宿主兼容描述，不依赖自身 pack。
- `parcels` 必填，是非空、无重复的相对路径数组。v1 要求每条路径是 `parcels/<folder>/parcel.json`；清单里没列出的 parcel 不加载。
- `save_compatibility` 可选，用当前包声明自身历史版本的存档升级入口；允许精确版本集合或最低历史版本，具体互斥字段及判断见第 24.3 节。不声明时不授权跨版本直接升级。
- manifest 和配置的 `packs` 子域拒绝未知键及错误类型；配置顶层其他子域保留。JSON 文件拒绝重复对象键、注释与尾逗号，不能让 Gson 最后写入覆盖重复键后再声称已经校验。
- 不提供 `enabled`（manifest 内）、`override`、`scripts`、`tech_tree` 或 parcel 开关字段。

依赖版本语法只支持：`*`、精确版本字符串、半开区间 `[lower,upper)`；上下界都必填，不支持 `^`、`~`、OR 或下载 URL。pack 比较器按上述三段 SemVer；mod 比较器复用当前 NeoForge 的版本比较实现，禁止自己写词典序比较。语法或版本无法比较时拒绝包。

### 21.3 Parcel manifest 与资源根

```json
{
  "format_version": 1,
  "id": "example_railway:m1",
  "name_key": "parcel.example_railway.m1.name",
  "description_key": "parcel.example_railway.m1.description",
  "icon": "example_railway:textures/pack/m1.png",
  "indexes": ["data/example_railway/kasuga_lib/data_driven/m1.json"]
}
```

- parcel 的 `id` namespace 必须出现在 pack 的 `namespaces` 中，在 pack 内唯一；没有独立 `version`、`dependencies`、`enabled` 或科技树字段。
- `name_key` 必填；`description_key`、`icon` 可选。`icon` 是图片资源位置（包含 `textures/` 和扩展名），不是 item ID。
- `indexes` 必填，允许空数组表示纯原生资源 parcel，无须创建 Kasuga 索引。自定义 reload schema 则列出含 `on_reload` 的索引；不能用空 indexes 省略 animation_clips 的入口。索引路径相对 parcel，位于任一已声明 namespace 的 `data/<namespace>/kasuga_lib/data_driven/` 下；内容路径按该索引 namespace 解析。
- pack 根和每个已列出的 parcel 根都可以有 `assets/<namespace>/`、`data/<namespace>/`。manifest 是作者元信息，不直接暴露为 Minecraft 资源。
- 对 pack 声明的每个 namespace，根下索引按 `(namespace, 文件名)` 升序发现；parcel 只发现 `indexes` 指定的索引。资源文件实际 namespace 必须在本包声明集合内；声明集合不要求其他包不使用它。

建议目录实例：

```text
example/
├── pack.json
├── assets/example_railway/lang/zh_cn.json
├── data/example_railway/kasuga_lib/data_driven/shared.json
└── parcels/m1/
    ├── parcel.json
    ├── assets/example_railway/models/block/m1/panel.json
    ├── assets/example_railway/textures/block/m1/panel.png
    └── data/example_railway/
        ├── kasuga_lib/data_driven/m1.json
        └── carriages/m1/blocks.json
```

**物理路径 → 逻辑路径：**去掉 pack 根或 parcel 根前缀，余下的 `assets/...` / `data/...` 原样成为游戏资源路径。例如 `parcels/m1/data/example_railway/carriages/m1/blocks.json` 映射为 `data/example_railway/carriages/m1/blocks.json`。普通资源和注册解析使用同一映射，不可分别猜测位置。

Kasuga 索引示例：

```json
{
  "on_register": ["carriages/m1/blocks.json"],
  "on_reload": []
}
```

上述内容路径依然相对**逻辑** `data/example_railway/`。多个 roots 先组成该 pack 的资源视图，索引可以引用本 pack 的共享内容。不能把 `parcels/m1/` 再拼到索引条目上；也不能通过索引跨包读取文件。

### 21.4 包内合并、安全与快照

- 包内技术默认顺序为 pack 根最低，再按 manifest 的 parcel 清单顺序从低到高组成一个 pack 视图；parcel 不是玩家可单独启用/排序的单位。普通资源在包内和包间都按该确定顺序处理。
- 多来源的同逻辑路径是合法覆盖，不拒绝整个 pack。普通 `getResource` 返回最高优先级文件；`getResourceStack` 按低到高保留各层，以供 Minecraft 语言、tag、sounds 等消费者执行自己的合并/replace 语义。不对任意 JSON 统一深合并。
- 同一个物理根或 ZIP 内重复 entry/重复文件是结构错误；同一路径来自不同包或已列出的不同 parcel 根则是有序覆盖。注册期索引/定义去重与 claims 检查仍使用各来源，不用资源顺序替换注册项。
- 可提供本包 `namespaces` 集合内任意 namespace 的覆盖资源。覆盖不等于引用 registry 定义；包若必需被覆盖目标的内容，必须显式声明依赖，否则目标不存在时覆盖文件仍可作为普通资源存在。
- 拒绝符号链接、绝对路径、反斜线、空段、`.`/`..` 段和非法资源路径；限制策略同时作用于索引读取和资源枚举。
- 建议限制：JSON 单文件 8 MiB、普通单文件 256 MiB、单包 1 GiB、单包文件数 50000；这些是可调技术默认值，超限报明确错误，不无限读取。
- ZIP 额外拒绝重复 entry、越界 entry、符号链接 entry 和非法根布局；限制以**实际解压读取的字节数**计，不能信任 ZIP header 中的大小。达到上限立即停止；关闭所有 archive handle，损坏/截断 ZIP 报告错误，禁止任意解压到安装目录外。
- 启动后资源来源也必须固定。建议将通过安全检查的有效文件复制到 `<game-dir>/kuayue/cache/packs/<content_hash>/` 的本次加载快照；缓存写完再发布，原安装目录在运行中变化不会改变本次资源视图。
- 缓存是派生产物，不存包启用状态，不覆盖作者目录；复用缓存前验证清单/摘要。v1 不做自动监控原目录、安装包自动重载或自动删除世界内容。

## 22. 底层 SPI 与构建执行契约

### 22.1 来源、符号和验证上下文

第 16 节的 `ContentSource.open/exists` 只是简化示意，无法单独完成资源枚举。正式 SPI 必须提供逻辑文件枚举或不可变文件清单、来源定位和指定入口索引，计划放在 Kasuga 的 `registration.data_driven.source` 包。

- `SourceDescriptor`：`sourceId`、`ownerId`、`namespaces`、诊断标签。owner 是包/来源身份，不是唯一 namespace；通用层不 import KuaYue 类型。索引 namespace 从逻辑路径获取，定义 ID 必须显式限定。
- `ContentSource`：受限逻辑路径读取、稳定文件枚举、入口索引集合。打开失败区分不存在、无权限、越界；调用者负责关闭流，绝不复用关闭后的 JAR 文件系统。
- `LocatedDefinition<T>`：已解析值、handler、来源、逻辑文件路径、JSON Pointer，例如 `/bogey_models/0/parts/2/motion`。
- `DefinitionKey`：类型域 + 归一化 ID。用来查找定义，不等同于最终 Minecraft 注册 ID。
- `RegistryClaim`：目标 registry/domain + 最终 ID + 定义来源。派生 item、style cycle 等也必须声明占用；不是只有顶层 `id`。
- `ValidationContext`：查询解析定义、代码预留符号、资源存在性、工厂能力，并提交诊断。它不含可以修改世界、注册表或运行时全局桶的方法。

校验不能依赖 `apply()` 写入的 `metaStore`：先解析所有定义，再建立只读定义目录，最后执行校验。必须区分“定义未通过校验”和“定义完全不存在”。

跨包引用仅对 manifest 显式依赖开放，不做传递可见性；用真实定义 owner/来源解析，不按共享 namespace 猜归属。自身定义可引用自身。覆盖提供一个同名资源与引用其他包内容是两件事；覆盖包实际需要目标内容时，作者必须显式声明依赖。

覆盖不得反向制造依赖：A 原本引用自己的逻辑资源/配置 ID，B 在高优先级提供替代，不要求 A 再依赖 B。引用权限检查基于该 ID 在本包/显式依赖中是否可见，再选择有效覆盖提供者；获选 B 定义内部的新引用仍按 B 自己的显式依赖验证。否则会把正常覆盖误变成 A↔B 依赖环。

### 22.2 严格模式与旧入口兼容

- 保留旧 `buildForMod()` 默认行为和测试，包括原来的重复处理、默认 namespace 和宽容诊断；不能为了车包改变所有消费端的行为。
- 新车包入口采用显式 `STRICT` 模式：工厂缺失、未知注册组、组继承环、错误属性形状、未知 handler 和必需引用错误均为阻断诊断，不静默挂根或跳过。
- 此处 STRICT 仅约束必需结构、引用和注册安全，不代表严格内容一致性；不能比较同版本包 hash 或对某类包施加额外兼容门槛。
- `TypeHandler` 的新方法可以有 default 实现以维持 Java 源兼容，但严格模式必须检查 handler 是否声明支持严格预检与 claims；缺少支持时报 `HANDLER_NOT_STRICT_READY`，不能把空 validate 当作已充分验证。
- 本期要给通用注册 handler 和 KuaYue 所用 handler补齐能力描述。不要调用测试源集的 factory 来充当发布环境支持。
- 非注册型 handler 可以合法声明空 registry claims；纯资源包没有启动定义也合法，不能以“零方块/物品”拒绝。不涉及 bogey 的会话不能强制创建共享 BE。启动期配置仍需要类型域内的定义 ID 和引用校验，空 claims 不代表没有身份或重复定义问题。
- 启动期非注册配置的重复 `DefinitionKey(type,id)` 按有效资源优先级选胜者，再对胜者校验引用、编译 metadata。没有获选的配置不能进入 apply；方块/物品 registry claims 不使用此覆盖策略。
- 引用预检分级：声明式强引用（group、size、model plan、motion、clip 等）必须启动前验证；模型/贴图内部由现有 loader 解析的依赖至少在实机 smoke 中验证。不能把“文件存在”报告为“完整模型已经可渲染”。

### 22.3 编排、拒绝与稳定性

建议让管线输出 `PreparedContent`（不可变定义、符号、诊断、claims）和 `BuildResult`（注册根、诊断、finalizer 结果）；整包选择由 KuaYue 做。

1. 基于配置/依赖收集候选来源，读取启动文件快照并解析全部定义。
2. 按 handler 分类：非注册配置选择最高优先级定义，注册型定义保留做 claims 检查；随后建立有效只读目录，按获选定义来源校验显式依赖和字段/引用。
3. KuaYue 拒绝有阻断错误的 owner，并拒绝依赖它们的 owner；从剩余集合重新校验引用，直到集合不再缩小。此过程**不能调用 apply**。
4. 对最终有效集合按 `(handler phase, handler 注册顺序, source 稳定顺序, 文件路径, 数组索引)` 构建；组按父关系额外排序。不能用磁盘枚举顺序作为稳定顺序。
5. 每会话执行一次 finalizer；生成共享对象的 claims 应提前可推导或由宿主预留，不能 finalize 后才发现重复 ID。
6. 一次性接入宿主注册树，随后沿原有 NeoForge 注册事件提交。发生 apply/finalize/commit 意外错误时本次启动失败；不丢弃个别定义后继续。

失败分类：

| 阶段 | 处理 |
|---|---|
| 外部 pack 元数据/依赖/预检失败 | 拒绝整包及依赖者，其他独立包继续 |
| corepack 被显式禁用 | 正常状态，不是加载错误；依赖者被阻断 |
| 已启用 corepack 自身预检失败 | 发行缺陷，本次启动失败，不悄悄视为禁用 |
| 外部包与 corepack/代码基础设施的 registry claim 冲突 | 按注册策略拒绝外部包；普通覆盖资源不走此错误分支 |
| 两个外部包重复 registry claim | 按注册策略拒绝双方；普通资源重复按优先级处理 |
| 多包 namespace 或普通资源路径重复 | 合法；按用户顺序提供覆盖/资源栈，无包级拒绝 |
| 构建、共享 finalizer 或注册提交失败 | 本次启动失败，不能承诺部分回滚 |
| 普通资源 reload 失败 | 沿游戏错误处理并报告；不能临时删注册或缩小有效包集合 |

依赖拓扑只用于准备/构建，依赖者在依赖之后，独立项按 ID 稳定排序。资源顺序独立使用 `packs.order`，依赖拓扑不得覆盖它。启动快照同时冻结有效 owner 集与资源顺序，不得重复 attach/commit。

### 22.4 宿主注册提供者与运行时资源入口

目前 `JsonTreeIntegration` 会自动调用 `buildForMod()`，并捕获顶层异常。新接口必须支持宿主为某 mod 注册**显式构建提供者**：

- 其他 mod 无 provider 时继续走旧入口。
- KuaYue 的 provider 接管 `kuayue` 的数据来源，返回有效包注册根；没有有效包时返回空根而不是回退到旧 JAR 扫描。
- provider 必须在首次 `KuaYue.REGISTRY.register()` 前安装，读取整个游戏实例的启动集合；存档加载与连接处理不得调用 provider 重选包。不能依赖不同 `@PostConstruct` 之间未说明的执行顺序；应通过显式 bootstrap 或注入依赖保证 happens-before。
- 严格 provider 的致命异常不得被原来只打印日志的 catch 吞掉。不能给 KuaYue 手动 append 一次，同时保留默认扫描再 append 一次。

注册提供者与资源发现读取同一 `StartupPackSnapshot` 的有效集合和冻结优先级。KuaYue 为 `CLIENT_RESOURCES` / `SERVER_DATA` 保留 pack 分层、单资源选择和资源栈读取能力；Minecraft 默认资源包界面不得绕过全局开关或即时重排。KuaYue 的专用顺序界面写下次启动配置。`pack.mcmeta` 可由适配器按运行版本生成，不要求作者额外填写另一套格式版本。

若资源发现发生得更早，应先发布只读发现配置，再在实际资源读取前确认严格准备结果；具体 hook、事件总线和初次 reload 时机必须由 E0 生命周期验证确定。发现事件不能触发 Minecraft registry 提交，也不能在服务器已读入世界后才决定启用集。

## 23. Corepack 可禁用的实现契约

### 23.1 四种启动与覆盖环境必须都支持

1. `kuayue-core` 默认启用：默认内容按现有 ID 和行为加载。
2. 在实例级 `kuayue/config.json` 禁用 `kuayue-core` 并重启、无外部包：基础 KuaYue 和依赖模组仍可正常启动、创建测试世界。
3. 禁用 `kuayue-core` 并重启、有自包含外部包：外部车型正常注册/渲染，且不隐式依赖默认车型。所有存档共用这份启动集合。
4. `kuayue-core` 启用且有覆盖包：默认注册保留，但普通模型/纹理/配方等按玩家顺序选择；移动 corepack 到高优先级后重启，可覆盖外部包提供的同路径资源。

corepack 禁用只影响包内容，不把 `kuayue` mod 或 handler 从运行环境移除。外部包依赖 `mods.kuayue` 不等于依赖 `dependencies.packs["kuayue-core"]`。若引用 corepack 内容，必须显式声明后者；禁用时拒绝依赖者并报告 `DEPENDENCY_DISABLED`，绝不自动重启用。

### 23.2 基础设施与包内容归属清单

迁移前生成版本控制下的归属清单，例如 `KuaYue/doc/packs/v1/content-ownership.json`，每项记录最终 ID/资源路径、类别、当前实现位置、owner 和迁移动作。

| 类别 | owner | corepack 禁用后的行为 |
|---|---|---|
| 模组入口、handler、工厂、公式/模型运行时、网络 | KuaYue runtime | 保留 |
| 诊断、管理翻译、稳定创造栏 | KuaYue runtime | 保留；图标不引用默认车型 |
| 全部有效 bogey 共用的 BE 类型及 proxy wiring | KuaYue runtime | 外部 bogey 仍可使用；无 bogey 时安全不创建 |
| 默认车型、默认轨道、默认 bogey、餐食及配套资源 | corepack | 不注册、不挂载、不进入派生配方/标签 |
| 作者新增车型及配套资源 | 对应外部 pack | 按自己的开关与依赖加载 |

不得只迁移 JSON 而忽略旧 Java 静态注册。要逐项审计哪些默认内容仍在静态初始化时 `setParent()` 到根注册树，哪些默认配方/资源通过 JAR 根目录自动暴露。

### 23.3 已发现必须处理的具体位置

- `KuaYueTrainPanelModule.TRAIN_PANEL_TAB` 的 icon 当前取 `I11Panel.DF11_DOOR`。改为 vanilla/Create 中始终存在的安全物品或 runtime 自有图标；保留 tab ID，不能因为调用图标而加载/注册默认车型。
- `BogeyStyleTypeHandler.apply()` 当前调用 `BogeyRuntimeRegistration.ensure()`。移到宿主会话 finalizer，收集全部有效 bogey 后一次执行。
- `BogeyRuntimeRegistration` 的共享 `loco_bogey_entity`、proxy ID 和 global BE supplier 属于 runtime，不受 `kuayue-core` 整包开关控制，也不存在默认 parcel 开关。零 bogey 时不抛异常，单个外部 bogey 时有效方块集合必须包含它。
- registry group 缺失不能宽容挂根掩盖 corepack 依赖。外部包应声明自己的数据组；只有 runtime 明确发布的稳定符号可不依赖 corepack 使用。
- 硬编码默认 bogey/车型 ID 的运行代码和资源配方应按归属清单审计；需要默认内容的行为要声明依赖、移入 corepack 或有明确回退。

### 23.4 JAR 资源隔离

建议构建时将内置 `kuayue-core` 专属普通资源放在 JAR 的 `kuayue-packs/kuayue-core/` 下，按标准 pack 根组织：manifest、`assets/`、`data/`。runtime 资源保留在 JAR 普通根下；运行适配器仅在该包有效启用时暴露内嵌根。

现有 resource compiler 模块可以继续保留作者源目录，但要调整**输出位置**，不能把同一份 corepack `assets/` / `data/` 同时输出到 JAR 根和内嵌根。禁用时跳过 JSON 还不够，JAR 根的自动扫描也必须没有泄漏。

若无法使用内嵌根，替代的过滤 adapter 必须同时过滤注册索引和原生资源路径，并给出等价的资源可见性测试；不能仅为 Kasuga 模型 loader 过滤，而让 vanilla 配方/loot/tag 仍看见它们。

验收必须包含：禁用后 corepack 来源的默认注册和资源不进入有效视图，runtime 与外部包仍可见；若外部包提供了同路径资源，它仍可见，不能把这种合法共享 namespace 误判为 corepack 泄漏。恢复启用、重排并重启后原 ID 恢复，资源按新的顺序生效。

## 24. 诊断、指纹、存档与测试契约

### 24.1 可机器读取的加载报告

输出 `<game-dir>/kuayue/reports/packs/startup.json`，包含格式/环境版本、安装包状态、请求开关、有效 ID 和声明版本、diagnostics。内部缓存摘要可选，不是强制协议字段或兼容判断条件。

诊断字段：`code`、`severity`、`stage`、`ownerId`、`sourceId`、`parcelId`（可空）、`logicalPath`、`jsonPointer`、`relatedIds`、`message`。对同一输入稳定排序。第一批错误码至少包括：

```text
INVALID_MANIFEST / UNSUPPORTED_FORMAT / DUPLICATE_PACK_ID
INVALID_NAMESPACE / UNSAFE_PATH / LIMIT_EXCEEDED / INVALID_ZIP / DUPLICATE_ZIP_ENTRY
DEPENDENCY_MISSING / DEPENDENCY_DISABLED / VERSION_MISMATCH / DEPENDENCY_CYCLE
HANDLER_NOT_STRICT_READY / UNKNOWN_FACTORY / UNKNOWN_GROUP
INVALID_REFERENCE / UNDECLARED_DEPENDENCY / RESOURCE_OVERRIDE / REGISTRY_CONFLICT
BUILD_FAILED / FINALIZE_FAILED / CONTENT_MISMATCH / SAVE_VERSION_INCOMPATIBLE
```

`RESOURCE_OVERRIDE` 是信息级覆盖追踪，不是拒绝包的错误，报告选中的最高层和被遮蔽来源；`REGISTRY_CONFLICT` 仍是阻断诊断。致命初始化失败尽量写报告，不因报告写失败吞掉原始错误。v1 需要资源顺序 GUI，但不要求完整图形诊断面板，日志与机器报告必须足够定位。

### 24.2 Hash 仅作为内部缓存/诊断工具

KuaYue 不用文件 hash 决定车包、存档或多人兼容性。同 pack ID、同声明版本下，文件可不同，平台不阻断、不强制发版、不根据字节差异要求同步；作者承担内容兼容责任。所有包使用此统一规则，不区分是否纯资源包。

公开管理/历史元组以 `(pack id, version, format_version)` 为基础，不要求 content hash。资源顺序是用户设置，不构成文件一致性证明。不得保留一个隐藏的“同版本 hash 不同就拒绝”检查。

hash 如有实际工程用途，可以作为内部快照缓存键或可选调试信息：对参与读取的逻辑层、路径和原始字节计算 SHA-256，不含安装绝对路径、mtime、容器名或 ZIP 压缩元数据。它的作用是避免缓存误复用，不是认定内容是否兼容。目录/ZIP 同逻辑内容可得到相同缓存键；不同文件内容也可以正常加载。

是否必须计算内部 hash、采用何种缓存键由实现任务评估；v1 协议和存档不要求它，不能为了获得“指纹”重新引入严格内容一致性门槛。

### 24.3 自身历史版本的存档升级契约

当前包在自己的 manifest 中声明 `save_compatibility`，描述“此版本能够直接接收哪个历史版本保存的世界”，不是依赖别的包版本，也不是多人兼容范围。

两种声明方式互斥，以下是字段片段，不是完整 manifest：

```json
{
  "version": "0.2.0",
  "save_compatibility": {
    "from_versions": ["0.1.0"]
  }
}
```

```json
{
  "version": "0.3.0",
  "save_compatibility": {
    "minimum_version": "0.2.0"
  }
}
```

- `from_versions`：非空、无重复的精确历史版本数组。0.2.0 的示例直接允许 0.1.0 → 0.2.0。
- `minimum_version`：允许 `[minimum_version, 当前版本)` 内历史版本直接升级。0.3.0 的示例允许 0.2.0 → 0.3.0，但不允许 0.1.0 → 0.3.0。
- 两种字段必须且只能出现一种，不提供任意转换脚本；所有声明版本必须合法且小于当前版本。用户示例的 0.1、0.2、0.3 在当前三段版本基线中写作 0.1.0、0.2.0、0.3.0。
- 不声明时不授权跨版本升级；同版本无须再声明兼容，直接接受，不比较文件内容。降级默认不支持。
- 兼容判断只针对**同一 pack ID**。包重命名、拆分/合并或替代另一包不能通过本字段自动完成。
- 不计算传递兼容闭包：即使 0.2 接收 0.1、0.3 接收 0.2，0.3 也不能直接接收 0.1，除非自己声明它。玩家先运行并成功保存 0.2，再换 0.3，才是两次升级。
- 所有历史包都必须通过判断；一个不兼容项即可阻止正常载入。新依赖也必须在当前启动集合满足，兼容声明不自动启用或下载依赖。
- 平台只检查声明版本/兼容字段，不证明 registry ID、BlockState、BE 数据实际可迁移。作者声明错误或改内容不改版本的风险均由作者承担；本字段不是自动迁移脚本。
- 世界版本历史通过 Minecraft SavedData 随存档保存，不记录或比较 hash；先检查历史，不在预检失败时更新。具体写入时机见第 24.4 节，首次缺记录策略另行明确。
- corepack 使用相同字段，不是特殊的永远兼容包。

### 24.4 世界保护与多人时机

**世界历史使用 Minecraft SavedData，存储归属已经确定。** 执行基线的数据名为 `kuayue_packs`，由游戏写到 `<world>/data/kuayue_packs.dat`。这取代原全局配置的世界历史表，不需要世界路径索引或额外 UUID。移动、改名或复制整个世界时，该文件随世界一起保留；复制得到的是相同历史的副本，不要求额外改变身份。

只在服务器的主世界（Overworld）数据存储中维护一份世界级记录，避免在 Nether/End 的 data 目录分别生成历史。同一局域网/单机使用内置服务器，专用服务器使用其实际世界目录；远程客户端不另建本地世界历史副本，也不通过此文件下载/同步车包。

计划新增 KuaYue `PackHistorySavedData extends SavedData`。Minecraft 1.21.1 的 API 用法可参考：

```java
// 计划实现示意：在主世界和相应生命周期可用时调用。
SavedData.Factory<PackHistorySavedData> factory = new SavedData.Factory<>(
        PackHistorySavedData::new,
        PackHistorySavedData::load);
PackHistorySavedData history = server.overworld().getDataStorage()
        .computeIfAbsent(factory, "kuayue_packs");
```

- `load(CompoundTag, HolderLookup.Provider)` 解析自己的 NBT payload；`save(CompoundTag, HolderLookup.Provider)` 返回 payload；实际 `.dat` 封装、压缩和普通存盘交给 Minecraft，不手工写 ZIP/JSON/NBT 文件。
- Kasuga 已有 `Saved<T>` 包装：`KasugaLibNext/modules/inject/src/main/java/lib/kasuga/inject/auto_configure/Saved.java:27–36,59–62` 默认取 Overworld 并调用 computeIfAbsent；同目录 `SavedFactory.java:104–107` 使用相同 Factory。可复用或直接调用原生 API，不必新增通用存档框架。
- 现有包装绑定 `ServerStartedEvent`，不意味着它足够早完成载入前检查。E0 必须核对读取/校验的时机；不能把“有 SavedData API”当作已解决早期兼容检查。

建议 v1 payload（示意为 SNBT，不含 Minecraft 自动附加的外层数据）：

```snbt
{
  schema_version: 1,
  initialized: 1b,
  packs: [
    {id: "kuayue-core", version: "2.0.0", format_version: 1},
    {id: "example-railway", version: "0.2.0", format_version: 1}
  ]
}
```

- 只记录历史 pack ID、声明版本和格式版本，包括当时有效启用的 corepack。没有 enabled、资源顺序、hash、下载地址或独立加载设置。
- 新创建的对象 `initialized = false`，与已有 `initialized = true, packs = []` 的合法空内容历史区分；不能只用 computeIfAbsent 返回非空对象认定“已有历史”。
- 校验历史时只读对象。世界已获准使用并进入正常保存流程后，更新记录并 `setDirty()`，交给 Minecraft 的正常 SavedData 保存；拒绝载入或预检失败不覆盖历史。
- 不承诺 `.dat` 与全部世界 chunk 文件构成跨文件原子事务；实现需要遵循游戏正常保存顺序，而非提前在注册/配置扫描阶段写世界文件。
- 数据版本未知或解析失败应给出诊断，不将损坏历史当作首次缺记录并覆盖。Factory 读取失败与真正文件不存在的区分由 E0/API 适配测试确认。
- 对象由当前服务器/世界持有；切换世界或关闭服务器时清理引用，不缓存到跨世界全局单例中。

**世界身份问题已解决，首次缺少记录的策略仍未确定：**车包系统之前创建的旧存档，或被移除了 `.dat` 的世界，没有可读取的历史版本，无法验证“0.3 最低兼容 0.2”。可只提示并在首次正常保存建立历史，也可让用户提供历史版本；这是独立候选，不因采用 SavedData 就自动决定。平台不扫描所有方块猜作者版本，也不默认删内容。

- 历史已知时按 `save_compatibility` 检查声明版本变化，同版本接受内容差异；所有包同策略，不为世界单独选包。缺包和首次缺记录的处理门槛仍待明确。
- 当前资源顺序可在运行报告中用于诊断，不写成 SavedData 开关或配置；顺序变化本身不拒绝旧世界，实际数据兼容风险由作者保证。
- 存档兼容性检查只读本次 `StartupPackSnapshot`，无权添加、移除、启用、禁用或重新构建来源。打开或切换世界不会改变游戏的包集合。不兼容时提示修改全局配置并重启，不自动生成世界专用包设置。
- 复制世界时只需随完整世界复制 `data/kuayue_packs.dat`，不依赖复制另一实例的全局配置历史。新实例的启用包仍由新实例配置决定，SavedData 不改变它。
- 检查必须在可能读取/保存缺失 registry ID 的世界数据之前完成；在普通 `ServerStartingEvent` 才发现缺包可能太晚，E0 要验证安全 hook。
- 多人车包副本由作者/服主分发，KuaYue 不下载、同步文件或自动切换配置。同包同版本不同内容不受 hash 检查限制；资源顺序差异可供诊断，不以此实施强同步。基本缺包/声明版本差异检查的处理需对齐原有游戏协议并另行明确，不能用存档升级声明代替多人环境元信息。
- 服务器和客户端各自固定实例级集合；连接事件无权更改本地开关、来源或顺序。管理员自行管理分发与配置，KuaYue 不自动修复差异。
- 单包自动下载、恢复世界的强制继续开关、运行时更改 pack 集都不属于本期。

### 24.5 测试与证据

纯 JVM fixtures 覆盖 JSON/路径错误、ZIP 直接读取/重复 entry/字节上限、目录/ZIP 逻辑内容等价、显式依赖拒绝、多 namespace、启动配置与资源顺序、registry claims、corepack、自身版本兼容和快照。如果实现内部 hash，单独测试缓存键等价，不把它加入兼容判断。

版本矩阵包含精确/最低历史版本授权、未授权升级/降级和同版本不同内容不阻断。SavedData 测试覆盖 NBT 往返、dirty 保存、关闭重开、主世界只一份、复制/改名、不同世界不串数据、缺记录与合法空记录区分、损坏文件不覆盖。首次缺记录策略确认后补对应期望；多人无下载同步和 hash 阻断。

FML/游戏测试覆盖真实 registry 提交；客户端 smoke 覆盖模型/纹理/动画和创造栏图标；专用服务器 smoke 覆盖无客户端类泄漏、配方和世界保护。

每次测试记录命令、退出码、测试数、是否跳过、报告路径。私有模型不作为必需 fixture。不得为了让测试通过删除 FML launcher、过滤失败测试或使用 `-x test` 作为验收。

## 25. 可交付给 agent 的开发任务

新增代码优先放在现有模块，不先创建新的 Gradle 子模块。建议 Kasuga 新 SPI 位于 `modules/data-driven/src/main/java/lib/kasuga/registration/data_driven/{source,pipeline,validation}/`；资源适配位于现有 core resource 包。KuaYue pack manager 位于 `modules/core/src/main/java/cn/kycraft/kuayue/packs/`；车型 handler 仍位于 train-parts。core 不得直接依赖 train-parts，车型集成通过宿主扩展点接入。

| 任务 | 依赖 | 修改范围与交付物 | 完成验收 |
|---|---|---|---|
| **E0 生命周期与归属验证** | 无 | 审计 provider 安装、FML 注册、资源发现、世界载入 hook；建立 corepack/runtime 内容归属清单 | 输出代码路径/事件时序和基线结果；确认零包与资源隔离技术路径；未确认则报告阻塞，不猜 hook |
| **L1 来源与逻辑视图** | E0 | source SPI、JAR/目录/ZIP adapter、稳定逻辑层、安全读取 | 三来源逻辑内容等价；可选 hash 只用于缓存；重复 entry、损坏 ZIP、限额、handle 生命周期通过 |
| **L2 纯预检与占用** | L1 | 定义目录、严格 handler 能力、validation、claims、diagnostic DTO | apply 前查出坏引用、跨类型 block/item 冲突；宽容旧入口测试保持 |
| **L3 会话与宿主 provider** | L2 | 统一 phase、一次性 finalizer、严格 provider、默认 fallback | 无包也不回退 JAR；只 attach 一次；致命构建错误不被吞掉 |
| **K1 协议与包选择** | E0，可先用 fake source | manifests、显式必需依赖、save_compatibility、全局配置 | 外部新包默认禁用且可首次启动前手动启用；同版本不同内容不限制；直接依赖规则；不另建文件 |
| **K2 业务校验及共享 runtime** | L2、K1 | train-parts 严格 handlers、runtime 符号、bogey finalizer、安全 tab icon | 一个独立外部 bogey 在 corepack 禁用时可创建 BE；零 bogey 安全；默认车型静态注册不泄漏 |
| **R1 资源快照桥接** | L1、L3、K1、E0 | mounts/cache、双侧 pack stack；Kasuga scoped 单资源/stack 适配 | corepack 与外部覆盖按全局顺序；类型特定合并正确；快照不受运行中目录/ZIP 修改影响 |
| **NR1 非注册型 schema 接入** | L2、L3、K1、R1 | 启动期非注册候选选择、ReloadHandler、只读预检、来源和生命周期 | models/motions 同 ID 按顺序先选后编译；reload 胜者正确；显式引用和零 claims 合法；不新增 block/item |
| **O1 游戏内覆盖顺序** | K1、R1 | KuaYue 客户端顺序界面、状态/待生效提示、配置保存 | corepack 可上下移动；保存时不改本次 stack，重启后生效；专用服务器读同一 order 契约而不加载 GUI |
| **K3 corepack 重打包迁移** | K2、R1 | 内嵌根、compiler 输出、旧路径/静态注册迁移 | 默认 ID golden；四种启动/覆盖环境；无默认资源泄漏或重复 registry 注册 |
| **S1 世界版本与多人平台** | E0、K3、O1；首次缺记录策略待确认 | PackHistorySavedData、Overworld Factory、版本比较、dirty 生命周期、环境元信息 | `<world>/data/kuayue_packs.dat` 往返/复制/隔离；无全局 worlds 表或存档开关；无 hash/下载同步；缺记录策略确认后补验收 |
| **V1 发布门槛** | 全部（含 NR1） | 新车型包、纯原生资源包、仅 reload 包、依赖样例、作者指南和报告 | 矩阵通过，三种内容路线都有验证；插件尚未实现不阻塞 |

推荐基础命令（执行前核对当前 Gradle 任务和环境）：

```sh
# KasugaLibNext 仓库目录
./gradlew :modules:data-driven:test
./gradlew :modules:modelling:modelUnitTest

# KuaYue 仓库目录
./gradlew :modules:core:test :modules:train-parts:test
./gradlew build
```

客户端/服务器 smoke 使用仓库支持的实际 run 配置及**隔离的测试游戏目录和测试世界**。不得启动或重写用户现有世界。依赖下载、图形环境或权限不足时明确记录未执行，不把静态文件清点当作游戏验证。

### Agent 每个任务的结束报告

1. 已实现的契约和所属任务 ID。
2. 修改文件、公开 API/格式差异及兼容性。
3. 实际执行的测试与结果、未执行原因。
4. 剩余阻塞及后续任务输入。
5. 不做跨任务隐式扩展；涉及 format_version、默认安全策略或产品范围变化先更新决策记录。

## 26. 方案达到可开发程度的检查清单

当前文档已把“能力方向”推进到字段、路径、身份、失败规则和任务接口级别，但仍不应声称所有任务已经能够无研究直接生成实现。

### 已经具备的任务输入

- corepack 可禁用、可排序及四种启动/覆盖验收。
- ZIP/目录发现、安全读取、manifest/config 示例、多 namespace、必需依赖、资源覆盖与优先级 UI 契约。
- 通用层/业务层边界，strict 与 legacy 模式的兼容约束。
- 预检、claims、拒绝闭包、共享 finalizer 和一轮提交规则。
- 文件快照与可选缓存键、作者版本兼容、统一宽松检查、新包禁用、无多人下载同步；世界历史确定用原生 SavedData 随存档保存，首次缺记录策略仍需确认。
- 按仓库和模块拆开的任务与完成标准。

### E0 必须消除的技术不确定性

- NeoForge 21.1.203 下，显式 provider 在注册前安装、资源包在首次 reload 前可见的确切 hook 和顺序。
- JAR corepack 内嵌根与现有 Gradle resource compiler 的兼容输出方式。
- 世界数据尚未因缺失 ID 被读取/保存之前的安全拒绝时机，以及客户端连接的前置检查时机。
- 默认 Java 静态注册、创造栏、配方与共享 BE 的完整归属清单；本文列出的具体位置不是完整审计结果。
- 实例 config.json 的读写及主世界 SavedData 获取/dirty 保存/异常读取区分。世界存储方式已确定，不再研究全局世界路径键；不另建启禁用配置或手工世界 JSON。

### 执行边界

**可以先交给 agent 做 E0、schema/parser fixtures 和三种来源适配原型；E0 通过后再开展注册/资源/存档集成。** 若技术验证推翻建议实现，先给替代方案及证据；不得把已确认的 ZIP 直接读取、资源覆盖与排序、多 namespace、必需依赖、按声明升级及此前的全局整包重启等规则重新标为待确认。

本轮只完善方案，不授权立即修改三个项目的生产代码、运行用户世界或发布版本。

## 27. 非注册型数据驱动与纯资源车包：调查结论及执行契约

> 本节由两个只读 subagent 分别调查 KasugaLibNext 与 KuaYue 后整理，并复核关键源码。下述“当前支持”指代码扩展点和已有消费者，不代表外部目录/ZIP 车包系统已经实现。调查未运行 Gradle 或游戏测试；测试文件是覆盖范围证据，不是本次执行通过记录。

### 27.1 明确结论

**当前数据驱动系统支持不创建 Minecraft 方块/物品的定义。** “schema 与启动注册流程有关”不等于“每种 schema 都必须生成 registry 项”。需要区分三种路线：

| 路线 | 输入与消费方式 | 是否创建 block/item | v1 使用方式 |
|---|---|---|---|
| 原生资源 | 模型、纹理、语言、声音、配方、tag、loot 等由已有游戏 loader 消费 | 不因加入资源文件而新增 block/item | 可完全没有 Kasuga 索引，parcel 使用 `indexes: []` |
| 启动期配置 | `on_register` → `TypeHandler.parse/apply`，可只编译 metadata/model plan | 取决于 handler；motions/models 本身不创建 | 当前尚无通用覆盖选择；v1 新增按优先级选配置后编译，不需要改为热重载 |
| reload 型自定义定义 | `on_reload` → `ReloadHandler.decode/register` → 自有 runtime bucket | 不需要创建 Minecraft registry 项 | 使用已有 FSM/clip 类型，或由 mod 提供新 handler 和实际消费者 |

`register` 一词在 reload API 中指写入运行时定义集合，不是再次执行 Minecraft 方块/物品注册。v1 不考虑同名方块/物品注册；资源型 clip 的同 ID 候选选择与 registry ID 冲突也不是同一机制。

### 27.2 已有实现与准确证据

以下路径均相对当前工作目录，行号对应本次读取的源码：

| 已有能力 | 证据 |
|---|---|
| `TypeHandler.apply` 不强制创建 registry 项；context 可保存配置 | `KasugaLibNext/modules/data-driven/src/main/java/lib/kasuga/registration/data_driven/TypeHandler.java:8–31`、`context/BuildContext.java:6–30` |
| `MetaTypeHandler` 只是标记基类，没有自动资源注册/持久化能力 | 同一模块的 `handler/MetaTypeHandler.java:1–6`；`RegistryGroupHandler.java:19–37,53–87` 创建配置组，不是 Minecraft registry entry |
| KuaYue motion 只编译配置 | `KuaYue/modules/train-parts/src/main/java/cn/kycraft/kuayue/parts/bogeys/data/BogeyMotionTypeHandler.java:25–40`：`CompiledBogeyMotion.compile` 后 `putMeta` |
| KuaYue model 只编译模型计划 | 同一目录 `BogeyModelTypeHandler.java:36–65`：解析 motion 引用、构建 `BogeyModelPlan` 并 `putMeta` |
| FSM/clip reload 类型是生产扩展，不仅是测试代码 | `KasugaLibNext/modules/modelling/src/main/java/lib/kasuga/rendering/models/mc/dynamic/fsm/FsmReloadHandlerRegistrar.java:22–31` 在初始化时登记两种 handler |
| 通用 reload SPI 可增加自定义顶层类型 | `KasugaLibNext/modules/data-driven/src/main/java/lib/kasuga/registration/data_driven/reload/ReloadHandler.java:41–118`，编排器按 registry 路由，不要求新增 block/item |
| reload 内容从普通资源管理器读取，可使用资源覆盖 | 同一目录 `ReloadOrchestrator.java:307–339,369–397`，索引经 `listResources`，内容经 `getResource` |
| clip 使用非 Minecraft 运行时 bucket，代码来源优先 | `KasugaLibNext/modules/render-core/src/main/java/lib/kasuga/rendering/models/uml/dynamic/fsm/FsmAnimationClips.java:17–26,53–73,96–102` |
| KuaYue 有实际使用的非注册型 clip | `KuaYue/modules/train-parts/resources/bogeys/data-driven/data/kuayue/kasuga_lib/data_driven/bogeys.json:2–10` 在 `on_reload` 中列出 QJ clip；`BogeyModelPlan.java:154–175` 按 clip ID 查询运行时集合 |

`bogey_sizes` / `bogey_styles` 会构造 Create 的注册期对象，不能因为它们不直接叫 block/item 就归类为无副作用资源。`bogey_blocks` 和 `tracks` 则确实创建方块。类型分类依据 handler 的行为，不依据文件名或“data”目录位置。

Kasuga FSM 示例位于 modelling 的 `src/contentTesting/resources`；生产 handler 在 `src/main/java`。KuaYue QJ 的索引是 register + reload 混合示例，不是已经实现的外部纯资源包。目前未发现 KuaYue 生产仓库中的 `pack.json` / `parcel.json` 外部包实例。

### 27.3 引入新 schema 需要什么

对于已经有消费者的原生资源，不需要额外引入 Kasuga schema；正确路径和资源栈就是契约。对于一个新自定义顶层键（例如未来的 UI 参数或模型运行配置），完整链路是：

```text
mod 发布 schema 文档及解码实现
→ mod 初始化时登记 TypeHandler 或 ReloadHandler
→ 索引/已声明 glob 发现内容 JSON
→ handler 解码、校验并发布定义
→ 游戏运行时消费者读取定义并产生行为/表现
```

- JSON Schema 文件只描述字段供编辑器/验证器使用；当前代码不会因为加入 `*.schema.json` 或 `$schema` 自动生成/登记 handler。
- `TypeHandler` 用于一次启动的构建；只写 `BuildContext.metaStore` 不等于建立了可独立查询的长期资源仓库。若配置需要在构建后使用，handler 必须将其发布给明确的运行时 owner。
- `ReloadHandler<T>` 提供类型名、解码、资源 bucket 清理/写入和全类型完成后的检查。新类型的代码由 mod 提供，不能由 pack 上传任意 Java/JS。
- 新资源 schema 不需要 block/item factory，但需要 codec/parser、稳定定义身份、来源归属、运行时存储和消费者。没有消费者的配置即使解码成功也不会改变画面或车辆行为。
- 当前 `bogey_models` / `bogey_motions` 在启动 phase 编译；v1 必须让这类非注册配置按全局优先级覆盖，先选择有效定义再验证引用/编译，并由现有 consumers 使用获选计划。它们仍可保持启动期生命周期，不自动移到 `on_reload`，运行中修改仍重启生效。

### 27.4 两种最小纯资源包

**A. 纯原生资源包：**不新增任何 registry 项，只提供已有 loader 能理解的资源。例如语言/外观覆盖包：

```text
appearance/                         # 可直接打包为 appearance.zip
├── pack.json
└── parcels/appearance/
    ├── parcel.json
    └── assets/kuayue/lang/zh_cn.json
```

Pack manifest 仍遵守第 21.2 节，列出一个 parcel，并声明实际资源 namespaces。示例 parcel：

```json
{
  "format_version": 1,
  "id": "kuayue:appearance",
  "name_key": "pack.example_appearance.name",
  "indexes": []
}
```

此包没有 Kasuga 索引，也没有 `on_register`；加载器不能以“没有注册内容”拒绝它。pack 根/parcel 根资源照常映射、排序、覆盖；数据强依赖由作者按实际需要声明。metadata 仍必填，空 indexes 不代表免 manifest、namespace 或安全校验。

**B. 仅 reload 的自定义资源包：**有 Kasuga 索引，但不声明启动注册内容。例如追加/覆盖动画片段：

```text
animation/
├── pack.json
└── parcels/animation/
    ├── parcel.json
    └── data/example_animation/
        ├── kasuga_lib/data_driven/animation.json
        └── animation_clips/preview.json
```

parcel 的 `indexes` 指向 `data/example_animation/kasuga_lib/data_driven/animation.json`；索引内容：

```json
{
  "on_reload": ["animation_clips/preview.json"]
}
```

片段文件形状沿用已有 loader，例如：

```json
{
  "animation_clips": [
    {
      "id": "example_animation:preview",
      "duration_seconds": 1.0,
      "formula_transforms": [
        {
          "target": {"bone": "group"},
          "transform": {"rotate": {"y": "30"}}
        }
      ]
    }
  ]
}
```

这是文件形状示意，不声称本次已运行 codec 测试。片段可以独立进入 bucket，但只有已有模型/FSM/其他消费者引用该 ID 时才有可见效果；文件里没有一个新方块。

`animation_clips` 没有默认目录 glob，必须列到 `on_reload`；`state_machines` 当前有 glob。只含 `on_reload` 的索引在现有索引解析中合法，显式空 `on_register: []` 也合法，但并非必填。一个内容路径同时列入两个域仍须拒绝。

### 27.5 当前覆盖能力的边界及 v1 补齐

1. **文件路径覆盖已有基础：**当前 reload 编排器从资源管理器选择索引/内容，同路径的高优先级文件可被选中。覆盖索引是替换该索引文件，不是合并任意 `on_reload` 数组；追加独立内容应使用不同索引路径。
2. **不同文件中同 runtime ID 尚需顺序适配：**当前 resolver 对资源候选执行 last-wins，但发现顺序包含逻辑路径排序，不能直接等同于玩家 `packs.order`。v1 必须保留候选来源/pack rank，并让资源型定义的胜者跟随有效资源优先级；空 registry claims 不能令此类重复定义绕过检查。
3. **代码来源不是资源层：**现有 FSM bucket 清理只删 RESOURCE，SCRIPT 记录优先且不能被资源覆盖。不能把“允许覆盖 corepack 的资源”实现成清空 runtime 代码定义。应将确实属于 corepack 的默认资源定义归入 RESOURCE 路径；可覆盖代码定义的通用政策不在本期。
4. **严格预检尚需接入 reload：**已有 reload listener 可以记录错误后继续，甚至先清资源桶再解码。车包严格启动预检必须复用 decoder，在构建/挂载前报告 owner 错误，而不是先产生 registry 副作用后才发现 reload 文件坏了。不能在预检时调用 clearBucket/register 改写运行时。
5. **启动/重载分别处理：**启动非注册配置按 `(type,id)` 与优先级选胜者后编译；注册项仍独立检查 claims；reload 定义从当前固定资源栈读取。包集合/顺序重启生效，不扫描安装目录、自动启用或更改顺序。
6. **所有包统一规则：**KuaYue 不识别纯资源包类别，只识别 handler 是否有注册副作用。所有包遵守相同开关、显式必需依赖、版本声明、安全和作者责任策略；同版本字节变化不限制，不为纯视觉包增加豁免或更严规则。

### 27.6 开发补充与验收

**KasugaLibNext：**复用 `ReloadHandler` / registry / orchestrator，不再发明一套只能注册方块的资源 schema 框架。补齐非注册型声明、只读 reload 预检、候选来源/priority 诊断，以及与资源栈一致的 runtime ID 选择；保留旧 API 和 SCRIPT/RESOURCE 所有权规则。

**KuaYue：**不按包内容分类管理，零 claims、空 indexes、只有 reload 或混合定义都是同一种车包。没有 bogey 时不创建共享 BE；启动 motions/models 增加覆盖选择，但不自动改成 reload 类型。所有新包默认禁用，新增 schema 要有 handler、consumer 和文档。

NR1 和发布验收至少覆盖：

- 纯语言/外观包在目录和 ZIP 中可加载，不创建 registry 项，重排后资源胜者正确。
- 仅 reload 的动画片段包可进入真实桶；配套消费者测试证明内容被使用，不以“JSON parse 成功”代替可见效果。
- clip 未列索引时按已有规则不加载；未知顶层 schema 产生诊断，不动态生成 handler。
- 两包同文件路径、不同文件但同 clip ID，两种重复模式均按 v1 资源优先级处理；重排必须重启。
- 两包同启动 model/motion ID：获选定义进入编译，下层配置不产生副作用；其消费者确实使用获选计划，方块/物品数量和 ID 不改变。
- 外部新包默认禁用，手动在首次启动前启用后才进入集合；A→B→C 不允许 A 未声明直接引用 C。
- 同版本的配置/模型/纹理改变不被 hash 阻断，所有包不按资源类别区分兼容策略；多人无文件下载/同步。
- runtime SCRIPT clip 不被资源覆盖或 reload 清理；资源定义随当前快照重新建立。
- 声明式预检不调用 registry 或运行时写入；零 claims 合法，有效包没有因缺少方块而被拒绝。
- 错误 reload 内容可归属 pack/parcel/字段；普通资源覆盖不变成注册 ID 替换。

已有测试源码可作为迁移基线：Kasuga 的 `FanDataDrivenReloadTest`、`AnimationClipReloadTest`、`ReloadHandlerExtensionPointTest` 和 data-driven 的 `JsonTreeBuilderIndexSchemaTest`；KuaYue 的 `QjFormulaAnimationTest`、`BogeyModelDataDrivenSchemaTest`。本次未执行这些 JVM/FML 测试；插件 `npm test` 的 10 项通过只说明编辑器模型组件逻辑，不是外部资源包的运行时验证。
