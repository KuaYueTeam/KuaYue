# KuaYue 车包调试

v1 没有游戏内车包管理界面。安装、开关和排序通过实例目录中的 `<game-dir>/kuayue/config.json` 完成；配置和包内容在启动时冻结，修改后必须完全退出并重启。

## 1. 关键路径

| 内容 | 路径 |
|---|---|
| 实例配置 | `<game-dir>/kuayue/config.json` |
| 外部目录包或 ZIP 包 | `<game-dir>/kuayue/packs/` |
| 启动报告 | `<game-dir>/kuayue/reports/packs/startup.json` |
| 世界包集记录 | `<world>/data/kuayue_packs.dat` |

`<game-dir>/kuayue/cache/packs/` 不参与当前启动，也不保存权威资源快照。不要从这里读取资源或据此判断包是否加载；若目录已存在，bootstrap 会清理它。

目录包的根目录和 ZIP 内容根都必须直接包含 `pack.json`。外部包默认关闭；新扫描到的包只进入 `inventory`。`kuayue-core` 默认开启，只从 `train-parts` JAR 的 `kuayue-packs/kuayue-core/` 读取。

## 2. 启用一个外部包

在 `packs.enabled` 中把包 ID 设为 `true`，并把它加入 `packs.order`。`order` 从低优先级到高优先级：

```json
{
  "packs": {
    "format_version": 1,
    "enabled": {
      "kuayue-core": false,
      "sample-train": true
    },
    "order": [
      "kuayue-core",
      "sample-train"
    ],
    "inventory": []
  }
}
```

调试独立外部包时通常关闭 corepack，减少 ID 和资源覆盖干扰。需要验证与默认车型共存时，把 `kuayue-core` 改为 `true` 并完整重启。

配置中未出现的外部包保持关闭。`inventory` 由程序按扫描结果重写，不需要手工维护。配置文件损坏时启动失败，不会自动改用默认集合。

## 3. 隔离启动

使用仓库中的最小目录包：

```sh
rm -rf /tmp/kuayue-pack-dev
mkdir -p /tmp/kuayue-pack-dev/kuayue/packs
cp -R examples/packs/sample-train /tmp/kuayue-pack-dev/kuayue/packs/
cp examples/kuayue/config.json /tmp/kuayue-pack-dev/kuayue/config.json
printf 'eula=true\n' > /tmp/kuayue-pack-dev/eula.txt
KUAYUE_RUN_DIR=/tmp/kuayue-pack-dev \
  ./gradlew :modules:train-parts:runServer --console=plain
```

当前代码在当前样例上的日志形状应为：

```text
Frozen 1 effective KuaYue packs; corepack=false
Prepared 8 definitions across 3 pack sources and 12 registry claims
Applied KuaYue pack registration session
Found new data pack kasuga/sample-train-parcel-demo/sample-train/parcel-demo, loading it automatically
Done (...)
```

`3 pack sources` 包含 runtime、`main` 和 `parcel-demo`。目录包只有一个 parcel，且 `main` 根没有 Minecraft 内容时，Minecraft pack discovery 只记录有资源的 `parcel-demo` 层。具体数量会随样例和 runtime 内容变化，不是版本契约。

使用集成样例：

```sh
rm -rf /tmp/kuayue-pack-integration
mkdir -p /tmp/kuayue-pack-integration/kuayue/packs
cp -R examples/packs/integration-train \
  /tmp/kuayue-pack-integration/kuayue/packs/
jq '.packs.enabled["kuayue-core"] = false' \
  examples/kuayue/config-integration-train.json \
  > /tmp/kuayue-pack-integration/kuayue/config.json
printf 'eula=true\n' > /tmp/kuayue-pack-integration/eula.txt
KUAYUE_RUN_DIR=/tmp/kuayue-pack-integration \
  ./gradlew :modules:train-parts:runServer --console=plain
```

当前预期同样包含 8 个定义、3 个来源和 12 个 claims，资源层名为 `integration-train/parcel-validation-car`。

## 4. 验证默认 corepack

删除隔离目录中的 `config.json`，或显式配置：

```json
{
  "packs": {
    "format_version": 1,
    "enabled": {
      "kuayue-core": true
    },
    "order": [
      "kuayue-core"
    ],
    "inventory": []
  }
}
```

完整重启后，当前实现的日志应包含：

```text
Frozen 1 effective KuaYue packs; corepack=true
Prepared 395 definitions across 33 pack sources and 525 registry claims
Applied KuaYue pack registration session
Found new data pack kasuga/kuayue-core-main/kuayue-core/main, loading it automatically
Found new data pack kasuga/kuayue-core-parcel-c22/kuayue-core/parcel-c22, loading it automatically
Found new data pack kasuga/kuayue-core-parcel-shared/kuayue-core/parcel-shared, loading it automatically
Done (...)
```

33 个来源是 runtime、corepack `main` 和 31 个 parcel。资源侧不会再次写出一份权威目录。数量和 IDs 是当前测试快照的证据，不应硬编码到兼容性判断。

## 5. 检查启动报告

报告路径：

```text
/tmp/kuayue-pack-dev/kuayue/reports/packs/startup.json
```

重点检查：

- `environment` 是否与当前宿主版本一致；
- `installations[]` 是否识别目标和 corepack；
- 目标的 `requested_enabled`、`valid`、`effective` 是否符合预期；
- `effective_packs[]` 是否等于最终注册集合；
- `diagnostics[]` 是否有阻塞错误；
- `diagnostics[].owner_id`、`source_id`、`stage`、`logical_path` 和 `json_pointer` 是否定位到具体来源。

报告先由发现阶段写出，注册 provider 成功后再更新。若报告与启动日志不一致，应以注册阶段更新后的 `effective_packs` 为准，并保留完整诊断。

## 6. 检查一次捕获与资源根

每个包在启动时发起一次平台捕获并只保留一个基线，然后由 `PackResourceSnapshotter` 拆成：

```text
main
parcel-<folder>
...
```

每个根拥有连续全局 rank 和独立 `FixedResourceSource`。注册 provider 使用同一固定根创建 `SnapshotContentSource`，资源侧把同一固定根交给 `ApprovedResourcePacks`。

底层目录或 ZIP adapter 可能为保证捕获一致性而复读输入，但不会生成第二份权威资源树。

日志检查点：

```text
Prepared ... across <N> pack sources ...
Found new data pack kasuga/<pack>-main/<pack>/main ...
Found new data pack kasuga/<pack>-parcel-<folder>/<pack>/parcel-<folder> ...
```

若 `main` 或某个 parcel 没有可投影的 `assets/`、`data/`、`pack.mcmeta`、`pack.png`，它仍可参与注册来源统计，但不一定产生可见的 Minecraft 资源层。

测试源文件冻结：

1. 启动一次并记录某个资源或定义结果；
2. 服务端保持运行，修改原安装目录中的文件；
3. 对已冻结集合执行 `/reload`；
4. 不应从修改后的原文件读取新字节。

需要验证新内容时完整重启。

## 7. 检查注册预检、claims 与整包拒绝

注册日志：

```text
Prepared ... definitions across ... pack sources and ... registry claims
Applied KuaYue pack registration session
```

`Prepared` 前的严格阶段已经完成定义解析、引用校验、reload 解码和 registry claim 冲突检查。出现外部内容错误时，日志可能先出现：

```text
Rejected KuaYue packs after strict preflight: [<pack-id>, ...]
```

随后 provider 会移除被拒绝 owner 及其依赖者，再重新准备和检查 claims。一个 parcel 失败时整个包被移除。最终没有阻塞诊断后才执行一次 apply/finalizer，并输出 `Applied`。

corepack 或 runtime 自身失败会中止启动。外部安装物的发现错误会标记其无效并阻断依赖闭包；配置错误和启动捕获失败会中止启动；不能根据 `Rejected KuaYue packs` 是否存在来推断所有静态校验错误都已隔离。

资源安装只接受已经完成严格预检的 `registeredPackIds()`；若注册选择尚未最终化，`installResources()` 会直接失败。联调拒绝场景时仍应同时确认目标包从 `effective_packs` 移出，并且其 `kasuga/...` 资源层没有参与 discovery。以 `startup.json`、`Prepared`/`Applied` 和 Minecraft pack discovery 三方日志共同判定，不能只凭配置中的 `enabled: true` 判断生效。

## 8. 检查 corepack 构建结果

重新生成：

```sh
./gradlew :modules:train-parts:processResources --rerun-tasks
```

检查 parcel 组成：

```sh
find modules/train-parts/build/generated/kuayue-corepack/parcels \
  -mindepth 1 -maxdepth 1 -type d \
  -exec basename {} \; | sort
```

当前应为 31 个目录：21 个车型相关 parcel、9 个转向架 parcel 和 `shared`。C70 与 NX70 分别拥有自己的 parcel 和索引，不得再建立 `freight` 这类大分类 parcel。

检查非法根索引：

```sh
test ! -e \
  modules/train-parts/build/generated/kuayue-corepack/data/kuayue/kasuga_lib/data_driven
```

检查 runtime 资源仍在普通 JAR 根：

```sh
find modules/train-parts/build/generated/kuayue-runtime-resources \
  -path '*/data/kuayue/kasuga_lib/data_driven/tracks.json'
```

corepack 边界验证：

- `train-parts` JAR 中存在 `kuayue-packs/kuayue-core/pack.json`；
- `core`、`provisions` JAR 中不存在 `kuayue-packs/kuayue-core/`；
- corepack 不包含 `resources/common/models/block/bogey/**`；
- corepack 不包含独立 `tracks/`、`seat/`、`toilet/` 或 provisions 资源；
- corepack 不含 `data/kuayue/kasuga_lib/data_driven/` 根索引；
- 索引只位于 `parcels/<id>/data/kuayue/kasuga_lib/data_driven/`。

## 9. 测试目录包与 ZIP 包等价

先用目录包启动并记录：

- `effective_packs`；
- `Prepared` 定义数和 claims 数；
- `kasuga/...` 资源层；
- 一个注册 ID 和一项普通资源。

然后在同一隔离目录中移除目录安装，压缩相同内容：

```sh
cd examples/packs/sample-train
zip -r /tmp/kuayue-pack-dev/kuayue/packs/sample-train.zip \
  pack.json parcels
```

ZIP 根必须直接出现 `pack.json`。完整重启后，目录与 ZIP 的逻辑根、定义、claims、注册 ID 和资源行为应等价。检查：

```sh
unzip -l /tmp/kuayue-pack-dev/kuayue/packs/sample-train.zip | head
```

若 ZIP 中出现外层 `sample-train/`，位置不符合协议。若同一 ID 的目录和 ZIP 同时存在，应先移除一个，避免把身份重复误判为顺序问题。

## 10. 常见失败

| 现象 | 检查 |
|---|---|
| 包未出现在扫描结果 | `kuayue/packs/` 路径、根 `pack.json`、ZIP 外层目录 |
| 已安装但未加载 | `enabled` 是否为 `true`，依赖是否也启用 |
| 依赖失败 | `dependencies.packs`、版本范围、环、依赖安装状态 |
| `UNSAFE_PATH` / ZIP 错误 | 符号链接、重复条目、绝对路径、`..`、ZIP64 或加密 |
| `CLAIM_CONFLICT` | 同一最终 registry ID 的 owner，宿主保留 ID |
| `MISSING_REFERENCE` | 本包、直接依赖或 runtime 中是否存在被引用定义 |
| 资源被另一包覆盖 | `packs.order` 和 package 内 parcel 顺序 |
| `/reload` 后仍无变化 | reload 是否声明，注册和包集合是否需要完整重启 |
| 世界拒绝进入 | 历史包关闭、缺失、降级或缺少 `save_compatibility` |
| 配置改动不生效 | 进程是否完全退出，而不只是返回标题界面 |

普通资源同名路径按冻结 rank 覆盖；registry ID 冲突不会通过资源覆盖解决。`/reload` 不改变包开关、依赖、注册表或 `ApprovedResourcePacks` 的启动层集合。
