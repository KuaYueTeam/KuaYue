# KuaYue 车包系统详细方案 v2（PR #86 / #87 资源平台代码基线）

> 本文是可独立阅读的实施方案，基于 2026-11-13 拉取 PR #87 后的本地源码，并对照 `plan_vfs.md` 复核。保留 `plan_detailed.md` 的已确认产品规则，调整底层复用、任务拆分、安全策略和生命周期边界；原方案和 VFS 规划不修改。原始设想见 `plan.md`。本文不授权立即修改生产代码、运行用户世界或发布版本，也不代表车包系统已经实现。

## 本版基线与修订摘要

### 仓库更新记录

上一轮已更新三个仓库；本轮只对 KasugaLibNext 执行 `git pull --ff-only`，未 stash、reset 或覆盖本地改动。另两个仓库保留并核对原有干净工作区，不将其描述成此次已查询远端最新状态。

| 仓库 | 分支 | 本次代码基线 | 更新结果 |
|---|---|---|---|
| KasugaLibNext | `master` | `24068ae8d5c7ecbc089c425824404565aa0974fc` | 从 #86 的 `36b46604b8db55d0ca769ca18dc46d1f506cf516` 快进；HEAD 为 `feat(resource): add scoped resource platform and async script tickets (#87)` |
| KuaYue | `main` | `eab56214818d9a0860c1a0ec030808e7c61b7802` | 沿用上轮 PR #19 基线；本轮未拉取 |
| KasugaLib-BlockBenchPlugin | `main` | `3640b134e998bcc0cf59ba7c8113d7f96e99b996` | 沿用上轮基线；本轮未拉取 |

KasugaLibNext 本轮增量涉及 78 个文件，新增资源平台、模型 adapter、脚本 Ticket、测试和文档。`modules/data-driven` 仅新增 opt-in `ScriptBlockReferenceDecoder`，没有修改 JsonTreeBuilder/TypeHandler/BuildContext/JsonTreeIntegration 的注册构建管线。KuaYue `settings.gradle:14–24` 已通过 composite build 引用 `../KasugaLibNext`，因此开发构建会使用新基础库；不等于发布的 Maven 工件已经包含相同提交。

### v2 相比原方案的变化

1. **复用范围从 VFS 扩大到资源平台。** `ResourceSource`、`ResourceSnapshot`、`ResourceCatalog`、URI、InputPolicy、路由与 MC 分层 adapter 均已有，不再计划另建同类基础设施。
2. **L1a 改为注册来源适配。** 共享平台 descriptor/locator；只补 namespace、pack/parcel、入口索引、本包共享根和原定义读取。
3. **L1b 改为启动冻结与策略补齐。** 已有捕获、实际字节限额与 lease；仍补整包聚合预算、目录预扫描成本、持久缓存（可选）及固定启动来源。
4. **R1 改为批准资源包集成。** `ApprovedResourcePacks` 已接 AddPackFindersEvent，`MappedPackResources` 保留 source/root 层；车包必须传入冻结来源，不直接传可变安装目录。
5. **修正旧生命周期风险。** #87 已分离 server/process 清理，并绑定 GameShuttingDownEvent；删除“服务器停止会清理进程列表”的当前缺陷描述，保留寿命/异步释放验收。
6. **模型/脚本按真实范围复用。** 非 MC bbmodel+内嵌 PNG 已有派生投影；其他模型迁移不完整。脚本改为显式 ScriptBlocks 注册、契约和非阻塞 Ticket，不是规划示例中的任意命名导出自动暴露；不扩大车包脚本范围。
7. **注册关键路径不变。** L2/L3、KuaYue 协议与依赖、bogey finalizer、corepack、顺序 UI、世界历史仍未实现，不能因资源平台已有就标车包任务完成。
8. **证据分层。** 本轮 Java wrapper 仍因缺 Java/JAVA_HOME 退出，未执行 JVM/FML/GUI 测试；上一轮插件 10 项通过只作历史记录，详见第 28 节。

### 本轮主要新功能及车包边界

| #87 已有功能 | 可直接复用 | 尚不能推导的保证 |
|---|---|---|
| ResourceUri / MinecraftNameCodec / 类型证据与查询 | Unicode 提供者身份、逻辑 key、稳定目录和生成名称 | 不改写既有 kuayue:*；类型证据不是可渲染验收 |
| ResourceSources / ResourceSnapshot / InputPolicy | 目录/归档/mod/VFS 捕获、链接/重复/CRC 检查、大小预算、私有暂存与 lease | 不是整包跨 root 预算或整进程固定来源；目录构造仍会先完整哈希扫描 |
| ResourceCatalog / ResourceRouting | 全部 provider、stack、可见性、模板、最长匹配和多分发 | 文件胜者不等于 DefinitionKey 胜者；Entry Point 依赖不等于 pack dependency |
| ApprovedResourcePacks / MappedPackResources / managedView | 首次资源发现、动态版本 metadata、双侧真实 MC stack、借用 manager | 每次 discovery 再 source.capture；不自动冻结车包或决定启用集合 |
| ResourceCoordinator / Generation / process/client/server scopes | prepare/publish、失败清理、代际 lease、正确 scope owner | 不回滚 registry 或任意 publish 副作用；不替代宿主注册 provider |
| ModelResourceSource / ResourceModelInput | 非 MC bbmodel 内嵌 PNG、子纹理 URI、同次发现前投影及 JSON/text/binary 输入 | 原生 assets/data 不走这条派生路径；OBJ/PMX 等外部依赖仍需适配 |
| ScriptBlockRef / ManagedScriptEntryPoint / ScriptTicket / ResourceAccess | opt-in 契约化脚本块、逻辑端、异步调用、受限内存工作区 | Javet 不可信沙箱未确证；车包 v1 不因此开放脚本 |

以下区分三种状态：**源码已有**表示实现存在；**计划新增**表示尚未实现；**待技术验证**表示接口方向可确定、生命周期或实际行为仍需测试。不得把源码存在、上游报告或单测源码误写成此次实机验收通过。

## 项目背景与长期愿景

### 三个项目的分工

| 项目 | 当前定位 | 车包职责 |
|---|---|---|
| KuaYue | 基于 Create 和 KasugaLibNext 的铁路模组 | 定义 pack/parcel 协议、车型内容、依赖和开关，调用通用构建与资源管线 |
| KasugaLibNext | 注册、模型、动画、渲染及数据驱动基础库 | 提供来源、统一构建、校验、诊断和资源适配；不认识车包业务 |
| KasugaLib-BlockBenchPlugin | Blockbench 模型组件编辑插件 | 运行时协议稳定后，读取消费端 schema 并导出可安装车包 |

Create 提供铁路游戏机制，Blockbench 提供模型和动画创作。先验证手写车包运行时闭环，插件通用表单和导出不阻塞 v1。

### 目标链路

```text
作者制作模型、动画、内容定义与元数据
→ 手写或由插件导出 pack
→ 玩家将目录或 ZIP 放入 <game-dir>/kuayue/packs/
→ 配置整包开关与覆盖顺序
→ 启动/重启：发现、校验、冻结来源、统一注册、加载资源
→ 使用新增车型
```

一个 **pack** 是安装与启禁用单位，包含多个 **parcel**；parcel 是车型组织单位，无独立开关。长期内容包括方块、模型、纹理、声音、本地化、UI 主题、配方、tags、loot，以及可能的蓝图。文件可打包不等于已有运行时消费者。

KuaYue 自带内容作为内置 `kuayue-core`，默认启用、允许整包禁用与排序，重启生效。namespace 保持 `kuayue`，已有游戏 ID 不变；关闭内容不卸载 runtime、工厂、handler 或共享基础设施。

数据驱动是“数据组合已有行为”，不是任意 JSON 创造行为。Schema 必须有对应 Java handler 和消费者；`on_register` 是启动处理域，不意味着每种定义都创建方块/物品；`on_reload` 可加载状态机、动画等非注册定义。

## 0. v1 范围与阅读导航

v1 支持目录和 ZIP 直接安装、整包开关、多 namespace、跨包资源覆盖、游戏内排序、corepack 禁用、启动期统一注册和声明版本兼容。所有存档共用启动集合；打开世界和连接服务器不重新选包。

- 第 1–10 节：产品规则、来源、生命周期、依赖与 corepack。
- 第 11–19 节：路线、风险、项目分工和必须补齐的能力。
- 第 20–24 节：执行基线、完整文件契约、VFS 边界、构建和存档规则。
- 第 25–27 节：任务、就绪检查和非注册内容路线。
- 第 28 节：更新后的代码证据和本次验证记录。

后文 API 名称中明确标为“计划”的项目不代表已有代码；内部命名可调整，公开格式与 SPI 调整必须同步文档、schema、fixtures 和测试。

## 1. 目标、非目标与成功标准

### 已确认目标和规则

- 安装目录固定为 `<game-dir>/kuayue/packs/`；直接子目录或直接子 ZIP 对应一个 pack，根直接含 `pack.json`，不需要玩家解压。
- 整包开关和资源顺序保存在 `<game-dir>/kuayue/config.json` 的 `packs.enabled` / `packs.order`；只有一个实例级入口。
- 外部新包默认禁用，只有明确 `true` 才请求加载；首次启动前可手写配置。corepack 默认启用，禁用后不自动恢复。
- 玩家可在游戏内调整包括 corepack 在内的覆盖顺序，保存只影响下次启动，不立即重载或注册。
- 同一包可以提供多个 namespace，多个包可以共享 namespace。普通资源可覆盖 `kuayue` / `minecraft` 等路径，不要求排他 namespace 所有权。
- 所有依赖都是必需依赖；缺失、禁用、版本不满足或依赖环阻断相关包。不自动启用、下载或补齐依赖。
- 内容引用只允许本包与 manifest 显式依赖；A→B→C 不授权 A 直接引用 C。
- 同包同版本内容可以不同，不用 hash 阻断存档或多人连接。版本兼容依据作者声明，而非文件一致性。
- 注册同名方块/物品不在 v1 支持范围；最终 registry ID 冲突按注册冲突拒绝，资源顺序不授权替换已注册对象。
- 启动期非注册配置可按优先级覆盖。纯原生资源、仅 reload、混合内容遵守同一套包规则，不增加类别豁免。

### 非目标

运行中安装/卸载生效、热注册、parcel/存档开关、进入世界自动换包、科技树、研究/解锁、自动下载同步、任意 Java/JS/handler 上传、通用 schema 反射编辑器、已注册对象动态替换，均不属于 v1。

### 成功标准

1. 不改 Java、不重建 KuaYue，仅安装并启用一个外部 pack，重启后出现完整的新车型。
2. 禁用后重启，该包不注册、不挂载；独立有效包继续，依赖者被阻断。
3. 无效包诊断给出 pack/parcel、文件、字段/引用及原因，没有静默部分注册。
4. 内置与外部内容经过同一协议、预检和统一注册会话。
5. corepack 禁用且零外部包仍能启动；独立外部 bogey 等可使用 runtime。
6. 目录/ZIP/JAR 同逻辑内容得到等价的解析与资源行为；多 namespace 和资源栈合并正确。
7. 切换世界或连接服务器不改变启动集合；世界历史用该世界的 `data/kuayue_packs.dat`。
8. 纯资源包可使用 `indexes: []`；仅 reload 包无需创建 block/item；两条路线都有真实消费者验收。

## 2. 更新后的实现边界

### 源码已有

- `modules/core` 已有跨平台 VFS：读写、元数据、枚举、ZIP/JAR、叠加、挂载、事务和变更订阅。VFS 包没有 Minecraft 客户端依赖。
- `VfsPackResourcesBridge` 可暴露标准 `assets/<namespace>/` / `data/<namespace>/`；`ScopedResourceManager` 可适配 `VfsBackedPackResources`。
- `GeneratedShaderResourceProvider` 使用内存 VFS，`KsgPackResource` 接入 VFS，`LiveModelSync` 用目录 VFS 管理开发预览。
- 数据驱动已有 handler phase、分类型身份解析、索引 register/reload 分域、重复定义 resolver 和 reload 扩展。
- KuaYue models/motions 编译 metadata，FSM/clip 使用 reload bucket，非注册内容能力不是纯设想。
- #87 新增 `ResourceSources`、`InputPolicy`、`ResourceSnapshot`：实际捕获字节、SHA-256、两次一致性检查（最多三轮）、内存/暂存文件和读取 lease。归档严格入口独立于旧 ZipVfs 宽容索引。
- `ResourceUri`、`MinecraftNameCodec`、`ResourceCatalog` 和条件查询已实现；保留 source/root 提供者及 rank，先可见性后选胜者，类型不匹配不向低层回退。
- `ResourceRouting` / `StrictResourceJson` 已实现模板继承、整输出替换、显式多分发、路径段最长匹配及结构优先/无父回退。实际启动事件名称为 `bootstrap`，不是 plan_vfs 示例中的 `host_bootstrap`。
- `ApprovedResourcePacks` 已订阅 AddPackFindersEvent；每层生成 `MappedPackResources` 并提供默认 pack metadata。`ScopedResourceManager.managedView()` 访问真正 MC manager 的 stack。
- `ResourceCoordinator` / `ResourceGeneration` 已实现批次 prepare、发布 executor、失败清理、旧代际 lease 与退役释放；ResourceSystem 已有 process/client/server scope 和游戏关闭 hook。
- `ModelResourceSource` 已处理非 MC bbmodel 的内嵌 PNG，保留原文件、生成子纹理/模型副本并提前投影；新模型 scanner 会发现 `kasuga_generated:models/v1/...`。
- 新脚本平台有显式 block 注册、参数/结果契约、非阻塞 Ticket、有界请求环、代际撤销和 ResourceAccess；是 opt-in 基础，非车包功能。

### 仍未实现

- `JsonTreeBuilder.buildForMod()` 仍通过 `ModList` / `IModFile` 读取模组来源，未接 VFS 或外部 `ContentSource`。
- `BuildContext` 仍以单个 modId 表示上下文，没有 host/owner/source/namespaces 的完整分离。
- `TypeHandler` 没有严格预检能力、registry claims、会话 finalizer 等新 SPI。
- `JsonTreeIntegration` 仍固定调用 `buildForMod()` 并捕获异常；没有宿主 provider。
- KuaYue 仍在 `KuaYueApplication.init()` 调用 `KuaYue.REGISTRY.register()`；未发现其生产树中的外部 `pack.json` 实例。
- 车包 manifests、配置、依赖求解、`StartupPackSnapshot`、游戏内顺序界面和 SavedData 历史仍是计划新增；不要与已有通用 ResourceSnapshot 混淆。
- `ApprovedResourcePacks.discover()` 每次重新 capture；资源注册不会自动保证“运行中源文件变化不生效”。固定来源 adapter 尚需车包实现。
- 目录 wrapper 在节点/link 检查后仍调用 DirectoryVfs 构造全量哈希；按 root 的 InputPolicy 不自动汇总为按 pack 限额。Windows junction、初始扫描限额与竞态仍需补测/补齐。
- `MappedPackResources` 是普通 PackResources，不实现 VfsBacked；现有 `getPackResources()` 分支不会把它加入旧 scoped 清单。普通 ResourceManager/managedView 可见不等于旧逐包消费者已迁移。
- 各格式模型来源/外部依赖迁移、通用不可信脚本沙箱和任意 MC/GPU 原子回滚不由本次实现全部完成。

**设计约束：**普通资源可见性不等于启动注册可用性。注册来源必须在首次注册分发前确定，不能等到 `ResourceSystem` 的客户端 setup 或服务器 starting 回调再选包。

## 3. 核心概念与身份

| 概念 | 语义 |
|---|---|
| Pack | 安装、启禁用、依赖、版本和世界兼容的最小单位 |
| Parcel | 包内车型与元数据组织单位，无独立开关或版本 |
| Corepack | JAR 内 `kuayue-core`，默认内容，允许禁用；不含 runtime 基础设施 |
| ContentSource（计划） | 给注册解析提供稳定文件清单、入口、受限读取与 provenance |
| Resource layer（平台已有，车包接入待做） | ResourceCatalog / MappedPackResources 保留真实 owner/root/rank，不能只有胜者视图 |
| StartupPackSnapshot（计划） | 本进程固定的有效包集合、顺序、定义来源和稳定文件内容视图 |

明确分开 `hostModId`、`ownerId`、`sourceId`、资源 namespace 和 registry ID。已有 `ResourceSourceDescriptor(ownerId, sourceId, rootId, rank, origin)` 不含 namespaces、pack/parcel 和入口索引；这些由车包/注册 adapter 补充，不再复制另一套文件身份。

#87 已新增 URI、逻辑 key、类型/子类型/tag 目录和 MC 名称 codec，推进了 Issue #49 的统一身份目标；尚无所有模型消费者迁移或整个 Issue 已验收的证据。车包内部可复用平台身份，作者仍用已确认的 pack/parcel/原生资源 ID，不改写 `kuayue:*`。`VfsPath.hashCode()` 是普通对象哈希，不是资源名称替代。

## 4. 安装、分发与元数据

```text
<game-dir>/kuayue/packs/example/      # 或 example.zip，ZIP 内不含 example/ 外层
├── pack.json
├── assets/<namespace>/...
├── data/<namespace>/...
└── parcels/<folder>/
    ├── parcel.json
    ├── assets/<namespace>/...
    └── data/<namespace>/kasuga_lib/data_driven/<index>.json
```

安装目录名和 ZIP 文件名不决定 pack ID。内置 corepack 从 JAR 内发现，不复制为外部安装包。相同 pack ID 的目录和 ZIP 同时安装是身份冲突，不能用优先级择一。

- pack manifest：格式、稳定 ID、版本、namespace 集、翻译键、parcels、必需依赖、自身历史版本兼容声明。
- parcel manifest：格式、稳定 ID、翻译键、图标、索引列表；没有 enabled/version/dependencies。
- config：实例级开关、覆盖顺序、安装清单；不保存世界历史。
- index：沿用 Kasuga 的 `on_register` / `on_reload`，不另造方块 schema。

manifest 路径相对 pack 根；parcel 索引相对 parcel 根；索引内容条目相对该逻辑 `data/<namespace>/`。未经验证的作者路径不得直接交给宽容的 scoped adapter。

## 5. 启动加载生命周期

```text
显式 bootstrap
→ 安全发现/读取配置与 manifests
→ 必需依赖和版本校验
→ 受限捕获文件内容、逻辑层与 provenance
→ 解析全部定义、配置胜者选择、只读预检和 claims
→ 整包拒绝及依赖闭包，重新预检到稳定集合
→ 冻结 StartupPackSnapshot
→ 单一构建会话、一次 finalizer、一次 attach
→ 原有 NeoForge 注册事件提交
→ 客户端/服务器从同一快照构建资源栈
```

资源发现若早于最终准备结果，可先发布只读发现配置；实际消费前必须确认严格准备成功。事件、总线和 happens-before 由 E0 验证，不假设不同 `@PostConstruct` 的顺序。

- 外部包预检失败：整包及其依赖者拒绝，独立有效包继续；不只关闭错误 parcel。
- corepack 禁用：正常配置；零包返回空根，不回退旧 JAR 扫描。
- 已启用 corepack 自身预检失败：发行缺陷，本次启动失败。
- apply/finalize/注册提交意外失败：本次启动失败，不承诺撤销已发生副作用。
- 普通 reload 失败：沿游戏错误处理并报告，不临时缩小包集合或移除注册项。

快照必须同时固定集合、顺序和文件字节。已有 ResourceSnapshot 可固定一次捕获的字节，但 ApprovedResourcePacks/coordinator 下次调用 capture 可能捕获新内容。StartupPackSnapshot 必须持有全启动周期的根快照，所有注册解析、后续 discovery/reload 从它读取；只冻结 List、保留打开的 ZIP 或直接注册 raw ResourceSource 都不够。

## 6. 内容来源接口方向

### Kasuga 通用来源

计划在 `registration.data_driven.source` 新增薄注册来源 adapter，文件身份/读取直接复用 #87 core source，不另建 LocatedFile 或 source backend：

```java
// 计划 SPI 示意，不是当前代码；实际类型名称可以调整。
interface ContentSource extends AutoCloseable {
    ResourceSourceDescriptor descriptor(); // 复用已有 owner/source/root/rank/origin
    Set<String> namespaces();              // 注册/包协议补充，不由 sourceId 猜测
    List<ResourceSnapshot.LocatedResourceFile> files();
    List<LocatedIndex> entryIndexes();     // 计划新增的入口定位，关联已有 file
}
```

来源需补充 host-independent 的 namespaces、pack/parcel、入口 namespace 和诊断映射；owner/source/root、逻辑路径、revision 与 rank 已由 core descriptor 提供。读取使用 LocatedResourceFile.open(lease)，区分不存在、无权限、越界、损坏与失效；调用方关闭流。

使用平台 `LocatedResourceFile` / ResourceUri 准确打开原定义，避免 ResourceCatalog.find 或 MC 全局覆盖改变注册候选。本包共享内容查找可用限定 owner 的 stack/find，但要保留最终定义 provenance。

已有 `ResourceSources.mod()` 支持展开目录或普通归档；仍要将 IModFile 的实际根/内嵌根接入并做开发环境 fixtures，不强制将虚拟子根当物理 ZipFile 路径。平台 `vfs(..., sourcePrefix, logicalPrefix, ...)` 只改逻辑路径，URI path 仍是原 raw 路径；车包建立 pack/parcel root 时需明确具体 URI 根相对路径，并避免重复捕获和双重拼接。

### KuaYue 和插件

KuaYue 选择有效 owner、构造 roots 和显式依赖权限，提供 bogey/track 业务校验；Kasuga 不扫描 KuaYue 安装目录或解析包依赖。

插件后续读取静态 schema/editor hints，导出暂存目录、验证后发布。模型 JSON 的 `kasuga_lib` 扩展与注册索引不是同一种机制；live preview 不等于可安装车包协议。

## 7. ID、依赖、覆盖与资源合并

### 三类冲突分开处理

| 类别 | 身份域 | v1 行为 |
|---|---|---|
| 普通资源 | 逻辑资源路径 | 最高优先级单读取；需要合并的消费者读取完整栈 |
| 非注册配置 | `DefinitionKey(type,id)` | 按冻结 rank 选胜者，再校验、编译；下层不 apply |
| 注册占用 | `registry/domain + effective ID` | 保留所有注册候选检查 claims，拒绝冲突；不 last-wins |

`on_register` 可以混合配置和注册型定义，不能整文件覆盖后丢掉下层注册项。入口索引和原定义按来源读取；配置候选在类型级选择。

### 依赖与引用

- 依赖图基于 pack ID，不基于 namespace；检查缺失、禁用、版本不满足与环。
- 只允许自身和显式直接依赖的定义，不采用传递可见性。
- 普通资源覆盖本身不强制依赖目标包；真正需要其内容时作者显式声明。
- 覆盖不能反向制造依赖：A 本来引用自己的 ID，B 提供高层替代，不要求 A 依赖 B；B 替代定义内部的引用按 B 的依赖权限验证。
- 引用校验保留真实 owner；不能按 `kuayue` namespace 猜定义来自 corepack。

### 资源顺序

`packs.order` 低到高，依赖拓扑不重排它。包内根最低，然后 manifest parcel 顺序低到高。单资源取最高层；语言、tags、sounds 等沿真实游戏消费者的 stack/replace 语义，不发明通用 JSON 深合并。

`CompositeVfs.detectConflicts()` 可提供同路径来源线索，但这些是普通覆盖事实，不是 registry claims，也不能直接按“冲突”拒绝车包。

## 8. 实例配置、世界兼容与多人

“已安装”“请求启用”“有效加载”分别报告。扫描只补 inventory，不改开关，不自动启用依赖。运行中配置修改只影响下次启动。

世界历史用 Overworld 的 SavedData，记录 `(pack id, version, format_version)`；不包含 enabled、资源顺序或 hash。它只检查固定启动集合，不参与选包。

同版本内容变化直接接受，作者承担风险。跨版本依据当前包自己的 `save_compatibility`；不构造传递升级闭包。禁用不会自动清理世界旧方块，界面必须警告。

缺历史和基本缺包/多人版本差异的处理门槛仍待产品确认，不擅自提供强制继续、推测版本或删除内容。已知历史的未授权升级/降级需阻止正常载入。

多人文件分发由作者/服主管理；KuaYue 不下载、不同步、不自动换包，也不按同版本字节差异阻断。可交换 ID/声明版本/格式供诊断，具体连接检查对齐原有 registry 协议。

## 9. Corepack 迁移

1. 建立版本控制下的 runtime/corepack 归属清单，覆盖 JSON、旧 Java 静态注册和所有最终资源。
2. corepack 专属内容建议输出到 JAR `kuayue-packs/kuayue-core/`，含 manifest、assets、data；runtime 资源留在普通根。
3. 不能把同一份 corepack 内容同时输出到 JAR 根和内嵌根；否则禁用仍可能被 vanilla 自动发现。
4. corepack 与外部来源走同一构建/资源管线，ID 保持 `kuayue:*`。
5. 分批验收简单车型、bogey、track、餐食，再到 QJ 复杂模型；golden 比较 ID、属性、动作和资源行为。
6. 若采用过滤 adapter 替代内嵌根，必须同时过滤注册索引和 vanilla 资源，不只过滤 Kasuga 模型 loader。

corepack 的子根、parcel rebasing 和 manifest 映射仍由 L1a/K3 接入；复用 ResourceSnapshot/ResourceSources，而不是再写归档 backend。注册和 MC 投影必须引用同一捕获字节。

## 10. 内容、蓝图与编辑器范围

v1 不包含科技树、研究、解锁字段和行为。蓝图仅在定义了独立列车拼装消费者后再考虑，不能因文件存在就纳入必需运行时。

名称、介绍、分类和图标可用于内容浏览，但不引入科技解锁。插件的 diamond/IK/物理/粒子与 live preview 是模型能力，不代替 pack manifest、依赖、注册和版本管理。

## 11. 分阶段实施路线（v2）

| 阶段 | 工作 | 完成门槛 |
|---|---|---|
| A：E0 与协议基线 | 生命周期、归属、配置/schema fixtures、更新后基线 | 事件证据及边界明确；不是只看源码存在 |
| B：L1a/L1b | 复用资源平台，补注册入口/根映射、整包预算和启动冻结 adapter | 三来源等价，安全补齐/固定内容/句柄通过 |
| C：L2/L3/K1 | 只读预检、claims、统一会话/provider、包配置与依赖 | apply 前拒绝错误；零包不回退；一次 finalizer/attach |
| D：R1/K2/NR1 | 批准资源包集成、业务校验、共享 runtime、配置与 reload 选择 | 新车型和两种纯资源路线实际加载 |
| E：O1/K3 | 排序界面、corepack 重打包与旧内容迁移 | 四种启动环境、无资源/静态注册泄漏 |
| F：S1/V1 | 世界保护、多人元信息、完整矩阵与发布 | 明确未决策略后补验收，不用 hash 强一致 |
| G：插件导出 | 消费端 schema、pack/parcel 表单、验证后发布 | 与手写包同加载器，不留下半成品 |

#87 已提供阶段 B/D 的大量通用底座，相关任务改为 adapter/策略与集成，不重复开发目录、URI、通用捕获或 MC stack。但未执行当前环境 Java 测试，且车包入口、整包预算和启动冻结未实现，不能标阶段 B/D 完成。阶段 C 的注册管线仍是主要关键路径。

## 12. 验证矩阵

| 场景 | 预期 |
|---|---|
| 无外部包、corepack 默认启用 | 旧内容 golden 一致 |
| 有效目录/ZIP/JAR 内容 | 等价的逻辑定义、注册和资源；ZIP 无须用户解压 |
| 同 pack ID 重复安装 | 身份冲突，非资源顺序择一 |
| 外部包未明确启用 | 默认不加载；首次启动前手写 true 可启用 |
| 配置损坏、错误类型/版本 | 阻止启动，不覆盖原配置或自动恢复 corepack |
| `indexes: []` 纯原生资源 | 零 claims 合法，资源真实可见 |
| 仅 `on_reload` | bucket 和消费者使用内容，不新增方块/物品 |
| 多 namespace / 共享 namespace | 合法；真实 owner 与依赖仍可定位 |
| 两包同普通资源路径 | 按 rank 覆盖，诊断信息级，不拒包 |
| 语言/tags/sounds 多层 | 单资源与栈读取均符合实际消费者语义 |
| 同非注册 model/motion/clip ID | 类型域内按 rank 选胜者，不按文件名字典序猜顺序 |
| 不同 handler 相同最终 registry ID | apply 前识别并拒绝注册冲突 |
| 混合配置和注册文件 | 不因配置覆盖丢掉注册 claims |
| 错字段/引用/未知 factory/group | 严格预检拒整个 owner，其他独立 owner 可继续 |
| 缺依赖/禁用/版本不符/环 | 阻断依赖闭包，不自动启用或下载 |
| A→B→C，A 未声明 C | 拒绝直接引用 C |
| A 自身 ID 被 B 覆盖 | 不强制 A 反向依赖 B；B 内部引用按 B 校验 |
| 绝对路径、反斜线、空段、`.`/`..` | 原始输入即拒绝，不先归一化 |
| 目录 symlink/junction、ZIP symlink | 拒绝，不静默跳过；根外内容不读取 |
| 重复/非法/截断 ZIP、压缩炸弹 | 稳定诊断、受限读取、无部分成功 |
| 文件数/单文件/包总量超限 | 尽早中止，不能全读后才检查 |
| 原安装目录/ZIP 运行中变化 | 本次资源字节与集合不变 |
| 多次 repository discovery/reload | 冻结 adapter 每次提供原启动字节，不再次读取安装来源 |
| 多 parcel/roots 合计超限 | 以整个 pack 汇总，不能每个 root 各获 1 GiB/50000 额度 |
| 旧 scoped 与 MappedPackResources | managedView/实际消费者迁移可见，无遗漏或重复扫描 |
| source/root rank 冲突、parcel ID 含冒号 | 宿主分配合法稳定 ID 和互不冲突的 rank，不用 hashcode 或 namespace 猜测 |
| Windows 保留名及大小写差异 | 容器边界和缓存映射有明确行为，无静默改名/漏资源 |
| 内置服务器关闭再开/切换世界 | 进程快照仍可用，世界句柄不串用，client 资源不误关闭 |
| 游戏内改顺序/开关不重启 | 只保存待生效设置，当前 stack/注册不变 |
| corepack 禁用、零包 | 启动和空内容测试世界可用，无默认资源泄漏 |
| corepack 禁用、独立外部 bogey | runtime/共享 BE/图标可用，不隐式加载默认车型 |
| corepack 禁用、依赖者存在 | 阻断依赖者，不恢复 corepack |
| corepack 重排重启 | 原 ID 不变，资源胜者正确 |
| 同包同版本不同字节/顺序 | 不因 hash 或顺序拒绝世界/多人 |
| 合法/未授权升级、降级 | 依据自身版本声明；无传递升级闭包 |
| SavedData 复制/改名/不同世界 | 历史随存档，Overworld 一份，不串数据 |
| 缺历史/损坏历史/合法空历史 | 三者区分；缺历史策略另行确认，损坏不覆盖 |
| reload RESOURCE 与 SCRIPT | 不清除/覆盖代码来源；包集合不随 reload 变化 |
| 致命 apply/finalize/commit | 启动失败，不声明文件事务能回滚注册 |

每条矩阵需记录真实命令、退出码、报告、跳过项和 fixture 来源；源码清点不能替代实机行为。

## 13. 主要风险与待验证事项

1. **注册时序：**`KuaYueApplication.init()` 与 lazy JSON dispatch 仍需显式 bootstrap/provider 顺序。
2. **身份污染：**`BuildContext.getModId()`、旧根 namespace 属性及 metadata key 不能直接承担多 owner。
3. **严格模式：**旧重复 last-wins、WARN 吞错、缺 group 挂根需保留 legacy，但不可沿用为车包成功语义。
4. **来源提前读入：**ResourceSources.directory 先校验节点/link 后仍构造 DirectoryVfs；初始全量哈希没有 InputPolicy 字节预算，需上游补齐或有界入口，不认为 capture 限额覆盖此前成本。
5. **严格范围：**归档重复/link/CRC/实际读取限额已有；暂不支持 ZIP64/加密/分卷，需明确提示。Windows junction、路径替换竞态和 JSON 深度在各消费者的落地仍须验证。
6. **启动冻结：**ApprovedResourcePacks 每次 discovery/source.capture 会读新内容；需从固定根快照提供可重复 capture，不能每轮重新扫描安装目录。
7. **分层接入：**Catalog/MC stack 已有；仍补 pack/parcel rebasing、全局唯一 rank、仅显式依赖及旧 scoped 消费者迁移。
8. **资源寿命：**server/process 清理缺陷已修复，GameShuttingDownEvent 已绑定；lease、disposal executor 寿命、finder与coordinator独立 owner 仍需集成测试。
9. **预算与缓存：**InputPolicy 按 source/root，车包应按 pack 聚合；通用私有 spool 已避免原样物理文件名，持久缓存与全启动复用不是已有保证。
10. **corepack：**Java 静态注册、编译输出及 vanilla 资源自动发现仍可能绕过开关。
11. **world safety：**已有 SavedData 包装绑定 `ServerStartedEvent`，不证明缺包检查足够早。
12. **验证环境：**当前 Linux 环境未找到 Java，JVM/FML/GUI 行为待补测；不修改测试过滤或下载私有模型绕过验收。

## 14. 现有内容作为迁移基线

现有 KuaYue 的车厢索引、bogey、track、provisions 和相关 tests 仍可作 golden fixtures。原方案的 201 条车厢、5 条轨道、53 组 bogey 数据是历史源码清点，不是本次更新后的运行计数；E0 必须从最终编译资源和实际注册重新生成清单，不直接继承数字作为验收结果。

检查 JSON 与旧 Java 并存路径，例如 locomotive 包中的静态 `.setParent()`，以及 `KuaYueTrainPanelModule` 的 DF11 图标；只迁移 JSON 不足以让 corepack 可禁用。

## 15. 两个运行时项目的职责边界

### KasugaLibNext

通用 VFS、只读来源/逻辑层、统一准备与构建、严格能力、定义目录、claims、finalizer、结构化诊断、scoped/标准资源适配。通用模块不 import KuaYue 类型，不读取其 config 或包依赖。

### KuaYue

manifest/config、安装发现、有效 owner 选择、必需依赖、版本兼容、车包快照编排、业务 handler、共享 runtime、资源优先级 UI、corepack 重打包和世界历史。通用捕获工具可放 Kasuga，车包限额参数和包策略由 KuaYue 提供。

### 不增加的职责

Kasuga 不成为通用车包 manager、依赖求解器、网络市场或同步器。插件不参与首次运行时注册验收。父目录不是 Git 仓库，各仓库变更分别报告；不修改无关工程。

## 16. Kasuga v1 必须补齐的能力（不是重写 VFS）

### 16.1 来源 SPI 与逻辑根

只新增注册专属 `ContentSource` / `LocatedIndex` 与上下文 adapter；复用已有 ResourceSourceDescriptor、ResourceSnapshot.LocatedResourceFile、ResourceCatalog 和 lease。不再新增另一套 SourceDescriptor/LocatedFile backend。补 pack/parcel root rebasing、namespaces/入口目录和稳定 rank；所有注册解析读取固定捕获字节。

`VfsWriteTransaction.ContentSource` 是延迟打开输入流的函数接口，与注册来源 SPI 不同，不因名字相同认为已完成。

### 16.2 多来源准备与构建

```java
// 计划 API。
PreparedContent prepare(Collection<ContentSource> sources, BuildOptions options);
BuildResult build(PreparedContent content, RegistrationTarget target);
```

先解析、配置胜者选择、校验、claims，再构建；全部有效来源进入一个会话。保留原定义 provenance，不仅合并 JSON；旧 `buildForMod()` 保留兼容包装。

### 16.3 身份和 metadata 上下文

区分 hostModId、ownerId、sourceId、namespaces、当前定义/handler/文件/JSON Pointer。显式 ResourceLocation 不被统一重写成宿主 namespace；metadata 以定义目录及来源作用域解析，不能依赖 namespace 排他性。

### 16.4 无副作用预检与能力声明

给 handler 增加计划中的 `validate(definition, ValidationContext)` 及严格能力声明。parse 验字段形状，validate 验工厂、组、属性、强引用；apply 构建对象而非唯一验证入口。

空 default validate 不能证明严格就绪；缺能力报 `HANDLER_NOT_STRICT_READY`。旧接口仍保留宽容行为，但车包不会因 WARN、挂根或跳过 entry 被视为成功。

### 16.5 跨 handler RegistryClaim

声明 `registry/domain + final ID + owner/source`，包括派生 item 和共享对象预留 ID。不同类型产生相同 Block 必须冲突，不采用分类型 dedup 的 last-wins。资源重复与 claims 重复分开。

### 16.6 一次 finalizer

全部相关 handler 完成后执行一次 build-finalizer，收集所有有效 bogey block，创建唯一共享 BE 和完整 validBlocks。零 bogey 不创建，不报缺少块错误；共享对象 claims 在 finalize 前可推导或预留。

### 16.7 结果对象和诊断

`PreparedContent` 包含不可变定义目录、claims、候选来源和 diagnostics；`BuildResult` 包含注册根及 finalizer 结果。Kasuga 报事实，KuaYue 决定整包拒绝和依赖闭包。

### 16.8 普通资源适配

优先复用 #87 的 ApprovedResourcePacks / MappedPackResources / McManagedResourceView，不再自建通用完整 stack。`ReloadOrchestrator` 仍读正常 ResourceManager，注册输入另由 ContentSource 给出。旧逐 pack scoped 消费者需接 managedView 或补明确 adapter：当前 MappedPackResources 未实现 VfsBacked，不能声称旧 getPackResources() 已覆盖新投影。

## 17. KuaYue v1 必须实现的业务

1. Pack manager：安全发现、实例配置、固定有效集合、可诊断安装状态。
2. Protocol：manifests、namespace、必需依赖、版本范围、自身升级声明。
3. Sources：corepack 和外部 roots 映射到 Kasuga 来源，提供真实 owner/parcel。
4. Validation：bogey/track/车型强引用、handler 严格能力和最终占用声明。
5. Runtime：共享 bogey BE finalizer、安全创造栏图标、零包通用能力。
6. Resources：同快照双侧资源栈和 provenance，原生与自定义非注册内容统一规则。
7. Ordering：游戏内顺序、待生效提示、冻结本次栈，不用依赖排序代替玩家顺序。
8. Corepack：重打包、旧 Java/JSON 迁移、资源隔离和 golden 回归。
9. History：Overworld SavedData、声明版本校验、世界引用清理，待确认策略单独阻塞。
10. Multiplayer：环境元信息诊断，不下载、不自动换包、不以 hash 强一致。

## 18. 延后和非必需底层功能

网络来源、安装器、通用包管理、任意脚本/handler 上传、科技树、提交后全面 rollback、热注册、存档开关均不在本期。

已有 VFS 跨后端写事务可用于缓存工具，但不是注册原子性的实现；缓存可用暂存目录和发布边界。`watch` / `VfsWatchService` / live preview 不启用为安装车包自动重载。

需要的是受限读取、稳定内容和资源分层能力；这些已有通用实现，车包聚焦其适配与剩余限制。#87 的 URI/类型目录/路由和脚本块不要求车包修改既有资源 ID，也不开放任意 JS。`ManagedScriptEntryPoint`、ScriptTicket 和可写内存 Workspace 不进入本期车包必需任务。

## 19. 推荐实现顺序

1. E0：补 JDK、重跑 #87 平台/旧入口基线；通用 finder 和 scope 已有，验证其与 KuaYue bootstrap/注册/世界 hook 的顺序。
2. K1 parser/config fixtures 与 L1a 薄注册 adapter 可并行，以已有 ResourceSnapshot 或 fake source 解耦。
3. L1b：补整包预算、目录初始扫描边界和冻结 source；已有严格归档/字节捕获不重写。
4. L2 严格定义目录/claims，L3 会话/provider，保持旧入口 golden。
5. R1：固定根来源注册到 ApprovedResourcePacks，验证 discovery/reload 不读安装目录、双侧 stack 和生命周期；先简单 M1，再 K2 bogey/track 与 NR1。
6. O1 与 K3，验收 corepack 四种环境和完整资源合并。
7. 明确缺历史/基本缺包策略，完成 S1 和 V1；插件最后。

PR #87 已提供 URI、来源捕获、路由、MC finder、scope 和模型适配；不等待 plan_vfs 所有格式/脚本沙箱构想完成。剩余关键路径是车包启动冻结/策略接入、L2/L3 注册安全与业务迁移。

## 20. 执行规则与决策状态

- 产品规则（ZIP、排序、多 namespace、重启生效、仅整包开关、必需直接依赖、同版本字节宽松等）已确定，不重复提问或降为待确认。
- 字段与 SPI 为执行基线，内部命名可变；公开契约变动同步 schema、示例与测试。
- 平台已有临时捕获、AddPackFindersEvent 和关闭 hook；车包持久缓存布局、冻结来源实现以及 KuaYue happens-before 仍是待验技术方案，不把平台实现当车包集成通过。
- 首次缺历史、基本缺包/连接版本差异处理门槛仍待确认；不得以采用 SavedData 或严格模式替用户决定。
- 每个任务包含输入、输出、修改仓库/模块、依赖、测试、证据及剩余阻塞。
- 不运行用户世界，不修改测试过滤掩盖失败，不使用 `-x test` 作为验收。

## 21. 文件格式、逻辑映射与严格输入契约

### 21.1 安装与配置

ZIP 扩展名发现不区分大小写；不递归寻找嵌套安装包。ZIP 根必须直接含实际内容。

```json
{
  "packs": {
    "format_version": 1,
    "enabled": {"kuayue-core": false, "example-railway": false},
    "order": ["kuayue-core", "example-railway"],
    "inventory": []
  }
}
```

这是统一 config 的片段，无关顶层子域保留。文件或 packs 子域缺失用默认值；已有 JSON 损坏、错误类型或版本阻止启动，不覆盖损坏文件。未知安装 ID 保留并警告。

order 低到高；缺省排序技术建议 corepack 最低、其余稳定 ID 补齐。order 中出现不代表启用；不存在/禁用 ID 保留但不入有效栈。UI 显示当前/待生效状态，仅保存下次启动设置。

### 21.2 Pack manifest

计划发布 `KuaYue/doc/schemas/packs/v1/{pack,parcel}.schema.json` 及 config packs 子域约束。

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
  "save_compatibility": {"minimum_version": "0.2.0"}
}
```

版本范围是协议示例，必须核对实际运行时元数据，不由 Git 提交或文件名推定。

| 字段 | 约束 |
|---|---|
| format_version | 必填整数 `1`，未知版本拒绝 |
| id | 必填稳定 ID，建议 `[a-z][a-z0-9_-]*`；外部不得声明 `kuayue-core` |
| version | 必填三段 SemVer 发布版本，无 prerelease/build suffix；与格式版本分离 |
| namespaces | 必填非空、无重复数组，合法 `[a-z0-9_.-]+`；不跨包排他 |
| name_key | 必填非空翻译键；缺翻译回退原键、警告，不阻止专用服务器 |
| description_key | 可选非空翻译键 |
| dependencies | 必填 mods/packs 映射；仅必需依赖，无 optional/下载 URL |
| parcels | 必填非空、无重复，路径 `parcels/<folder>/parcel.json`；未列出的 parcel 不加载 |
| save_compatibility | 可选；精确历史版本集合或最低历史版本，互斥，见 24.3 |

外部包必须声明示例中的五个基础 mod 兼容范围；corepack 使用宿主内置描述，不依赖自身 pack。范围只支持 `*`、精确字符串、双界半开 `[lower,upper)`；pack 用三段 SemVer，mod 用 NeoForge 版本比较，不自行词典序比较。

manifest/config packs 子域拒绝未知字段、重复 JSON 对象键、错误类型、注释和尾逗号。可复用 StrictResourceJson 的 token/depth 检查，再用车包字段/schema 校验；平台的 routing/template schema 不是 pack schema。manifest 不含 enabled/override/scripts/tech_tree。

KuaYue 从有效 manifests、固定 roots 和 order 在 Java 中生成 ResourceRouting binding，使用 `minecraft_resource_schema`。不新增车包必填 kasuga.resources.json、不另建开关配置；作者自带路由不能自动生效。只允许宿主批准的输出，注册消费者不允许 resource_reload/remote_connect。平台真实事件名为 `bootstrap`；批准 MC pack 注册要在 discovery 前完成，不将 MinecraftResourceEntryPoint（只接受 resource_reload）错误绑定 bootstrap。

### 21.3 Parcel、入口与根映射

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

parcel ID 是合法 ResourceLocation，namespace 必须已声明、包内唯一。图标是图片路径，不是 item ID。indexes 必填，可空；索引 namespace 从路径获取，位于 `data/<namespace>/kasuga_lib/data_driven/`。

pack 根按 `(namespace, 文件名)` 升序发现索引；parcel 只读声明入口。去除 pack/parcel 物理根后，assets/data 原样成为逻辑路径：

```text
parcels/m1/data/example_railway/carriages/m1/blocks.json
→ data/example_railway/carriages/m1/blocks.json

索引：{"on_register":["carriages/m1/blocks.json"],"on_reload":[]}
→ 内容相对逻辑 data/example_railway/，不是再拼 parcels/m1/
```

索引可引用本包共享根中的内容，不能按路径跨包读文件。注册型入口按具体来源保留，不因一个高层同名索引就丢掉下层 registry claims；非注册内容另按类型选胜者。reload 的标准资源索引同路径按资源覆盖替换，不任意合并数组。

### 21.4 分层顺序与资源读取

pack 根 rank 最低，后续按 manifest parcel 顺序；包间按冻结 order。文件目录用已有 ResourceCatalog 保留全部 provider，不压平。为每个有效 `(pack, root)` 按全局序号分配不同 int rank，不能每包都从 0 重起；否则同路径同 rank 会被平台拒绝。rank 分配验证容量/溢出，不由依赖 topo 改顺序。

已有 MappedPackResources 按 source/root 暴露独立层，ApprovedResourcePacks 按 rank 发现并生成 required/fixed-position pack；实际 MC ResourceManager 决定单读取和 stack。KuaYue 从一个逻辑车包管理其所有 root 层，验证玩家顺序与其他默认/模组资源相对位置及默认 UI 不绕过管理。

sourceId 可用稳定 pack ID；rootId 用 `main` 或明确的稳定 parcel 映射，不直接传 `example_railway:m1`（平台 ID 不允许冒号）。映射可用可逆编码并记录 parcelId，不能用 hashCode 或安装目录名。路径区分根相对 URI path 与映射 logicalPath。

不能把 `MountTableVfs` 的最长前缀匹配当优先级；所有包直接 mount 到同一路径也不构成叠加。`CompositeVfs(LAST_WINS)` 可用于单读取或本包共享文件查找，不作为完整 stack 唯一来源。

### 21.5 严格安全与限额

**原始输入先检查，后构造 VfsPath：**拒绝绝对路径、反斜线、控制字符、空段、`.`/`..`、非法资源路径和未声明 namespace。不调用宽容 scoped normalizer 替作者修复路径。

- 目录：拒绝根及后代符号链接/junction/不支持节点；不能仅跳过。受限读取时验证来源未逃逸，对捕获期间被替换的链接仍拒绝；不能只靠一次 pre-scan。
- ZIP：使用已有 ResourceSources.archive 的 central directory/link/重复路径/文件目录冲突/CRC 检查，另验证 pack 根布局和 namespaces。当前拒绝 ZIP64、加密、分卷；明确映射为 INVALID_ZIP/不支持诊断，不偷偷解压或回退 ZipVfs 宽容入口。ZIP64 不是原产品单独承诺的格式，但若真实车包样例需要它，应显式补上游兼容后再验收。
- 限额技术默认：JSON 8 MiB、普通单文件 256 MiB、单包 1 GiB、单包文件数 50000，JSON 深度 64；复用 InputPolicy 并按整个 pack 聚合，而不是每个 parcel/root 各领总量预算。派生模型/图片再验总字节和像素预算。
- 按实际读取/解压字节计数，包括 manifests；不要信任 ZIP header 或 stat。到上限立即停止，不先 `readAllBytes()` 再检查。
- ResourceSources.archive 已在构建捕获前检查 raw entries，不重写该机制；不要从 ZipVfs.walk 恢复错误证据，任意 ResourceSources.vfs wrapper 也不自动获得 raw archive/link 严格性。
- ResourceSources.directory 的前置 checkDirectory 只限制节点和链接，随后 DirectoryVfs 构造仍全量哈希，没有按实际字节受限。此为 L1b 的明确待补边界：补有界目录入口/上游 API 或捕获前安全限额，不将后续 capture 限额误认为覆盖构造成本。Windows junction 和替换竞态也需实机测试。
- 旧 scoped 的 `./` 兼容和 ZIP 跳过错误的 legacy 行为保留，不能为车包改变所有消费者。

### 21.6 内容快照与缓存（技术执行建议）

复用 ResourceSnapshot 捕获本次实际字节与 descriptor/revision，之后注册和资源都引用这份内容，不再打开原安装目录。通用实现已将 >1 MiB 内容写到私有临时 spool；无需为 v1 重新发明 blob 存储。`<game-dir>/kuayue/cache/packs/<cache-key>/` 是可选持久复用建议，不是必须另写一套捕获才能启动。

快照不承诺对外部作者进程的整目录修改提供事务隔离；若捕获中观察到文件消失、结构或内容不稳定，拒绝/重试后重新验证，而不是声称捕获了某个外部事务时刻。

- 快照具有只读 API 和私有写入边界；已有 file.open(lease) 可在 owner 退役后使用现有 lease，失效后禁止新读取。进程 owner 持有固定根快照，server/client 只持有自己的消费 lease，不暴露可写 backend。
- root/owner 层分别存储，保留覆盖 provenance；hash 仅是内部缓存键或诊断工具。
- ResourceSnapshot 私有暂存文件不使用作者文件名，归档中的合法逻辑名无需原样写到物理缓存目录。Windows 目录源的 portable 名称限制仍要验证，不能静默改名或把不同容器都声称无条件等价。
- 创建缓存可借助 VFS 流式写入，但跨后端事务不是必需。不得承诺掉电安全或世界文件原子性。
- 不监控原安装目录、不自动改栈；普通 reload 重新读取同一快照。

**冻结来源 adapter（计划新增）是本轮必须保留的接入工作：** ApprovedResourcePacks 每次 discovery 会 `source.capture()` 并 retire 上次发现快照；coordinator 每轮也 capture。因此给这两者的 ResourceSource 必须来自已冻结字节，而非 ResourceSources.directory/archive 的 live 安装源。

adapter 不能简单多次返回同一个 ResourceSnapshot 对象，否则上轮 owner.close 后新读取会 STALE_REFERENCE。可在进程保留原根 snapshot 的前提下，每次使用 Builder.copy 生成独立消费快照，或补上游 lease-aware snapshot/view API；验证关闭、重复 capture 和峰值内存。新老消费代际读取相同启动字节，runtime generation 可变而包集合/顺序/内容不变。

## 22. 构建 SPI、严格模式与资源寿命

### 22.1 共享平台结构与计划注册结构

| 类型 | 必需信息 |
|---|---|
| ResourceSourceDescriptor（已有） | ownerId/sourceId/rootId/rank/origin；namespaces 与 pack/parcel 标签由注册 adapter 补充 |
| ResourceSnapshot.LocatedResourceFile（已有）/ LocatedIndex（计划） | 前者提供 URI、逻辑路径、descriptor、捕获字节/lease；后者增加入口 namespace 和索引域 |
| ResourceCatalog（已有） | source/root 全 provider、逻辑 stack、可见性后选胜者；不承担 registry claims 或业务定义目录 |
| ResourceRouting / ResourceCoordinator / ResourceGeneration（已有） | 平台路由、资源消费者代际和 scope，不等于宿主注册会话 |
| LocatedDefinition | payload、handler、来源、文件、JSON Pointer |
| DefinitionKey | 类型域与归一化 ID，非 registry claim |
| RegistryClaim | 最终 registry/domain、ID、定义来源，包括派生对象 |
| ValidationContext | 只读符号/资源/工厂查询、提交诊断，无 registry/world/bucket 写接口 |
| PreparedContent | 不可变目录、候选胜者、claims、diagnostics |
| StartupPackSnapshot | 有效 owner 集、固定 order、内容清单和读取句柄工厂 |

预检不能依赖 apply 写入的 metaStore。建立所有解析定义目录后校验，区分不存在和存在但无效。模型内部依赖至少实机 smoke，文件存在不是完整可渲染。

### 22.2 LEGACY 与 STRICT

旧 `buildForMod()` 的 namespace 默认、重复处理和宽容 diagnostics 保持兼容。新 provider 显式 STRICT：未知工厂/group、组环、属性形状、缺引用、未知 handler 和缺严格能力均阻断。

非注册 handler 可合法空 claims，但必须有稳定定义身份与预检；零定义/零 bogey 会话合法。models/motions 胜者先选后编译，不自动改为热 reload。代码预留 symbols/claims 与 corepack 内容区分。

### 22.3 稳定编排与错误策略

依赖 topo 用于准备/构建，同阶段顺序明确为 handler phase、handler 注册顺序、source 稳定顺序、文件、数组索引，group 额外按父依赖排序。不得用磁盘枚举或资源名字典序代替 pack rank。

外部 owner 预检失败及依赖者拒绝后，重建有效目录与胜者并重新校验到稳定集合，全过程不 apply。两个外部包 claims 冲突按基线拒双方；外部与 corepack/runtime 预留冲突拒外部。普通资源冲突是信息级。

最终会话一次 finalizer/attach/注册事件接入。致命异常穿透，不沿用 `applyUnchecked` 或 `RegTypeHandler` 的仅日志吞错路径。

### 22.4 宿主 provider 与双侧发现

计划新增按 mod 的显式构建 provider：其他 mod 无 provider 保持旧入口；KuaYue provider 接管 kuayue 来源，零包空根，不 fallback。首次 `KuaYue.REGISTRY.register()` 前安装，保证显式 happens-before。

客户端 `CLIENT_RESOURCES`、服务器 `SERVER_DATA` 使用同一快照规则，各实例自行持有其启动集合。默认资源包 UI 不得绕过开关或即时重排受管理车包；专用界面只写待生效 config。

MappedPackResources 已在缺失 pack.mcmeta 时按运行时 PackType 生成 metadata，不要求作者额外维护一套版本。普通 bridge 保持旧行为。MC finder 注册要在初次 discovery 前，宿主 provider 安装要在首次 registry dispatch 前；已有两个 API 不保证 KuaYue 的二者顺序，仍由 E0/L3 验证。

### 22.5 VFS/资源所有权与关闭

#87 已修复旧行为：`closeManagedResources(server)` 对非 null server 只清其列表，对 null 才清进程列表。`createProcessResourceScope`、`createClientResourceScope`、`createServerResourceScope` 和 GameShuttingDownEvent 的 close hook 均已有，不再计划修复“关闭世界误关进程资源”这一旧缺陷。

执行基线建议：

| 生命周期 | 持有者 | 关闭时机 |
|---|---|---|
| 启动固定根快照 | KuaYue bootstrap 进程 owner / registerManagedCloseable | 应用进程关闭，不在 world/server stop 释放 |
| 客户端资源 adapter/代际 | createClientResourceScope / client owner | 客户端消费退役/退出；最后 lease 在发布 executor 释放 |
| MinecraftServer 的资源 adapter/挂载 | server-specific owner | 该 server 停止，不能关闭共享快照 |
| 单个流/临时 ZIP handle | 打开者或明确 owning adapter | 读取结束/adapter 关闭；不复用已关闭 archive |
| SavedData 和世界引用 | 当前 server/world | world/server stop 清引用 |

使用已有 scope APIs 和 ResourceSnapshot/Generation lease。所有 scope ID 在 ResourceSystem 的同一 map 中唯一，包含宿主、逻辑端和 server 实例区分，不能为每个 server 重用同一个字符串。finder 的注册 handle 只解除注册、不关闭 source；宿主另管理固定根 owner。MappedPackResources 持有 file leases，MC managedView 借用 manager 不关闭它。

ApprovedResourcePacks 和 coordinator 是两条独立消费路径，不共享某个可随意 close 的 snapshot owner；必须由冻结 adapter/lease 协调。ResourceGeneration.disposal 需等所有 lease，publication executor 在最终释放前保持存活。多 server 同进程 finder 使用 serverSnapshots 列表的场景需验证旧 layers 的 lease，不从 scope API 名称推导已实测隔离。

统一 mount 名应区分 owner/scope；`replaceUnifiedMount` 会移除已有同路径挂载，旧 handle 安全不代表不同 server 在同路径可并存。`CompositeVfs.close()` 仅关闭订阅，不替调用方关闭 delegates，也要纳入所有权测试。

GameShuttingDownEvent 已调用 ResourceSystem.close，按 client/process/server 清理并关闭 unifiedVfs；旧“只有 cleanup 方法没有退出 hook”的描述不再成立。VFS 核心无客户端依赖仍不等于 ResourceSystem 已在本环境通过 dedicated-server 实测；异步脚本停止和 generation disposal 也不是任意同步清理保证。

## 23. Corepack 可禁用契约

### 23.1 四种环境

1. 默认启用：旧内容和 ID 一致。
2. 禁用、零外部包：runtime 可启动并创建空内容测试世界，无默认内容泄漏。
3. 禁用、自包含外部包：外部车型/bogey 可用，不隐式依赖默认内容。
4. 启用、覆盖包：默认注册保留，资源按 order 选胜者；corepack 可移动到最高层。

依赖 `mods.kuayue` 不等于依赖 pack `kuayue-core`。引用默认内容必须声明后者，被禁用时报 `DEPENDENCY_DISABLED`，不恢复开关。

### 23.2 内容归属

计划 `KuaYue/doc/packs/v1/content-ownership.json`：每项最终 ID/资源路径、类别、源码位置、owner、迁移动作。

| 内容 | owner | corepack 禁用后 |
|---|---|---|
| 模组入口、handlers、工厂、公式、模型 runtime、网络 | runtime | 保留 |
| 诊断/管理翻译、安全创造栏 | runtime | 保留，不引用默认车型图标 |
| 共享 bogey BE/proxy wiring | runtime | 有有效 bogey 时唯一创建；零 bogey 安全不创建 |
| 默认车型/轨道/bogey/餐食及资源 | corepack | 不注册、不挂载、不进入默认配方/tags |
| 外部新增内容 | 外部 owner | 按开关/依赖 |

### 23.3 已确认仍需修改的具体代码

- `KuaYueTrainPanelModule.TRAIN_PANEL_TAB` 图标仍使用 `I11Panel.DF11_DOOR`，需改 runtime/vanilla/Create 安全物品，保持 tab ID。
- `BogeyStyleTypeHandler.apply()` 仍调用 `BogeyRuntimeRegistration.ensure()`，迁到会话 finalizer。
- `BogeyRuntimeRegistration` 无 block 时抛错、validBlocks 从当前 context 收集、共享 BE 设置到静态 group；需覆盖零 bogey 和全来源聚合。
- 审计各 locomotive 静态 `.setParent()`、默认 ID 硬编码和 JAR 根资源；不能假设已有 JSON 迁移全部替代静态注册。

### 23.4 隔离验收

关闭 corepack 后，其来源不在有效视图；外部包同 namespace/同路径仍可见，不能误判为泄漏。runtime 翻译和通用模型能力保留。重启恢复后原 ID 与资源恢复；禁止重复注册或双份编译输出。

## 24. 诊断、缓存、世界与测试契约

### 24.1 机器报告

计划输出 `<game-dir>/kuayue/reports/packs/startup.json`：环境、格式、安装/请求/有效状态、声明版本、diagnostics。字段至少：

```text
code, severity, stage, ownerId, sourceId, parcelId,
logicalPath, jsonPointer, relatedIds, message
```

稳定排序，基础错误码：

```text
INVALID_MANIFEST / UNSUPPORTED_FORMAT / DUPLICATE_PACK_ID
INVALID_NAMESPACE / UNSAFE_PATH / LIMIT_EXCEEDED / INVALID_ZIP / DUPLICATE_ZIP_ENTRY
DEPENDENCY_MISSING / DEPENDENCY_DISABLED / VERSION_MISMATCH / DEPENDENCY_CYCLE
HANDLER_NOT_STRICT_READY / UNKNOWN_FACTORY / UNKNOWN_GROUP
INVALID_REFERENCE / UNDECLARED_DEPENDENCY / RESOURCE_OVERRIDE / REGISTRY_CONFLICT
BUILD_FAILED / FINALIZE_FAILED / CONTENT_MISMATCH / SAVE_VERSION_INCOMPATIBLE
```

`RESOURCE_OVERRIDE` 信息级，显示胜者与遮蔽来源；`CONTENT_MISMATCH` 只用于内部缓存损坏/捕获诊断，不表示同版本内容差异的兼容阻断。报告写失败不能吞原始致命错误。

### 24.2 Hash 仅内部用途

公开历史/管理身份为 `(id, version, format_version)`；不要求 content hash。同版本字节不同、order 不同，不能隐藏拒绝。

ResourceSnapshot descriptor.revision 已按捕获原始字节 SHA-256 生成；可用于缓存/捕获一致性，不加入版本兼容判定。包缓存键如需要，还要包含层/逻辑路径及适配器版本，不把单文件 revision 当完整包键。VFS stat hash 可为空，元数据仍不是内容不变证明。

没有工程用途就不必计算公开指纹；VFS 全内容 polling 的存在不改变兼容政策。

### 24.3 自身历史版本升级

```json
{"version":"0.2.0","save_compatibility":{"from_versions":["0.1.0"]}}
```

或：

```json
{"version":"0.3.0","save_compatibility":{"minimum_version":"0.2.0"}}
```

两字段互斥、恰好一个；历史版本合法、小于当前版本；from_versions 非空无重复，minimum 接受 `[minimum,current)`。不声明不授权跨版本；同版本直接接受；降级不支持。

只比较同 pack ID；不支持重命名/拆合自动迁移，不计算传递升级闭包。作者声明不证明 BlockState/BE 实际可迁移，平台不生成迁移脚本。

### 24.4 世界 SavedData

计划 `PackHistorySavedData extends SavedData`，数据名 `kuayue_packs`，Minecraft 写到 `<world>/data/kuayue_packs.dat`。Overworld 一份，随世界复制/改名；远程客户端不另保存历史。

```java
// 计划示意，须在 E0 验证的安全时机使用。
var factory = new SavedData.Factory<PackHistorySavedData>(
        PackHistorySavedData::new, PackHistorySavedData::load);
var history = server.overworld().getDataStorage()
        .computeIfAbsent(factory, "kuayue_packs");
```

建议 payload：

```snbt
{schema_version:1, initialized:1b,
 packs:[{id:"kuayue-core",version:"2.0.0",format_version:1}]}
```

- 缺文件/new object 的 initialized=false 与合法 initialized=true、packs=[] 区分。
- load/save 按 Minecraft 1.21.1 NBT/HolderLookup API，压缩和存盘交给游戏，不手写世界 JSON 或 ZIP。
- 校验只读；世界获准使用且正常保存时更新并 setDirty，拒绝时不改历史。
- 损坏/未知版本不视为新世界，不覆盖；E0 验证 Factory 异常与真实不存在如何区分。
- server/world 持有对象，停止清引用。现有 Saved 包装的 ServerStartedEvent 不作为载入前保护证据。
- 不承诺 `.dat` 与全部 chunks 跨文件原子事务，VFS 文件事务不适用于此。
- 缺历史策略与基本缺包处理待确认；安全拒绝 hook 必须早于读取/保存缺失 registry ID 的世界数据。
- 兼容失败只提示调整实例配置并重启，不修改当前快照、下载文件或按世界另选包。

### 24.5 测试与运行

纯 JVM：parser/schema 一致、重复键、路径/ZIP安全与限额、三来源等价、层 rank、快照和句柄、claims、严格/legacy、配置、显式依赖、版本与 SavedData 往返。

FML/游戏：真实 registry 提交、零包和 corepack 四种环境；client smoke 验模型/纹理/动作/图标/顺序；dedicated server 验无 client 类泄漏、配方、生命周期和世界保护。

实际运行只用隔离测试目录/世界。保留 FML launcher，不改过滤掩盖失败；私有资产不作为必要 fixture。报告命令、退出码、数量、skip 与环境原因，详见本次记录第 28.3 节。

## 25. 可交付的任务（v2）

新注册 SPI 仍在现有 data-driven 的 source/pipeline/validation 包，内部引用已有 core.resource.source/view/routing/lifecycle；不再次创建通用文件目录/URI/MC stack。KuaYue manager 放 core 的 `cn.kycraft.kuayue.packs`，车型 handler 留 train-parts，core 不直接依赖 train-parts；无需新增 Gradle 模块。

| ID | 依赖 | 交付与范围 | 验收 | 当前状态 |
|---|---|---|---|---|
| E0 | 无 | 复核 #87 finder/scope 与 KuaYue 注册/world 时序、归属、JDK 基线 | 已有 hook 的真实接入证据，不能猜顺序 | 通用 finder/process 清理已实现；宿主验证未完成 |
| L1a | E0；原型可先做 | 薄 ContentSource/入口 SPI、复用平台 descriptor/file、pack/parcel rebasing、namespaces/依赖定位 | 三来源/旧 JAR golden、本包共享 roots、不丢注册原定义 | 通用来源/身份/目录已有；注册 adapter 未完成 |
| L1b | L1a；可先做 fixtures | 冻结 ResourceSource、整包预算、目录初扫限额/竞态补齐、可选持久缓存 | 多次 capture/discovery 不读安装目录；多 roots 不绕预算；关闭/峰值/Windows | 通用严格捕获/spool/lease 已有；车包策略未完成 |
| L2 | L1a | 定义目录、strict handler 能力、validate、claims、诊断 | apply 前查引用与跨 handler 冲突；legacy 不变 | 未完成 |
| L3 | L2 | 会话、phase、一次 finalizer、宿主 provider、fatal 传播 | 零包不 fallback、只 attach 一次、错误不被吞 | 未完成 |
| K1 | E0；fake source 可先做 | manifests、config、必需依赖、版本、root requests | 外部默认禁用、直接依赖、配置保留、无新增开关文件 | 未完成 |
| K2 | L2、L3、K1 | 业务强引用、runtime symbols、共享 BE finalizer、安全图标 | 独立 bogey+corepack 禁用可用；零 bogey 安全 | 未完成 |
| R1 | L1b、L3、K1、E0 | 冻结 roots 注册 ApprovedResourcePacks、宿主生成固定路由/rank、managedView/旧 scoped 迁移、scope ownership | 双侧真实栈、发现不重新捕获 live 源、默认 UI 不绕管理、切世界不混寿命 | 分层投影/finder/metadata/scope 已有；车包集成未完成 |
| NR1 | L2、L3、K1、R1 | 启动配置胜者、只读 reload 预检、owner/rank、业务 runtime 发布；复用模型输入 | model/motion/clip 按 rank，零 claims 合法，SCRIPT 保留 | 文件目录/模型 adapter 已有；语义编排未完成 |
| O1 | K1、R1 | KuaYue client 顺序界面、状态、待生效配置 | corepack 可排序，不即时改栈，server 不加载 GUI | 未完成 |
| K3 | K2、R1 | corepack 内嵌输出、旧静态注册迁移、runtime 分离 | ID golden、四环境、无泄漏/重复注册 | 未完成 |
| S1 | E0、K3、O1；未决策略确认 | SavedData、版本、dirty/关闭、环境元信息 | 历史复制/隔离/损坏不覆盖，无 hash/下载/换包 | 未完成 |
| V1 | 全部含 NR1 | 新车型、原生纯资源、仅 reload、依赖 fixtures、作者指南 | 矩阵与双侧 smoke，有发布依赖来源证据 | 未完成 |

推荐命令（JDK 21 与 Gradle 任务先核对；两仓库 composite build 不同时重建相同输出）：

```sh
# KasugaLibNext
./gradlew :modules:core:test :modules:data-driven:test :modules:scripting:test :modules:modelling:modelUnitTest --console=plain

# 上一组完成后，可补 #87 的 vanilla metadata 与真实脚本集成（不开放车包脚本）
./gradlew :modules:modelling:test --tests '*ModelResourceSourceTest' :external:slp:javet:test --tests '*ResourceScriptTicketIntegrationTest' --console=plain

# 随后在 KuaYue
./gradlew :modules:core:test :modules:train-parts:test --console=plain
./gradlew build

# 插件基础单测，不替代 runtime 验收
npm test
```

agent 结束报告：任务 ID/契约、文件和公开变化、实际测试/skip、剩余阻塞与下游输入。不得跨任务隐式扩展业务范围或发布版本。

## 26. 开发就绪检查

### 已有输入

协议示例和产品规则；#87 的来源捕获/URI/目录/路由/分层投影/MC finder/metadata/scope/lease/模型 adapter；已有 handler/reload 扩展、corepack 四环境、strict/legacy 边界和 fixtures 方向。新脚本功能属于非必需背景，不改变车包信任规则。

### E0 尚需消除

- JDK 21 和当前合并提交的 JVM/FML 基线。
- provider 安装早于首次 registry dispatch 的显式顺序，以及已实现 AddPackFindersEvent 与 KuaYue bootstrap/初次消费的 happens-before。
- dedicated-server 下 ResourceSystem 和新 adapter 的真实 classloading。
- 内嵌 corepack 与 resource compiler 输出及发布工件版本/来源。
- 验证已实现 GameShuttingDownEvent/process/server 清理与 snapshot/generation/MC leases，尤其 discovery 重复、内置服务器多次开关和 disposal executor 退出。
- 固定来源 adapter、按 pack 预算与目录初始全量哈希；MappedPackResources 在旧 scoped 逐包消费者中的可见性迁移。
- world 数据消费前的兼容检查 hook，SavedData 缺失/损坏区分。
- runtime/corepack 的完整 JSON、Java、资源归属清单。

可先做 E0、parser fixtures、L1a/L1b 纯原型；E0 未通过不接生产注册/世界保护。用户世界不用于测试。

## 27. 非注册型数据驱动与纯资源包

### 27.1 三种路线

| 路线 | 当前基础 | v1 补齐 |
|---|---|---|
| 原生资源 | Minecraft loader、ApprovedResourcePacks/MappedPackResources/managedView、通用捕获 | 冻结外部 roots、包策略/依赖/rank/注册同源；indexes 可空 |
| 启动期配置 | BogeyMotion/Model handlers，ResourceCatalog 和模型输入 adapter | `(type,id)` 按冻结 rank 先选后验后编译，业务 runtime 发布 |
| reload 自定义定义 | ReloadHandler、FSM/clip buckets、真实 MC stack、平台 generation | 只读预检、pack owner/rank、同 runtime ID 选择、固定启动输入；不用脚本 block 替代 |

bogey_sizes/styles 会创建 Create 注册期对象，不能因不是 block/item 就认定无副作用。新 schema 仍需 codec/handler、稳定 ID、owner、runtime storage 和消费者；JSON Schema 文档不会自动注册 handler。

### 27.2 最小原生资源包

```text
appearance/
├── pack.json
└── parcels/appearance/
    ├── parcel.json
    └── assets/kuayue/lang/zh_cn.json
```

```json
{"format_version":1,"id":"kuayue:appearance","name_key":"pack.example_appearance.name","indexes":[]}
```

pack manifest 仍必填、声明 namespace/parcels/dependencies。没有 Kasuga 索引也合法，不创建 claims；不能以零方块拒绝，也不按“视觉包”给兼容豁免。

### 27.3 最小 reload 包

```text
animation/
├── pack.json
└── parcels/animation/
    ├── parcel.json
    └── data/example_animation/
        ├── kasuga_lib/data_driven/animation.json
        └── animation_clips/preview.json
```

索引：

```json
{"on_reload":["animation_clips/preview.json"]}
```

内容示意（须 codec 验证，不声明已实测）：

```json
{
  "animation_clips": [{
    "id": "example_animation:preview",
    "duration_seconds": 1.0,
    "formula_transforms": [{
      "target": {"bone": "group"},
      "transform": {"rotate": {"y": "30"}}
    }]
  }]
}
```

只有消费者引用 clip 才有可见效果。animation_clips 默认 index-only，state_machines 有 glob；一个路径不可同时列入 register/reload 两域，未知 schema 不动态生成 handler。

### 27.4 当前 resolver 的边界

现有 reload 按资源路径/glob 与 index 顺序发现并用 last-wins，不能直接认为等于玩家 order。两种重复都测试：同文件路径与不同文件同 clip ID。

只读启动预检复用 decoder，不调用 clearBucket/register；原有 reload 先清桶再解码且隔离错误的行为不能作为车包预检。SCRIPT 记录优先且不被 RESOURCE 清理，corepack 默认可覆盖定义归 RESOURCE，不清空 runtime 代码来源。

NR1 验收包括原生语言目录/ZIP、真实 clip 消费、启动 model/motion 胜者、不新增 block/item、显式依赖、错误 owner/字段定位、SCRIPT 保留以及同版本内容宽松政策。

## 28. 更新后源码证据与本次验证

### 28.1 通用底座证据

以下路径相对工作目录，行号对应本版记录的提交；后续修改需更新。

| 事实 | 源码 |
|---|---|
| VFS 读写、枚举、stat/hash、事务、监听 | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/core/vfs/VirtualFileSystem.java:15–95` |
| VfsPath 去首尾 slash，拒控制/反斜线/冒号，物理 portable 独立 | 同 VFS 包 `VfsPath.java:41–149` |
| DirectoryVfs 构造完整哈希扫描；防逃逸而非全禁 symlink | 同包 `backend/DirectoryVfs.java:57–127,659–693` |
| ZipVfs 宽容跳过非法 entry，重复路径 Map.put，读取未加业务限额 | 同包 `backend/ZipVfs.java:66–102,134–144` |
| CompositeVfs 胜者读取与去重枚举，没有 stack API，close 仅订阅 | 同包 `backend/CompositeVfs.java:54–138,190–237` |
| Pack bridge 标准路径与 ResourceLocation 限制 | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/core/resource/pack/VfsPackResourcesBridge.java:83–143` |
| scoped adapter 支持目录/flat 枚举，错误时 listEntries 可空 | 同 resource/pack 包 `VfsScopedPackResourcesAdapter.java:28–66` |
| scoped manager 支持 managedView，但旧列表只适配 Composite/VfsBacked/Path/File | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/core/resource/ScopedResourceManager.java:47–73` |
| server/process 清理分离，process/client/server scopes 及游戏退出 hook 已有 | 同 resource 包 `ResourceSystem.java:59–65,108–133,219–258` |
| live preview 单独启用 repository 并 client mount，不是全车包自动发现 | `KasugaLibNext/modules/modelling/src/main/java/lib/kasuga/rendering/models/mc/component/LiveModelSync.java:50–123` |
| 严格目录/归档/VFS source、两次捕获、raw archive/CRC 检查；目录构造仍全量哈希 | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/core/resource/source/ResourceSources.java:21–120,122–208` |
| 实际字节/JSON/文件数预算、私有 spool、file/stream lease；policy 按一个 Builder | 同 source 包 `ResourceSnapshot.java:25–92,97–173`、`InputPolicy.java:4–10` |
| source descriptor 已含 owner/root/rank，未含 namespaces/parcel | 同 source 包 `ResourceSourceDescriptor.java:6–13` |
| URI/生成 codec 与 raw 严格路径 | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/core/resource/identity/ResourceUri.java:1–100`、`MinecraftNameCodec.java:1–32`、`ResourcePaths.java:9–35` |
| 全 provider/stack/可见性后胜者与相同 rank 拒绝 | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/core/resource/view/ResourceCatalog.java:12–74` |
| 路由模板、授权、最长结构匹配和无父回退 | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/core/resource/routing/ResourceRouting.java:48–137` |
| AddPackFinders 接入、每次 live capture、按 root/rank 分层发现 | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/core/resource/pack/ApprovedResourcePacks.java:24–71` |
| captured file leases、保留原生路径、DERIVED 保留域、自动 pack metadata | 同 resource/pack 包 `MappedPackResources.java:17–98` |
| 借用真实 MC manager 的 stack/捕获，不关闭 MC pack | 同 resource/pack 包 `McManagedResourceView.java:12–37` |
| coordinator 捕获/prepare/publish，失败清理不回滚外部副作用 | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/core/resource/lifecycle/ResourceCoordinator.java:29–92` |
| generation 退役/lease/发布 executor 释放 | 同 lifecycle 包 `ResourceGeneration.java:25–73` |
| 非 MC bbmodel+PNG 子资源、像素预算、保留原始文件，native assets/data 跳过 | `KasugaLibNext/modules/modelling/src/main/java/lib/kasuga/rendering/models/mc/source/resource/ModelResourceSource.java:26–70` |
| 统一 text/json/binary 来源，sibling 限同 source/root | 同 modelling source/resource 包 `ResourceModelInput.java:18–51`、`ResourceModelSources.java:9–25` |
| 无 proxy 也扫描 kasuga_generated 模型 | `KasugaLibNext/modules/modelling/src/main/java/lib/kasuga/rendering/models/mc/source/model/KasugaModelScanner.java:29–38` |
| opt-in 脚本块、契约、scope registry 和异步准备/关闭 | `KasugaLibNext/modules/scripting/src/main/java/lib/kasuga/scripting/resource/ManagedScriptEntryPoint.java:16–74`、`ScriptBlockRegistry.java:1–174` |
| 非阻塞 ScriptTicket、取消与 completion token；无 raw 引擎返回值契约 | `KasugaLibNext/modules/core/src/main/java/lib/kasuga/core/resource/script/ScriptTicket.java:10–49`、`ScriptContract.java:1–60` |
| 有界请求 ring / facade，sandbox 能力仍需验证 | `KasugaLibNext/modules/scripting/src/main/java/lib/kasuga/scripting/resource/ScriptSubmissionRing.java:1–75`、`ScriptSafetyCapabilities.java:4–7` |

### 28.2 仍需改造的构建证据

| 事实 | 源码 |
|---|---|
| 注册输入仍通过 IModFile，不使用 VFS；旧 root namespace 和 dedup | `KasugaLibNext/modules/data-driven/src/main/java/lib/kasuga/registration/data_driven/builder/JsonTreeBuilder.java:67–154,419–457,673–741,744–804` |
| lazy registry callback 固定 buildForMod，顶层异常捕获 | `KasugaLibNext/modules/data-driven/src/main/java/lib/kasuga/registration/data_driven/JsonTreeIntegration.java:20–33` |
| TypeHandler 只有 parse/apply/identity 等，未含新 strict/claims SPI | 同 data_driven 包 `TypeHandler.java:8–65` |
| data-driven 唯一新增为 opt-in 静态脚本引用 decoder，未接注册会话 | 同 data_driven 包 `resource/ScriptBlockReferenceDecoder.java:10–15` |
| BuildContext 单 modId + metaStore | 同包 `context/BuildContext.java:6–30` |
| RegTypeHandler 缺 factory WARN、group fallback、catch WARN | 同包 `handler/RegTypeHandler.java:63–111` |
| reload 清桶、资源发现及候选 last-wins | 同包 `reload/ReloadHandler.java:19–117`，`reload/ReloadOrchestrator.java:83–95,253,307–397,507–548` |
| FSM/clip handler 生产注册，SCRIPT/RESOURCE 分域 | `KasugaLibNext/modules/modelling/src/main/java/lib/kasuga/rendering/models/mc/dynamic/fsm/FsmReloadHandlerRegistrar.java:22–31`；`KasugaLibNext/modules/render-core/src/main/java/lib/kasuga/rendering/models/uml/dynamic/fsm/FsmAnimationClips.java:17–25,50–73,98–102` |
| KuaYue 启动直接 registry.register | `KuaYue/modules/core/src/main/java/cn/kycraft/kuayue/KuaYueApplication.java:22–28` |
| models/motions 只编译 metadata，model 引用在 apply 查 | `KuaYue/modules/train-parts/src/main/java/cn/kycraft/kuayue/parts/bogeys/data/BogeyModelTypeHandler.java:36–65`、`BogeyMotionTypeHandler.java:30–40` |
| 共享 BE 无 block 抛错，validBlocks 在当前 context 收集 | 同 bogeys/data 包 `BogeyRuntimeRegistration.java:23–51` |
| 创造栏仍依赖默认 DF11 door | `KuaYue/modules/train-parts/src/main/java/cn/kycraft/kuayue/parts/KuaYueTrainPanelModule.java:17` |
| Saved 包装绑定 ServerStarted、默认 Overworld | `KasugaLibNext/modules/inject/src/main/java/lib/kasuga/inject/auto_configure/Saved.java:27–36,51–62` |
| KuaYue 本地 composite build 引用基础库 | `KuaYue/settings.gradle:14–24` |

### 28.3 本次实际执行

| 操作 | 结果 | 证据边界 |
|---|---|---|
| Kasuga `git pull --ff-only` | 成功；36b4660 → 24068ae（#87），78 文件增量 | 本地代码更新成功，不代表二进制发布；其他仓库本轮未拉取 |
| Kasuga `./gradlew :modules:core:test :modules:data-driven:test :modules:scripting:test :modules:modelling:modelUnitTest --console=plain` | 退出码 1，wrapper 启动前找不到 java/JAVA_HOME | 没有编译、没有执行任何 JVM/FML 测试，不计测试失败数 |
| 上一轮 KuaYue Gradle / 插件 npm test | 历史：前者缺 Java、退出 1；后者 10 pass/0 fail/0 skip、退出 0 | 本轮未重跑，不描述为 #87 当前基线验证 |
| 本次 client/server/世界 smoke | 未执行 | 不启动或改写用户世界 |
| v2 文档与平台示例静态检查 | 8 个文档 JSON 块、4 个新增平台 JSON 文件可解析；0–28 节及显式源码路径/行号检查通过；原 plan_detailed.md 摘要不变 | 不代替 Java 编译、路由/资源语义或游戏测试；计划新增路径不要求当前存在 |

本轮 JAVA_HOME 未设、PATH 无 java，常见 JDK 目录未找到可用安装；未安装 JDK 或修改全局配置。当前命令在 wrapper 检查处即失败，不从既有 build/test-results 推断本轮测试通过。补测时顺序执行以免 composite build 重建相同输出。

上游 `KasugaLibNext/doc/reference/vfs.md:65` 的 #86 测试数字是历史报告，不适用于 #87 本轮测试计数。新 `doc/reference/resource-platform.md:119–127` 给出平台、vanilla metadata/Javet 集成和无世界 client smoke 命令，并说明模型集成场景不在纯 JVM modelUnitTest 中；本轮没有执行这些测试，不将测试源码/文档描述当作已通过记录。

#87 新测试证据包括 ResourceIdentity/Lifecycle/Pipeline/Safety/ScopeOwner/TypeDetectorTest、ModelResourceSourceTest、ScriptSubmissionRingTest、ResourceScriptTicketIntegrationTest 和 ResourcePlatformClientSmoke。前者覆盖真实 Pack/MultiPackResourceManager、严格归档、lease/代际与 scope；后者意图验证首次生成模型/纹理、替换和删除。它们是后续复用验收入口，不免除车包自己的启动冻结、依赖、claims、corepack 和真实车型矩阵。

### 28.4 本次交付与下一步

本轮只快进 KasugaLibNext 并原地调整本文件；保留 `plan_detailed.md` / `plan_vfs.md`，不修改三个项目生产代码。下一步是补齐 JDK 21/E0，L1a/L1b/R1 复用新平台并实现车包适配与剩余边界，L2/L3 注册管线继续作为主要开发内容。

### 28.5 与 plan_vfs 的逐项对照（源码状态，不是完成验收）

| 平台规划 | #87 实际进展 | 车包计划变化 |
|---|---|---|
| VFS-F1 来源/稳定读取 | ResourceSources、实际字节 capture、InputPolicy、spool、lease、严格 archive 已有；目录初扫预算/Windows/按 pack 聚合仍有边界 | L1a 不再写 backend，L1b 做冻结 adapter/聚合策略与补齐 |
| VFS-I1 身份/目录 | canonical URI、可逆 MC 名称、类型证据/tag/query、provider/stack 已有 | 共享 locator/descriptor；不改作者 registry/native ID，不新建第二目录 |
| VFS-D1 模板/路由 | schema、strict JSON、模板继承、最长结构匹配、多输出与 no-fallback 已有 | KuaYue 在宿主生成批准路由，不增加必填作者配置 |
| VFS-L1 生命周期 | EntryPoint、context、coordinator、generation、取消/abort/发布与 scope 已有 | 复用资源生命周期，不替代注册预检/provider/finalizer |
| VFS-C1 MC 管理视图 | managedView、真实 stack/来源捕获、借用 manager 已有；旧 scoped 列表未适配 MappedPackResources | R1 做实际消费者迁移，不同时重复逐包扫描 |
| VFS-C2 MC 投影 | AddPackFindersEvent、RepositorySource、按 root/rank MappedPackResources 和 metadata 已有 | R1 接入冻结来源；每次 discovery 的 capture 不能读安装目录 |
| VFS-M1/M2 模型/派生 | 通用 text/json/binary input；非 MC bbmodel+PNG 派生模型/纹理和 scanner 已有 | 减少模型通用工作；车型装配/原生模型/外部依赖仍验收 |
| VFS-R1 受管理重载 | coordinator current generation/lease 与提前派生投影已有；MC/GPU/回调副作用仍不全局回滚，所有 loader 未全面迁移 | 普通 reload 从固定启动字节重建；不改变包集合/顺序 |
| VFS-S0/S1 脚本引用 | 实际接口改为 package URI + block + side + contract；显式 ScriptBlocks.register，Ticket 有界异步调用 | 仅记录新平台能力，不在车包 schema 启用；普通 module.exports 不自动暴露 |
| VFS-S2 脚本访问 | ResourceAccess/data-only facade、权限、预算、可撤销内存工作区已有；Javet untrusted 能力 UNVERIFIED | 不可信脚本开放和物理可写工作区不能标完成，也不是 v1 车包前置 |
| VFS-V1 里程碑验收 | 新 fixtures/FML/Javet/client smoke 已加入，当前环境尚未执行 | 不按“代码文件齐全”宣布 A/B/C/D 或车包 V1 全部完成 |

另有实际接口差异：事件 `bootstrap` 取代规划示例 host_bootstrap；ResourceSource.capture 返回 ResourceSnapshot，而不是 files/open 原型；ResourceSourceDescriptor 未包含 namespaces；脚本块用显式 registry 与 Ticket，而不是任意 module/export 引用。调用以当前代码和公开文档为准，规划不能当可直接编译的 API。

### 28.6 当前明确需要补齐/验证的接入清单

1. **启动来源冻结**：提供重复 capture 的固定 ResourceSource，owner/lease 互不误关；注册、首次 discovery 和后续 reload 同字节。
2. **pack/parcel 适配**：一次整包捕获后划分获准 roots，保留 URI 根相对路径、逻辑路径、入口 namespace、真实 owner、全局 rank 和直接依赖权限。
3. **预算与入口安全**：按 pack 聚合而非按 root 放大额度，补目录初始扫描边界；Windows junction/替换竞态/ZIP64需求明确处理。
4. **消费者兼容**：MappedPackResources 的旧 scoped 可见性、原生/生成模型依赖、普通 MC stack 与 DefinitionKey 候选选择分别验收。
5. **注册管线**：L2/L3 无副作用预检、claims、finalizer、provider、fatal 传播没有被 #87 取代。
6. **车包业务**：K1/K2/O1/K3/S1/V1 保留，不能因通用平台支持路由/脚本/重载扩大产品范围。
