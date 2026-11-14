# KuaYue 车包使用与运行时说明

## 1. 基本概念

车包是 KuaYue 中可安装、启用和排序的车型内容单位。一个车包拥有稳定的 `id` 和 SemVer 版本，声明可用命名空间、宿主依赖、车包依赖以及一个或多个 parcel。

parcel 是包内车型组织单位，拥有自己的 ID、名称键、可选图标和 KasugaLib 索引列表。新车型和外部车包必须按单个车型划分 parcel，例如 `c22`、`cr200j`、`df11`、`ss8` 或 `c70`。同一车型的车厢、机车或变体可以进入同一 parcel，但不能按“所有车厢”“所有机车”建立大类 parcel。多个车型真正共用的定义放在名为 `shared` 的 parcel。

目录包、ZIP 包和内置 `kuayue-core` 最终都转换为同一组逻辑输入：

```text
一个 main 根
+ 每个声明 parcel 一个 parcel-<folder> 根
+ 每个根对应的固定资源快照
```

## 2. 安装路径

外部目录包：

```text
<game-dir>/kuayue/packs/<container>/
└── pack.json
```

外部 ZIP 包：

```text
<game-dir>/kuayue/packs/<name>.zip
└── pack.json
```

ZIP 的 `pack.json` 必须直接位于压缩包根，不能多套一层 `<name>/`。容器目录名或 ZIP 文件名只用于定位安装物，不决定车包 ID。`pack.json.id` 相同且版本可以不同的两个安装物仍属于同一身份，不能同时生效。

实例侧路径：

| 内容 | 路径 |
|---|---|
| 实例配置 | `<game-dir>/kuayue/config.json` |
| 外部车包 | `<game-dir>/kuayue/packs/` |
| 启动报告 | `<game-dir>/kuayue/reports/packs/startup.json` |
| 世界包集记录 | `<world>/data/kuayue_packs.dat` |

启动不会生成 `<game-dir>/kuayue/cache/packs/`。该路径不是资源来源、不是发布内容，也不是诊断依据；若旧版本留下它，bootstrap 会清理。

## 3. `config.json`

游戏内管理界面尚未实现，因此配置文件的 `packs` 子域是当前调试和部署入口。完整形状：

```json
{
  "packs": {
    "format_version": 1,
    "enabled": {
      "kuayue-core": true,
      "sample-train": false
    },
    "order": [
      "kuayue-core",
      "sample-train"
    ],
    "inventory": [
      "sample-train"
    ]
  }
}
```

字段含义：

| 字段 | 作用 |
|---|---|
| `format_version` | 当前必须为 `1` |
| `enabled` | 请求开启的包；外部包缺省关闭，`kuayue-core` 缺省开启 |
| `order` | 普通资源优先级，从低到高 |
| `inventory` | 启动扫描后重算的外部安装清单 |

外部包不会因为被扫描到就自动启用。启用时把包 ID 写入 `enabled` 并设为 `true`，同时建议显式列入 `order`。配置缺失或没有 `packs` 子域时使用默认值：内置 corepack 开启，外部包关闭。

启动会补全默认开关、把 corepack 和扫描到的外部包加入顺序，并重写规范化配置。`inventory` 始终由扫描结果重算，用户不需要维护。当前运行时解析器只严格校验 `packs` 子域，并保留其他顶层字段。独立的 `config.schema.json` 目前只允许顶层 `packs`，使用该 Schema 校验带有其他顶层域的完整实例配置前，应先按 Schema 拆分片段。

以下情况属于配置错误：文件不是普通文件、JSON 损坏、`packs` 不是对象、`packs` 内出现未知字段、格式版本不支持、包 ID 非法、启用值不是布尔值，或 `order`、`inventory` 不是字符串数组。配置错误会阻止启动，程序不静默恢复默认包集。

修改后必须完全退出游戏再启动：

```text
修改 kuayue/config.json → 退出进程 → 重新启动
```

## 4. `pack.json`

外部包示例：

```json
{
  "format_version": 1,
  "id": "sample-train",
  "version": "1.0.0",
  "namespaces": ["sample_train"],
  "name_key": "pack.sample_train.name",
  "description_key": "pack.sample_train.description",
  "dependencies": {
    "mods": {
      "minecraft": "1.21.1",
      "neoforge": "21.1.203",
      "kuayue": "2.0.0",
      "kasuga_lib": "1.0.0",
      "create": "6.0.6"
    },
    "packs": {}
  },
  "parcels": [
    "parcels/demo/parcel.json"
  ]
}
```

约束：

- `id` 使用 `[a-z][a-z0-9_-]*`，外部包不能使用保留 ID `kuayue-core`；
- `version` 必须是三段式 SemVer，例如 `1.2.3`；
- `namespaces` 至少一项，parcel ID 与图标命名空间必须属于声明集合；
- `dependencies.mods` 必须完整提供 `minecraft`、`neoforge`、`kuayue`、`kasuga_lib`、`create`；
- `dependencies.packs` 只声明直接、必需的包依赖；
- `parcels` 至少一项，每项严格为 `parcels/<folder>/parcel.json`；
- `save_compatibility` 可选，只能使用 `from_versions` 或 `minimum_version` 其中一种。

版本范围只支持 `*`、精确版本和 `[lower,upper)`。依赖缺失、依赖被禁用、版本不满足或形成环时，相关包及其依赖者被阻断，系统不会自动启用或下载依赖。

`pack.mcmeta` 可以随包提供；省略时资源适配层会使用宿主生成的 Minecraft pack metadata。`pack.json` 和 `parcel.json` 本身不会作为 Minecraft 内容暴露。

## 5. `parcel.json`

```json
{
  "format_version": 1,
  "id": "sample_train:demo",
  "name_key": "parcel.sample_train.demo.name",
  "description_key": "parcel.sample_train.demo.description",
  "icon": "sample_train:textures/pack_icon.png",
  "indexes": [
    "data/sample_train/kasuga_lib/data_driven/demo.json"
  ]
}
```

`id` 必须带 namespace，并使用声明的包 namespace。`indexes` 填合并视图中的逻辑资源路径，不带 `parcels/<folder>/` 前缀，且必须位于 `data/<namespace>/kasuga_lib/data_driven/`。物理文件对应：

```text
parcels/demo/data/sample_train/kasuga_lib/data_driven/demo.json
```

`indexes` 可以为空，适合只提供模型、贴图、语言或配方的 parcel。注册内容必须由索引的 `on_register` 显式列出；reload 内容由 `on_reload` 列出。parcel 根内存在但未被索引声明的数据驱动文件仍属于普通资源，不会成为该 parcel 的注册入口。

## 6. 目录与 ZIP 映射

推荐结构：

```text
sample-train/
├── pack.json
├── pack.mcmeta                  # 可选
└── parcels/
    ├── shared/
    │   ├── parcel.json
    │   └── data/
    ├── c22/
    │   ├── parcel.json
    │   ├── assets/
    │   │   └── sample_train/
    │   └── data/
    │       └── sample_train/
    │           ├── kasuga_lib/data_driven/c22.json
    │           └── sample_train/c22/
    └── bogey-sw220k/
        ├── parcel.json
        └── assets/sample_train/models/block/bogey/sw220k/
```

映射规则：

| 物理位置 | 逻辑资源路径或作用 |
|---|---|
| pack 根 `assets/...` | `main` 根中的 `assets/...` |
| pack 根 `data/...` | `main` 根中的 `data/...` |
| `parcels/c22/assets/...` | `parcel-c22` 根中的 `assets/...` |
| `parcels/c22/data/...` | `parcel-c22` 根中的 `data/...` |
| `parcel.json` | parcel 清单，不进入 Minecraft 普通资源 |

包根 rank 最低，parcel 根按 `pack.json.parcels` 声明顺序递增。不同包按冻结的 `packs.order` 分配连续全局 rank。若逻辑路径重复，高 rank 根胜出；`ApprovedResourcePacks` 仍按 source/root 保留独立层，不先压成一个目录。

目录包和 ZIP 包在这张映射表中完全一致。区别只在首次捕获时：目录使用只读目录来源，ZIP 使用严格归档来源。两者随后都经过同一个 `PackResourceSnapshotter` 和同一套注册、资源投影代码。

## 7. 一次捕获与固定来源

发现和清单校验完成后，每个有效包发起一次平台捕获，并只保留一个基线。`PackResourceSnapshotter` 在这个基线上完成：

1. 按包清单区分 `main` 与各 parcel 文件；
2. 只保留 `assets/**`、`data/**`、`pack.mcmeta`、`pack.png` 等可投影资源；
3. 检查 parcel 声明的每个索引确实存在；
4. 为每个根创建带 owner、source、root、rank 和 revision 的 snapshot；
5. 包装为 `FixedResourceSource`。

`FixedResourceSource.capture()` 每次返回独立消费快照，但字节仍来自启动时基线。因此注册 provider、注册 source 和资源发现拿到的内容一致，原目录或 ZIP 在启动后被修改也不会让不同阶段读到不同版本。

目录和归档 adapter 可能在内部做一致性复读，以便识别捕获期间的改动；这不代表启动会保留多份资源树。

注册 provider 从每个根创建 `SnapshotContentSource`。parcel 根只把 `parcel.json.indexes` 声明入口提供给注册管线，同时仍允许本包内部按精确来源路径读取被声明文件引用的内容。资源侧则把同一固定根交给 `ApprovedResourcePacks`。

普通资源投影不需要、也不会复制出第二份权威目录。`ApprovedResourcePacks` 在 Minecraft pack discovery 时反复 `capture()` 固定来源，并为每个 source/root 生成按 rank 排序的资源层；实际读取使用该次消费快照的字节和 lease。

## 8. 启用、排序与重启

启动时按以下顺序形成冻结集合：

1. 读取并补全 `config.json`；
2. 扫描目录包、ZIP 包和内置 corepack；
3. 校验宿主版本、包依赖和依赖环；
4. 移除失败包及其依赖者；
5. 按 `order` 从低到高生成有效集合；
6. 用户未列出的已安装包按稳定 ID 补到末尾；
7. 全部有效包各捕获一次并分配连续资源 rank。

普通资源的最高优先级提供者获胜；语言、tag 等 Minecraft 消费者仍按自身 stack 语义读取。注册定义不使用“高排名覆盖低排名”：相同最终 registry ID 的 claim 冲突会在 apply 前阻断相关包。

任何开关或顺序变更都要求重启。`/reload` 只重新读取已冻结集合中的 `on_reload` 文件，不改变包集合、注册表或资源栈。

## 9. 严格预检、claims 与整包拒绝

`KuaYueContentRegistrationProvider` 把 runtime 根和当前车包根放进同一个 `DataDrivenBuildPipeline`，并启用 `BuildOptions.STRICT`。准备阶段不调用 handler 的 apply，而是完成：

- 清单与来源身份检查；
- 索引及定义 JSON 解析；
- 重复定义、类型级胜者和依赖可见性检查；
- factory、字段、引用和业务约束校验；
- reload 文件解码预检；
- 根据每个定义收集最终 `RegistryClaim`；
- 与宿主保留 registry 项以及彼此之间的 claim 冲突检查。

出现阻塞诊断时，provider 把诊断映射到 owner，移除该整包以及所有依赖它的包，然后再次预检和检查 claims，直到集合稳定。一个 parcel 有错不会只关闭这个 parcel。

冲突策略：

- corepack 或 runtime 与外部包冲突时，只拒绝冲突的外部 owner；
- 多个外部 owner 冲突时拒绝全部相关 owner；
- corepack 或 runtime 自身失败时中止启动，因为这些属于 KuaYue 自身契约；
- 注册准备成功后只执行一次 apply 和一次 finalizer 序列，finalizer 失败会使本次构建失败。

外部安装物的清单、路径、ZIP 和依赖扫描错误会标记该安装物无效，并阻断请求它的依赖闭包；独立有效包继续。`config.json` 错误会中止启动。启动捕获阶段的 I/O 或一致性失败目前也会中止启动。不要把这些早期错误都与 provider 的整包隔离能力混为一谈。

## 10. 内置 corepack

`kuayue-core` 由 `train-parts` 独占提供，默认开启，源清单位于：

```text
modules/train-parts/src/main/resources/kuayue-packs/kuayue-core/
```

`corepack.gradle` 读取显式列出的车型、转向架和 `bogeys/data-driven`，生成：

```text
<train-parts JAR>/
└── kuayue-packs/kuayue-core/
    ├── pack.json
    ├── pack.mcmeta
    └── parcels/<vehicle-or-bogey>/
```

当前生成树有 21 个车型相关 parcel、9 个转向架 parcel 和 1 个 `shared` parcel。`cr200j` 的车厢与机车源进入同一个车型 parcel。`shared` 保存跨车型共享的转向架定义、运动、模型、注册组、货运注册组和 QJ 动画定义。

corepack 边界：

| 内容 | 归属 |
|---|---|
| KuaYue 车型内容 | 对应车型 parcel |
| 具体转向架内容 | 对应 `bogey-*` parcel |
| 跨车型共享转向架定义与动画 | `parcels/shared/` |
| `resources/common/models/block/bogey/**` | runtime 普通资源，不进 corepack |
| `tracks/`、`seat/`、`toilet/` | runtime 普通资源，不进 corepack |
| provisions 餐食资源 | provisions 模块普通资源，不进 corepack |

以下根路径必须是空/不存在：

```text
kuayue-packs/kuayue-core/data/kuayue/kasuga_lib/data_driven/
```

正确索引只在各 parcel 根内。C70 与 NX70 的块定义分别由 `c70`、`nx70` 索引加载，共用的 `freight_panels` 注册组放在 `shared` 索引；不得建立 `freight` 这类大类 parcel。

corepack 关闭后，其注册与资源根不会进入有效集合，但 runtime 轨道、座椅、厕所、餐食和基础设施仍保留。若 runtime 轨道引用 corepack 的默认转向架，严格预检会记录缺失引用；相关消费者按自身回退行为处理。

## 11. 启动报告

每次启动先写：

```text
<game-dir>/kuayue/reports/packs/startup.json
```

注册 provider 成功后，同一路径会被更新，加入注册阶段的有效集合和内容诊断。主要字段：

| 字段 | 含义 |
|---|---|
| `environment` | Minecraft、NeoForge、KuaYue、KasugaLib、Create 版本 |
| `installations[]` | 每个目录、ZIP 与内嵌包的扫描、请求、有效和合法状态 |
| `installations[].diagnostic_codes` | 该安装物关联的发现、依赖或注册错误码 |
| `effective_packs[]` | 当前最终生效包及其来源 |
| `diagnostics[]` | 阶段、owner、source、parcel、逻辑路径、JSON Pointer、相关 ID 和消息 |

报告中的 `requested_enabled` 是用户请求，`effective` 是最终结果。若 `startup.json` 与日志中的 `Frozen ...` 不一致，以注册阶段更新后的 `effective_packs` 和 `diagnostics` 为准。

## 12. 世界包集记录

服务器在世界加载早期检查：

```text
<world>/data/kuayue_packs.dat
```

记录内容是按包 ID 排序的 `(id, version, format_version)`，不保存启用标志、资源顺序或内容 hash。当前规则：

- 没有历史时，用当前最终注册集合初始化；
- 历史包当前缺失或被关闭时，拒绝进入世界；
- 世界记录的版本高于当前版本时拒绝降级；
- 世界版本更低且当前包声明兼容时允许升级；
- 新版包可以加入；
- 同版本内容变化、顺序变化和资源变化不由版本检查自动阻止。

多人服务器和客户端应使用一致的包配置与文件。KuaYue 不自动同步包，也不按同版本文件 hash 拒绝连接。

## 13. 安全限制

默认上限：

| 项目 | 上限 |
|---|---:|
| 单个 JSON | 8 MiB |
| 单个其他文件 | 256 MiB |
| 单个车包总量 | 1 GiB |
| 文件数量 | 50,000 |

目录包拒绝符号链接和非普通文件。ZIP 包拒绝符号链接、重复条目、非普通特殊条目、绝对路径、反斜杠、空段、`.`、`..`、文件/目录冲突、加密、分卷和当前不支持的 ZIP64。清单、索引和资源读取都受字节与数量上限约束。

这些检查只处理路径和资源消费安全边界，不代表车包内容已经过安全审计。只安装可信来源的车包。

## 14. 排错顺序

1. 确认目标包在 `kuayue/packs/`，且根目录直接有 `pack.json`；
2. 确认 `config.json` 已设为下次启动生效，包 ID 在 `enabled` 中为 `true`；
3. 检查 `startup.json` 的 `installations`、`effective_packs` 和 `diagnostics`；
4. 检查依赖是否安装、启用且版本满足；
5. 检查 parcel 路径、索引声明和索引指向的物理文件；
6. 用 `doc/schemas` 校验 JSON 形状；
7. 查看日志中的 `Frozen`、`Prepared`、`Applied` 行；
8. 若世界被拒绝，检查历史包、版本和 `save_compatibility`；
9. 若资源未刷新，确认进程至少发生过一次完整重启。

`Frozen` 表示启动集合与快照已冻结，`Prepared` 表示 STRICT 预检已生成定义与 claims，`Applied` 表示 apply/finalizer 已成功。包被 provider 拒绝时，应以报告中的 owner、logical path 和 JSON Pointer 定位原因。
