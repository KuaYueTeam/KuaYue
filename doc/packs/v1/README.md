# KuaYue 车包系统 v1

KuaYue 车包把内置车辆内容、外部车型内容与 Minecraft 普通资源放进同一次启动选择。`pack` 是安装、启用、依赖和覆盖顺序的单位；`parcel` 是包内车型组织单位，同一包内的 parcel 不能单独开关。

当前实现支持目录包、ZIP 包、10 个内置预设包、依赖校验、排序、严格注册预检、注册 claim 冲突检查、统一 finalizer、启动报告、世界包集记录、从冻结字节投影 Minecraft 资源，以及游戏内预设包管理页。管理页保存的是下次启动配置，修改后必须完全重启。

## 文档入口

| 文档 | 用途 |
|---|---|
| [GUIDE.md](GUIDE.md) | 安装、配置、清单、目录映射、快照、资源栈、依赖、报告和世界记录 |
| [AUTHORING.md](AUTHORING.md) | 制作目录包或 ZIP 包，按车型划分 parcel，编写资源与数据驱动内容 |
| [SCHEMAS.md](SCHEMAS.md) | 车包清单、配置和 KuaYue 数据驱动 Schema 的选择与校验 |
| [DEBUGGING.md](DEBUGGING.md) | 使用管理页或 `config.json` 调试、检查日志与启动报告、验证预设包布局 |
| [IMPLEMENTATION.md](IMPLEMENTATION.md) | 启动生命周期、一次捕获、注册预检、claims、finalizer 和资源投影原理 |
| [数据驱动 Schema](../../schemas/data-driven/v1/README.md) | KuaYue 内容字段、Java handler 与严格校验边界 |

## 关键路径

```text
<game-dir>/
├── kuayue/
│   ├── config.json
│   ├── packs/
│   │   ├── example/
│   │   │   ├── pack.json
│   │   │   └── icon.png           # 可选，车包图标
│   │   └── example.zip
│   └── reports/packs/startup.json
└── world/
    └── data/kuayue_packs.dat
```

目录包的容器目录和 ZIP 包的扩展名只用于发现，车包身份来自 `pack.json.id`。ZIP 根必须直接包含 `pack.json`。目录与 ZIP 使用相同的清单解析、逻辑路径映射、`PackResourceSnapshotter`、固定资源来源和注册 provider。

启动不会生成或读取 `<game-dir>/kuayue/cache/packs/` 作为权威资源树；当前实现直接从进程持有的冻结快照投影资源。若旧版本留下该目录，启动会清理它。

## 内置预设包

内置内容不再以单一 `kuayue-core` 提供，而是由 10 个独立预设包组成：

| Pack ID | 内容 |
|---|---|
| `kuayue-preset-running-gear` | 必选运行基础、通用转向架、Create 样式壳和共享运行数据 |
| `kuayue-preset-df` | DF5、DF11、DF11G、DF21 |
| `kuayue-preset-hxd` | HXD3D |
| `kuayue-preset-ss` | SS3、SS8 |
| `kuayue-preset-emu` | CR200J |
| `kuayue-preset-meter-gauge` | M1、JY30、DFH21 |
| `kuayue-preset-passenger-22` | 22 型客车 |
| `kuayue-preset-passenger-25` | 25、25B、25G、25K、25T、25Z |
| `kuayue-preset-freight` | C70、NX70 |
| `kuayue-preset-industrial` | JY290 |

除 running-gear 外，各预设包都声明：

```json
"dependencies": {
  "packs": {
    "kuayue-preset-running-gear": "*"
  }
}
```

缺失 `config.json` 时，9 个可选预设包默认启用；running-gear 因 `required: true` 由运行时自动加入，且不会写入 `config.json` 的 `enabled/order`。

编辑源位于：

```text
modules/train-parts/resources/packs/<pack-id>/
├── pack.json
├── icon.png
├── pack.mcmeta
├── assets/kuayue/lang/
└── parcels/<parcel>/
```

`modules/train-parts/preset-packs.gradle` 会逐包复制清单、parcel 和资源，并合并语言与 tag，生成：

```text
modules/train-parts/build/generated/kuayue-preset-packs/<pack-id>/
```

`processResources` 再把它写入 JAR 的 `kuayue-packs/<pack-id>/`。`common`、`tracks`、`seat`、`toilet` 仍由 `generateKuaYueRuntimeResources` 处理，不属于任一车型预设包；`core`、`provisions` 也不携带这些 preset 目录。

## 启动链路

```text
读取 kuayue/config.json
  → 发现目录包、ZIP 包和内置 kuayue-preset-* 包
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
- 游戏内管理页可以启用、禁用和调整下次启动顺序；安装目录可由“打开文件夹”按钮打开，但下载和压缩包复制仍需用户完成。
- 不执行热注册、运行中启停包、自动下载或多人自动分发。
- 目录与 ZIP 的逻辑输入等价；压缩格式仍需满足严格 ZIP 安全限制。
- `startup.json` 的“已请求”不等于“已生效”，最终状态与诊断以注册阶段更新后的报告为准。
