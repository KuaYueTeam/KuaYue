# KuaYue 车包系统 v1

KuaYue 车包把内置车辆内容、外部车型内容与 Minecraft 普通资源放进同一次启动选择。`pack` 是安装、启用、依赖和覆盖顺序的单位；`parcel` 是包内车型组织单位，同一包内的 parcel 不能单独开关。

当前实现支持目录包、ZIP 包、依赖校验、整包排序、严格注册预检、注册 claim 冲突检查、统一 finalizer、启动报告、世界包集记录和从冻结字节投影 Minecraft 资源。游戏内车包管理界面尚未实现，安装、启用和排序通过 `<game-dir>/kuayue/config.json` 完成，修改后必须完全重启。

## 文档入口

| 文档 | 用途 |
|---|---|
| [GUIDE.md](GUIDE.md) | 安装、配置、清单、目录映射、快照、资源栈、依赖、报告和世界记录 |
| [AUTHORING.md](AUTHORING.md) | 制作目录包或 ZIP 包，按车型划分 parcel，编写资源与数据驱动内容 |
| [SCHEMAS.md](SCHEMAS.md) | 车包清单、配置和 KuaYue 数据驱动 Schema 的选择与校验 |
| [DEBUGGING.md](DEBUGGING.md) | 使用 `config.json` 调试、检查日志与启动报告、验证 corepack 边界 |
| [IMPLEMENTATION.md](IMPLEMENTATION.md) | 启动生命周期、一次捕获、注册预检、claims、finalizer 和资源投影原理 |
| [数据驱动 Schema](../../schemas/data-driven/v1/README.md) | KuaYue 内容字段、Java handler 与严格校验边界 |

## 关键路径

```text
<game-dir>/
├── kuayue/
│   ├── config.json
│   ├── packs/
│   │   ├── example/
│   │   │   └── pack.json
│   │   └── example.zip
│   └── reports/packs/startup.json
└── world/
    └── data/kuayue_packs.dat
```

目录包的容器目录和 ZIP 包的扩展名只用于发现，车包身份来自 `pack.json.id`。ZIP 根必须直接包含 `pack.json`。目录与 ZIP 使用相同的清单解析、逻辑路径映射、`PackResourceSnapshotter`、固定资源来源和注册 provider。

启动不会生成或读取 `<game-dir>/kuayue/cache/packs/` 作为权威资源树；当前实现直接从进程持有的冻结快照投影资源。若旧版本留下该目录，启动会清理它。

## 内置 corepack

内置包 ID 为 `kuayue-core`，只由 `train-parts` 提供，默认启用。清单位于：

```text
modules/train-parts/src/main/resources/kuayue-packs/kuayue-core/
├── pack.json
├── pack.mcmeta
└── parcels/
    ├── shared/
    ├── c22/
    ├── cr200j/
    ├── bogey-cw2/
    └── ...
```

`modules/train-parts/corepack.gradle` 从显式列出的车型和转向架源目录生成这份真实车包树，再由 `train-parts` 的 `processResources` 写入 JAR 的 `kuayue-packs/kuayue-core/`。`core` 和 `provisions` 模块不携带 corepack。

corepack 的范围是 KuaYue 车型、对应转向架和跨车型共享的车型/转向架定义。轨道、座椅、厕所、餐食、调度、联锁、Java runtime 和通用资源不属于 corepack。`resources/common/models/block/bogey/**` 必须排除；只有从具体车型或转向架源目录明确选取的同名模型才属于对应车型或转向架 parcel。

现有 corepack 树包含 21 个车型相关 parcel、9 个转向架 parcel 和 1 个 `shared` parcel。车型内容按车型组织，例如 `c22`、`c70`、`cr200j`、`df11`、`nx70` 和 `ss8`；同一车型的车厢、机车或变体可进入同一 parcel。`shared` 只放跨车型复用的定义和资源。

corepack 根不得出现 `data/kuayue/kasuga_lib/data_driven/`。索引只能位于具体 parcel 的逻辑根下，例如：

```text
parcels/c22/data/kuayue/kasuga_lib/data_driven/c22.json
parcels/shared/data/kuayue/kasuga_lib/data_driven/bogeys.json
```

## 启动链路

```text
读取 kuayue/config.json
  → 发现目录包、ZIP 包和内置 kuayue-core
  → 校验清单、parcel、依赖、宿主版本和包内路径
  → 每个有效包发起一次平台捕获并只保留一个基线
  → 切分为 main 与 parcel-<folder> 固定资源根
  → runtime 与车包根进入同一 STRICT 注册预检
  → 检查定义、引用、reload 解码和 registry claims
  → 以 owner 为单位整包拒绝，并阻断依赖闭包
  → 对稳定集合执行一次 apply/finalizer
  → 使用同一冻结根通过 ApprovedResourcePacks 投影 Minecraft 资源
  → 写出 startup.json 并检查世界包集
```

`FixedResourceSource` 使注册读取、资源发现和后续消费者重复 `capture()` 时仍得到启动时捕获的同一批字节，但每次返回独立的消费快照。`main` 和每个 parcel 根分别保留 rank 与 provenance，因此不会先压平为一个无来源目录。

“一次捕获”指 `PackResourceSnapshotter` 只取得并保留一个基线。目录或 ZIP adapter 为防止捕获期间源变化，可能在内部做一致性复读；这些复读不会产生第二份权威资源树。

注册冲突与普通资源覆盖采用不同规则：

- 一个方块、物品、轨道或转向架等最终 registry ID 被多个 owner 声明时，严格预检拒绝相关整包；
- 同一普通资源路径允许多层提供者存在，由包顺序和包内根顺序决定高优先级胜者；
- 资源覆盖不能隐藏注册冲突，注册排名也不能授权替换已注册对象。

## 主要限制

- `packs.enabled`、`packs.order` 和包文件内容在启动时冻结；`/reload` 只刷新已选包中的 reload 内容。
- 不提供游戏内安装、开关或拖拽排序界面。
- 不执行热注册、运行中启停包、自动下载或多人自动分发。
- 目录与 ZIP 的逻辑输入等价；压缩格式仍需满足严格 ZIP 安全限制。
- `startup.json` 的“已请求”不等于“已生效”，最终状态与诊断以注册阶段更新后的报告为准。
