# KuaYue 车包调试

客户端设置页可以启用、禁用和调整已安装包的优先级，也可以打开外部包目录。设置保存到：

```text
<game-dir>/kuayue/config.json
```

包集合、顺序和注册内容在启动时冻结。保存设置不会热切换，必须完全退出并重启。

## 1. 关键路径

| 内容 | 路径 |
|---|---|
| 实例配置 | `<game-dir>/kuayue/config.json` |
| 外部目录包或 ZIP 包 | `<game-dir>/kuayue/packs/` |
| 启动报告 | `<game-dir>/kuayue/reports/packs/startup.json` |
| 世界包集记录 | `<world>/data/kuayue_packs.dat` |
| 内置预设包源码 | `modules/train-parts/resources/packs/` |
| 内置预设包生成物 | `modules/train-parts/build/generated/kuayue-preset-packs/` |

`<game-dir>/kuayue/cache/packs/` 不参与当前启动，也不保存权威资源快照。不要从这里读取资源或据此判断包是否加载；若旧版本留下它，bootstrap 会清理。

目录包的根目录和 ZIP 内容根都必须直接包含 `pack.json`。外部包默认关闭；新扫描到的包只进入 `inventory`。

## 2. 验证默认预设包

删除隔离目录中的 `config.json` 后启动，全部 10 个内置预设包默认启用：

```text
kuayue-preset-running-gear
kuayue-preset-df
kuayue-preset-hxd
kuayue-preset-ss
kuayue-preset-emu
kuayue-preset-meter-gauge
kuayue-preset-passenger-22
kuayue-preset-passenger-25
kuayue-preset-freight
kuayue-preset-industrial
```

日志应包含：

```text
Frozen 10 effective KuaYue packs: [...]
Prepared ... definitions across ... pack sources and ... registry claims
Applied KuaYue pack registration session
```

`running-gear` 的 `required` 为 `true`，由运行时自动加入最终集合，且不能配置在 `enabled/order` 中；其他预设包可以在管理页或 `config.json` 中关闭。

## 3. 只启用一个可选预设包

示例：只保留运行基础与东风，关闭其余可选预设：

```json
{
  "packs": {
    "format_version": 1,
    "enabled": {
      "kuayue-preset-df": true,
      "kuayue-preset-hxd": false,
      "kuayue-preset-ss": false,
      "kuayue-preset-emu": false,
      "kuayue-preset-meter-gauge": false,
      "kuayue-preset-passenger-22": false,
      "kuayue-preset-passenger-25": false,
      "kuayue-preset-freight": false,
      "kuayue-preset-industrial": false
    },
    "order": [
      "kuayue-preset-df"
    ],
    "inventory": []
  }
}
```

`order` 从低到高。`inventory` 是外部包扫描结果，不需要手工维护；内置预设包不写入该数组。

## 4. 调试外部包

`running-gear` 是 required，不能为了隔离而关闭。启动日志至少会包含它和外部包：

```text
Frozen 2 effective KuaYue packs: [kuayue-preset-running-gear, sample-train]
Prepared ... definitions across ... pack sources and ... registry claims
Applied KuaYue pack registration session
Found new data pack kasuga/sample-train-parcel-demo/.../parcel-demo, loading it automatically
```

`Prepared ... pack sources` 同时包含 runtime、`main` 根、每个 parcel 根和 running-gear 根。具体数量会随内容变化，不是协议常量。

使用仓库样例：

```sh
rm -rf /tmp/kuayue-pack-dev
mkdir -p /tmp/kuayue-pack-dev/kuayue/packs
cp -R examples/packs/sample-train /tmp/kuayue-pack-dev/kuayue/packs/
cat > /tmp/kuayue-pack-dev/kuayue/config.json <<'JSON'
{
  "packs": {
    "format_version": 1,
    "enabled": {
      "kuayue-preset-df": false,
      "kuayue-preset-hxd": false,
      "kuayue-preset-ss": false,
      "kuayue-preset-emu": false,
      "kuayue-preset-meter-gauge": false,
      "kuayue-preset-passenger-22": false,
      "kuayue-preset-passenger-25": false,
      "kuayue-preset-freight": false,
      "kuayue-preset-industrial": false,
      "sample-train": true
    },
    "order": [
      "sample-train"
    ],
    "inventory": [
      "sample-train"
    ]
  }
}
JSON
printf 'eula=true\n' > /tmp/kuayue-pack-dev/eula.txt
KUAYUE_RUN_DIR=/tmp/kuayue-pack-dev \
  ./gradlew :modules:train-parts:runServer --console=plain
```

仓库中的历史 `examples/kuayue/config*.json` 可能仍保留旧包 ID；调试时按上面的当前配置生成隔离实例配置。

## 5. 检查启动报告

```text
/tmp/kuayue-pack-dev/kuayue/reports/packs/startup.json
```

重点检查：

- `environment` 是否与当前宿主版本一致；
- `installations[]` 是否识别目标安装物；
- `requested_enabled`、`valid`、`effective` 是否符合预期；
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

provider 会移除被拒绝 owner 及其依赖者，再重新准备和检查 claims。一个 parcel 失败时整个包被移除。最终没有阻塞诊断后才执行一次 apply/finalizer，并输出 `Applied`。

required preset 或 runtime 自身失败会中止启动。外部安装物的发现错误会标记其无效并阻断依赖闭包；配置错误和启动捕获失败会中止启动。

## 8. 检查预设包生成结果

重新生成：

```sh
./gradlew :modules:train-parts:generateKuaYuePresetPacks \
  :modules:train-parts:generateKuaYueRuntimeResources --no-daemon
```

列出预设包：

```sh
find modules/train-parts/build/generated/kuayue-preset-packs \
  -mindepth 1 -maxdepth 1 -type d \
  -exec basename {} \; | sort
```

检查每个包的 parcel 与清单一致：

```sh
for manifest in modules/train-parts/build/generated/kuayue-preset-packs/*/pack.json; do
  pack_dir=${manifest%/pack.json}
  printf '%s\n' "$pack_dir"
  jq -r '.parcels[]' "$manifest" | sort
  find "$pack_dir/parcels" -mindepth 2 -maxdepth 2 -name parcel.json \
    -print | sed "s#^$pack_dir/##" | sort
done
```

检查 running-gear 是唯一 required：

```sh
jq -r 'select(.required == true) | .id' \
  modules/train-parts/resources/packs/*/pack.json
```

预期只输出：

```text
kuayue-preset-running-gear
```

检查所有非必选预设都依赖 running-gear：

```sh
jq -r '
  select(.required != true)
  | [.id, (.dependencies.packs["kuayue-preset-running-gear"] // "MISSING")]
  | @tsv
' modules/train-parts/resources/packs/*/pack.json
```

每行第二列都应为 `*`。

检查 runtime 资源仍在普通 JAR 根：

```sh
find modules/train-parts/build/generated/kuayue-runtime-resources \
  -path '*/data/kuayue/kasuga_lib/data_driven/tracks.json'
```

构建边界验证：

- `train-parts` JAR 中存在全部 `kuayue-packs/<pack-id>/pack.json`；
- `core`、`provisions` JAR 中不存在 `kuayue-packs/`；
- 独立 `tracks/`、`seat/`、`toilet/` 和 `common` 仍来自 runtime 编译结果；
- 车型专用转向架位于车型 parcel；
- 无特定车型归属的通用转向架位于 running-gear。

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

ZIP 根必须直接出现 `pack.json`。完整重启后，目录与 ZIP 的逻辑根、定义、claims、注册 ID 和资源行为应等价。

```sh
unzip -l /tmp/kuayue-pack-dev/kuayue/packs/sample-train.zip | head
```

若 ZIP 中出现外层 `sample-train/`，位置不符合协议。若同一 ID 的目录和 ZIP 同时存在，应先移除一个。

## 10. 常见失败

| 现象 | 检查 |
|---|---|
| 包未出现在扫描结果 | `kuayue/packs/` 路径、根 `pack.json`、ZIP 外层目录 |
| 已安装但未加载 | `enabled` 是否为 `true`，依赖是否也启用 |
| 预设包列表仍显示旧 ID | 配置或旧报告中的 stale ID；重新启动并检查新报告 |
| 依赖失败 | `dependencies.packs`、版本范围、环、依赖安装状态 |
| `UNSAFE_PATH` / ZIP 错误 | 符号链接、重复条目、绝对路径、`..`、ZIP64 或加密 |
| `CLAIM_CONFLICT` | 同一最终 registry ID 的 owner、宿主保留 ID |
| `MISSING_REFERENCE` | 本包、直接依赖或 runtime 中是否存在被引用定义 |
| 转向架 style 贡献失败 | style 所有者依赖、重复 size/block、block `default_style` 是否一致 |
| 资源被另一包覆盖 | `packs.order` 和 package 内 parcel 顺序 |
| `/reload` 后仍无变化 | reload 是否声明；注册、包集合和资源顺序需要完整重启 |
| 世界拒绝进入 | 历史包关闭、缺失、降级或缺少 `save_compatibility` |

普通资源同名路径按冻结 rank 覆盖；registry ID 冲突不会通过资源覆盖解决。`/reload` 不改变包开关、依赖、注册表或 `ApprovedResourcePacks` 的启动层集合。
