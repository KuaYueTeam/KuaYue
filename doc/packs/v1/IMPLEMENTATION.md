# KuaYue 车包系统 v1 实现与原理

更新日期：2026-11-19

## 1. 实现范围

v1 实现目录包、ZIP 包、10 个内置预设包、实例配置、依赖与排序、启动时一次捕获、严格注册预检、registry claims、统一 finalizer、普通资源投影、启动报告和世界包集记录。

客户端设置页提供已安装预设包的启用、禁用和顺序编辑，保存后写入：

```text
<game-dir>/kuayue/config.json
```

修改在下次完整启动时生效。当前不包含 Blockbench 导出、运行时启停包、热注册、自动下载、多人自动分发和通用脚本包。

## 2. 启动生命周期

```text
PackDiscovery 扫描 kuayue/packs/ 和内置 kuayue-preset-* 预设包
  → 读取并补全 config.json
  → 解析清单、parcel、namespace 与依赖
  → 检查宿主版本、pack 版本和依赖环
  → 冻结请求集合与顺序
  → PackResourceSnapshotter 对每个有效包建立一次平台捕获基线
  → 生成 main 与 parcel-<folder> 固定根
  → KuaYueContentRegistrationProvider 执行 STRICT prepare
  → 检查定义、引用、reload 解码和 RegistryClaim
  → 以 owner 为单位整包拒绝并阻断依赖闭包
  → 重新 prepare，直到集合稳定
  → pipeline.build 执行一次 apply 和 finalizer 序列
  → ApprovedResourcePacks 投影冻结根
  → 更新 startup.json
  → 服务器加载前检查世界包集
```

注册选择一旦成功应用，在本次进程中不能通过 `/reload` 改变。`/reload` 只读取冻结集合中的 reload 文件。

## 3. 发现、配置与排序

`PackDiscovery` 只扫描 `<game-dir>/kuayue/packs/` 的直接子项：

- 普通目录对应一个目录包；
- 扩展名大小写不敏感的 `.zip` 普通文件对应一个 ZIP 包；
- 不递归嵌套安装；
- 目录和 ZIP 根都必须直接有 `pack.json`。

容器名只用于物理定位。`PackManifest.id` 是安装和依赖身份。同一 ID 的重复安装不能被资源顺序消解。

`config.json` 的 `packs` 子域包括：

| 字段 | 行为 |
|---|---|
| `enabled` | 可调整包请求开关；内置预设包缺省开启，外部包缺省关闭 |
| `order` | 可调整包的资源 rank 与有效包顺序，低到高 |
| `inventory` | 启动时从外部扫描结果重算，不包含内置预设包 |

缺失配置使用默认值。已有配置损坏、版本不支持或 `packs` 字段非法时，bootstrap 写出报告后中止启动，不覆盖损坏输入。合法配置经补全后原子重写；运行时解析器保留 `packs` 之外的顶层字段。

依赖解析只使用当前请求集合和已安装目录：

1. 检查每个启用包的五个宿主依赖；
2. 检查必需 pack 依赖的安装、启用和版本；
3. 找出依赖环；
4. 把失败包及其所有依赖者加入阻断集合；
5. 自动加入 required 包并固定为最低优先级；
6. 按 `order` 生成其余有效包的升序集合；
7. 未列入 `order` 的可选已安装包按稳定 ID 补入。

依赖关系基于 pack ID，不基于 namespace。`mods.kuayue` 存在不等于依赖任何预设包。`kuayue-preset-running-gear` 标记 `required: true`，运行时和设置页都会锁定它；配置补全会剔除它的 `enabled/order` 条目；其余预设包依赖它。

## 4. 目录包与 ZIP 包

目录与 ZIP 的差别被限制在原始读取层：

| 来源 | 捕获适配 |
|---|---|
| 目录 | `ResourceSources.mod()`；本地目录使用只读目录检查，mod union filesystem 使用只读 NIO 遍历 |
| ZIP | `ResourceSources.archive()`；先复制到私有临时文件，再检查 central directory、重复项、链接、CRC 和大小 |

捕获后两者进入同一个 `PackResourceSnapshotter.capture()`。包根和 parcel 的物理文件在扫描阶段已经映射为同一组 `PackFileEntry`，所以逻辑索引、`main`、`parcel-*`、rank、注册来源和资源投影协议一致。

因此“目录与 ZIP 等价”指同一逻辑内容得到相同的注册和资源行为，不表示所有 ZIP 特性都被接受。当前严格归档入口拒绝加密、分卷、链接、特殊文件、重复条目、文件/目录冲突和 ZIP64；超过 JSON、单文件、整包字节或文件数限制的输入也会被拒绝。

## 5. `PackResourceSnapshotter` 一次捕获

`PackResourceSnapshotter` 是整个车包资源冻结的核心桥接层。

### 5.1 捕获一次

对每个有效包，它根据 `sourceKind` 创建一个目录或归档 live source，然后调用一次平台捕获：

```java
ResourceSnapshot source = live.capture();
```

捕获成功后立即关闭 live source。后续所有车包处理和普通资源发现不再打开原安装路径。

这里的“一次捕获”指只保留一个基线快照。`ReadOnlyNioSource` 和 archive source 可能为检测捕获期间的变化而内部复读两次；这属于一致性实现，不会生成多份权威资源树。

捕获使用与 `PackLimits` 一致的实际字节策略：

```text
JSON 单文件       8 MiB
普通单文件       256 MiB
整包总计          1 GiB
文件数量          50,000
JSON 深度         64
内存/暂存阈值     1 MiB
```

超过阈值的资源由平台快照决定是否落在私有暂存区。该机制不是另建的车包持久缓存。

### 5.2 切分根

每个包至少产生一个 `main` 根；随后按 `pack.json.parcels` 声明顺序产生：

```text
parcel-<folder>
```

`rootId` 来自稳定的 parcel 文件夹名，不使用安装目录名或对象 hashCode。每个根保存：

- owner ID 与 source ID，均为稳定 pack ID；
- `rootId`；
- 全局唯一 rank；
- `EXTERNAL` origin；
- 声明 namespace；
- parcel ID（仅 parcel 根）；
- parcel 声明的索引列表。

rank 从 runtime 之后连续分配。`main` 低于该包所有 parcel；parcel 之间按 manifest 声明顺序递增。不同包不重置为同一个 rank。

### 5.3 只投影资源并验证索引

快照层只把以下内容放入根：

```text
assets/**
data/**
pack.mcmeta
pack.png
```

`pack.json`、`parcel.json` 和清单本身不进入 Minecraft 普通资源层。

对 `main` 根，平台可发现包根下的数据驱动索引。对 parcel 根，`parcel.json.indexes` 是注册入口白名单；声明索引只要缺失，本次包捕获立即失败。parcel 中的其他数据文件仍是普通资源，但未声明索引不会进入该 parcel 的注册入口集合。

### 5.4 转为固定来源

每个构建出的根包装为：

```java
ResourceSources.fixed(descriptor, snapshot, policy)
```

`FixedResourceSource` 持有启动基线。之后每次 `capture()` 都从基线复制文件描述并返回独立 `ResourceSnapshot`；关闭前一次消费快照不会使下一次捕获失效。注册与普通资源因此共享同一初始字节，而不是各自重新读取可能已经变化的安装目录。

## 6. 注册来源适配

注册 provider 对每个固定根调用 `capture()`，再通过：

```java
ContentSourceFactory.snapshot(...)
```

创建 `SnapshotContentSource`。它保留：

- `ResourceSourceDescriptor` 的 owner/source/root/rank/origin；
- namespace 和诊断标签；
- `LocatedFile` 的 URI、logical path、size、revision 和 tags；
- parcel 的声明索引集合。

注册读取按具体来源 URI 打开文件，不调用全局 `ResourceCatalog.find`。因此一个高层资源胜者不会替换低层注册候选，也不会让注册读取悄悄跨到另一个包。

runtime 根使用 owner `kuayue-runtime`、rank `0`，并通过同一固定来源与 STRICT 管线连接。当前 runtime 根从 KuaYue owning file 的 secure root 捕获；开发环境 union filesystem 可包含多模块资源，但生产环境需要继续验证 core、train-parts、provisions 显式 root 清单，不能把“当前 owning file 可读”当成永久资源归属契约。

## 7. STRICT 预检

`KuaYueContentRegistrationProvider` 创建 `DataDrivenBuildPipeline` 并调用：

```java
pipeline.prepare(sources, BuildOptions.STRICT, dependencies);
```

准备阶段不执行 `TypeHandler.apply`，也不创建注册对象。它完成：

1. 来源身份、rank 和重复 source/root 检查；
2. 读取每个入口索引并按生命周期分域；
3. 解析 JSON、识别 handler 和定义；
4. 建立候选与有效定义目录；
5. 检查直接依赖可见性和声明 namespace；
6. 执行 handler 的无副作用 `validate`；
7. 检查父定义、引用、group 环和 reload 解码；
8. 调用 handler 的 `claims` 收集最终 registry 占用；
9. 检查重复 claim、与宿主保留项的冲突和 handler 严格能力。

最终 `PreparedContent` 包含定义、全部候选、register/reload 文件、检测诊断、依赖视图和 `RegistryClaim` 列表。此时尚未向 Minecraft registry 写入任何对象。

## 8. claims 与整包拒绝

provider 不仅依赖原始诊断的 owner，还会重建 `registryKey → owners` 映射。拒绝规则是：

- 任意阻塞诊断归因到 owner 时，移除该 owner；
- 同一 registry key 有多个外部 owner 时，移除全部相关 owner；
- 必选预设包或 runtime 与外部 owner 冲突时，只移除外部 owner；
- 必选预设包或 runtime 自身出现阻塞失败时，中止本次启动；
- owner 被移除后，所有直接或间接依赖它的包一起移除。

失败集合确定后，provider 从活动根列表中删除整包，关闭本次消费 source，再重新 prepare 和检查 claims。循环直到没有可隔离的阻塞 owner。一个 parcel 错误不会只关闭该 parcel，也不会留下该包其余 parcel 的部分注册。

这种重试保证预检和 claim 结果对应同一个最终来源集合。它不承诺回滚早期已发生的文件系统副作用或 apply 后发生的副作用。

## 9. apply、finalizer 与提交

稳定集合没有阻塞诊断后，provider 调用一次：

```java
BuildResult result = pipeline.build(prepared);
```

`build` 按 handler phase、type、source rank、logical path 和声明顺序稳定排序，先执行所有定义 apply，再按注册顺序执行 host finalizer。finalizer 能看到最终 `PreparedContent`，用于构建跨来源共享结果。

规则：

- STRICT `PreparedContent` 有阻塞诊断时，`build` 直接返回 `REJECTED`；
- 某个 apply 失败时，本次构建返回 `FAILED`，不会继续执行 finalizer；
- finalizer 失败时，本次构建返回 `FAILED`；
- apply/finalizer 的失败不尝试部分回滚已发生的 registry 或 runtime 副作用，本次启动应失败；
- 成功时只报告 `APPLIED` 并返回一个 `JsonRegistryGroup` 交给 Kasuga 注册树。

因此“一次 finalizer”是每次最终稳定构建一次，不是每个 parcel 一次，也不是每轮被拒绝的预检一次。

新增 handler 必须把会创建最终 registry 或共享 runtime 对象的副作用完整映射到 claims/finalizer。当前 claims 列表只证明已声明项的冲突检查，不能替代对每个 handler 注册后果的代码审计。

## 10. 注册选择与报告

成功构建后，provider 先更新：

```text
PackRuntimeState.registeredPackIds()
```

再把注册诊断合并到原发现报告并重写：

```text
<game-dir>/kuayue/reports/packs/startup.json
```

报告先由发现阶段写出，因此配置或依赖阶段致命错误也有机会留下证据。注册成功后，同一报告的 `effective_packs`、`installations[].effective`、`valid` 和 `diagnostic_codes` 按最终集合更新。

报告的主要结构：

```text
format_version
environment
installations[]
effective_packs[]
diagnostics[]
```

它是启动状态诊断，不是资源 hash 清单，也不是可复用的资源缓存索引。

## 11. `ApprovedResourcePacks` 普通资源投影

KuaYue 不生成第二份权威资源目录，也不使用可变安装路径作为 Minecraft `PackResources`。资源侧从冻结根收集 `ResourceSource`：

1. 只选择当前 `registeredPackIds` 中的包；
2. 把每个 `main`、`parcel-*` 作为独立来源；
3. 为来源建立 `minecraft` entry point 的 `ResourceRouting`；
4. 调用 `ResourceSystem.getApprovedPacks().register(...)`；
5. 平台在 Minecraft pack discovery 中调用 `FixedResourceSource.capture()`；
6. `ApprovedResourcePacks` 按 source/root 生成资源层，而不是先合并成一个目录；
7. 每层由 `MappedPackResources` 用捕获文件和 lease 提供读取；
8. 各层按全局 rank 排序后加入 Minecraft 资源栈。

平台会为缺少 `pack.mcmeta` 的逻辑层生成宿主 metadata。提供 `pack.mcmeta` 时，其内容作为捕获资源参与投影。

普通资源同一路径保留完整 stack。单个读取取最高 rank；需要多层的消费者通过资源栈或查询读取。若两个可见来源意外具有同一 rank，平台按 `ROUTING_CONFLICT` 拒绝，而不是用文件名字典序猜测。

### 当前事件顺序边界

`KuaYuePackBootstrap.installResources()` 只接受已最终化的 `registeredPackIds()`，并注册到 `ApprovedResourcePacks`。注册 provider 在返回成功结果前更新最终集合；资源安装若早于该状态会直接失败。Kasuga `afterRunning` 在宿主上下文启动后执行安装，首次 Minecraft pack discovery 读取的因此是严格预检后的集合。

现阶段排错必须同时检查：

- 注册日志中的 `Prepared`、`Rejected`、`Applied`；
- `startup.json.effective_packs`；
- `Found new data pack kasuga/...` 中实际发现的 roots。

排错时仍要同时检查注册日志、报告和实际 pack discovery，但被拒绝的包不会仅因初始 `Frozen` 集合进入 Approved 资源栈。

## 12. 内置预设包构建

内置内容只在 `train-parts`：

```text
modules/train-parts/resources/packs/<pack-id>/
modules/train-parts/preset-packs.gradle
```

`generateKuaYuePresetPacks` 逐包执行：

1. 校验目录名等于 `pack.json.id`；
2. 要求根目录存在 `pack.json`、`pack.mcmeta`、`icon.png`；
3. 校验 `pack.json.parcels` 与实际 parcel 目录一致；
4. 把旧式 `blockstates`、`models`、`sounds`、`textures`、`recipes`、`loots`、`scripts` 和 data 文件映射为标准 `assets/`、`data/`；
5. 合并多 parcel 的语言与 tag；
6. 检查每个 parcel 声明的 data-driven index 实际存在；
7. 输出到 `build/generated/kuayue-preset-packs/<pack-id>/`。

`generateKuaYueRuntimeResources` 单独处理：

```text
resources/common/
resources/tracks/
resources/seat/
resources/toilet/
```

`processResources` 把 preset 输出放在：

```text
train-parts JAR/
└── kuayue-packs/<pack-id>/
```

并把 runtime 编译结果放在 JAR 普通资源根。`core`、scheduling、provisions 和其他模块不复制预设包目录。

### 12.1 当前包与 parcel

当前有 10 个包、32 个 parcel。车型包各自声明具体车型，`running-gear` 单独拥有：

- `shared`：共享 sizes、blocks、styles、variants、motions、models、groups 和动画；
- `bogey-*`：CW2、MKZ、PK209P、QJ、SW160、SW220K、TKZ1、TKZ2、ZK6 等没有独立整车车型包的通用转向架。

DF5、DF11G、DF21、HXD3D、SS3、SS8、JY290、DFH21 的专用转向架资源留在各自车型 parcel，并通过 `bogey_style_variants` 向 running-gear 的样式贡献 variant。C70/NX70 等没有专用转向架定义的车型直接使用 running-gear 或外部定义。

### 12.2 索引规则

注册入口位于具体 parcel：

```text
packs/<pack-id>/parcels/<parcel>/data/kuayue/kasuga_lib/data_driven/<entry>.json
```

例如：

```text
packs/kuayue-preset-ss/parcels/ss3/data/kuayue/kasuga_lib/data_driven/ss3.json
packs/kuayue-preset-running-gear/parcels/shared/data/kuayue/kasuga_lib/data_driven/bogeys.json
```

车型 parcel 的 `on_register` 同时引用车体数据与自己的 `bogeys/<vehicle>.json`。基础样式及其通用 variants 留在 running-gear，依赖方向始终是车型包指向必选包。

## 13. 无物化缓存

当前启动路径不生成持久资源树，也不把 `<game-dir>/kuayue/cache/packs/` 当作输入。启动只对旧目录执行清理，防止历史缓存继续误导检查。

“无物化 cache”不表示内存中没有任何副本：

- `ResourceSnapshot` 可把大文件放入平台私有临时暂存；
- `FixedResourceSource` 会为每次捕获产生独立消费快照；
- `MappedPackResources` 持有消费快照和 lease。

这些是平台生命周期对象，不是可供玩家、编辑器或发布流程复用的车包目录。

## 14. 失败与隔离

| 阶段 | 当前行为 |
|---|---|
| 实例配置非法 | 写报告后中止启动 |
| 外部包发现、清单、路径或 ZIP 安全失败 | 安装物标记非法；请求者及依赖闭包不进入有效集合，独立包继续 |
| 启动捕获失败 | 关闭已捕获 runtime/包根并中止启动 |
| 外部 STRICT 内容失败 | 拒绝整包及其依赖者，独立包继续准备 |
| 注册 claim 冲突 | 按外部全部拒绝或 protected owner 优先策略处理 |
| required preset/runtime 严格预检失败 | 中止启动 |
| apply/finalizer 失败 | 本次构建失败，无部分回滚承诺 |
| 世界包集不兼容 | 阻止加载该世界 |

provider 的整包拒绝是注册内容层面的明确保证；它不扩大为所有早期 I/O、捕获或宿主启动错误都可自动隔离。

## 15. 世界包集

服务器在 Overworld 数据目录检查：

```text
<world>/data/kuayue_packs.dat
```

历史 payload 记录：

```text
schema_version = 1
initialized = true
packs[] = { id, version, format_version }
```

记录不包含 enabled、order 或 content hash。缺失或损坏历史与已初始化的空历史分开处理；损坏文件不被静默覆盖。已记录包缺失、关闭、降级或不满足当前 `save_compatibility` 时拒绝进入世界。

同版本内容变化、资源变化和顺序变化不会由版本号自动识别。多人服务端和客户端的一致性由部署方保证。

## 16. 当前验证入口

资源与 schema 修改后至少执行：

```sh
./gradlew :modules:train-parts:generateKuaYuePresetPacks \
  :modules:train-parts:generateKuaYueRuntimeResources --no-daemon

./gradlew :modules:train-parts:test --no-daemon

./gradlew :modules:train-parts:jar --no-daemon
```

静态检查应确认：

- 10 个 `pack.json` 的目录名与 ID 一致；
- `pack.json.parcels` 与 `parcels/*/parcel.json` 完全一致；
- 32 个 parcel 的索引文件全部存在；
- 所有非必选预设包依赖 `kuayue-preset-running-gear`；
- required 包只有 running-gear；
- `bogey_style_variants` 的 style、size、block、model 和默认样式一致；
- 生成 JAR 的 `kuayue-packs/` 包含当前全部预设包。

具体定义数、claims 数和资源层数量随内容增长而变，日志中的数字不是兼容性常量。
