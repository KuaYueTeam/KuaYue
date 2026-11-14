# 制作 KuaYue 车包

## 1. 确定包与车型边界

开始写文件前先确定：

- 稳定车包 ID，例如 `my-train-pack`；
- 三段式 SemVer，例如 `1.0.0`；
- 内容 namespace，例如 `mytrain`；
- 每个车型一个 parcel ID，例如 `mytrain:c22`；
- 是否依赖 `kuayue-core` 或其他直接依赖；
- 需要注册的方块、物品、轨道和转向架 ID；
- 目标 Minecraft、NeoForge、KuaYue、KasugaLib、Create 版本。

车包 ID 使用 `[a-z][a-z0-9_-]*`。Minecraft namespace 使用小写字母、数字、`_`、`.`、`-`。外部包不能使用保留 ID `kuayue-core`。

parcel 以单个车型为单位。车厢、机车和同一车型的变体可以放在同一个车型 parcel；多个车型共用且不属于某一车型的转向架定义、运动、模型和动画放入 `shared`。禁止创建 `parcels/carriages/`、`parcels/locomotives/` 之类的大类目录。

## 2. 建立目录

```text
my-train-pack/
├── pack.json
├── pack.mcmeta                  # 可选
└── parcels/
    ├── shared/
    │   ├── parcel.json
    │   └── data/mytrain/kasuga_lib/data_driven/shared.json
    ├── c22/
    │   ├── parcel.json
    │   ├── assets/
    │   │   └── mytrain/
    │   │       ├── blockstates/
    │   │       ├── lang/
    │   │       ├── models/
    │   │       └── textures/
    │   └── data/
    │       └── mytrain/
    │           ├── kasuga_lib/data_driven/c22.json
    │           └── c22/
    │               ├── groups.json
    │               └── blocks.json
    └── bogey-mytrain/
        ├── parcel.json
        └── assets/mytrain/models/block/bogey/mytrain/
```

只有确实被多个车型共用时才建立 `shared`。只服务单一车型的资源必须留在该车型 parcel。具体转向架按类型建立 parcel，例如 `bogey-sw220k`；不要把所有转向架塞进一个 `bogey` parcel。

仓库中的 [sample-train](../../../examples/packs/sample-train/) 是可直接加载的最小目录包。[integration-train](../../../examples/packs/integration-train/) 额外演示 blockstate、模型、语言、配方、loot table 和 tag。

## 3. 写 `pack.json`

```json
{
  "format_version": 1,
  "id": "my-train-pack",
  "version": "1.0.0",
  "namespaces": ["mytrain"],
  "name_key": "pack.mytrain.name",
  "description_key": "pack.mytrain.description",
  "dependencies": {
    "mods": {
      "minecraft": "1.21.1",
      "neoforge": "[21.1.0,21.2.0)",
      "kuayue": "[2.0.0,3.0.0)",
      "kasuga_lib": "[1.0.0,2.0.0)",
      "create": "[6.0.0,7.0.0)"
    },
    "packs": {
      "kuayue-core": "*"
    }
  },
  "parcels": [
    "parcels/shared/parcel.json",
    "parcels/c22/parcel.json",
    "parcels/bogey-mytrain/parcel.json"
  ]
}
```

规则：

- `dependencies.mods` 必须声明全部五个宿主依赖；
- `dependencies.packs` 只放直接且必需的依赖；
- 只有引用默认车型或默认转向架时才依赖 `kuayue-core`；
- `parcels` 顺序决定包内并级对同一路径的覆盖优先级，后声明者更高；
- 未列入 `parcels` 的 parcel 不会成为该包的注册根或资源根。

版本范围只接受 `*`、精确三段式版本和 `[lower,upper)`。不要使用 caret、tilde 或多个范围逗号组合。

## 4. 写 `parcel.json`

```json
{
  "format_version": 1,
  "id": "mytrain:c22",
  "name_key": "parcel.mytrain.c22.name",
  "description_key": "parcel.mytrain.c22.description",
  "icon": "mytrain:textures/pack_icon.png",
  "indexes": [
    "data/mytrain/kasuga_lib/data_driven/c22.json"
  ]
}
```

parcel ID 的 namespace 必须已在 `pack.json.namespaces` 中声明。`indexes` 使用合并后的逻辑路径，不带 `parcels/c22/` 前缀。对应文件是：

```text
parcels/c22/data/mytrain/kasuga_lib/data_driven/c22.json
```

`indexes` 可以为空，适合只提供模型、贴图、语言、配方或 tag 的 parcel。若要注册内容，索引文件必须由对应 parcel 显式声明；物理目录里未声明的数据驱动索引不会被当作注册入口。

## 5. 写索引和内容

索引示例：

```json
{
  "on_register": [
    "c22/groups.json",
    "c22/blocks.json"
  ],
  "on_reload": []
}
```

索引中的相对路径以该索引所在的 `data/<namespace>/` 为根，不再次添加 `parcels/c22/`。

注册组：

```json
{
  "registry_groups": [
    {
      "id": "mytrain:panels",
      "properties": {
        "no_occlusion": true,
        "strength": [1.5, 3.0],
        "map_color": "blue"
      },
      "item_properties": {
        "tab": "kuayue:train_panel_tab"
      }
    }
  ]
}
```

方块：

```json
{
  "blocks": [
    {
      "id": "mytrain:panel_lower",
      "type": "train_panel",
      "registry_group": "mytrain:panels"
    },
    {
      "id": "mytrain:panel_upper",
      "type": "slab_top",
      "registry_group": "mytrain:panels"
    }
  ]
}
```

当前可用 factory、`params`、引用规则和精确字段见 [数据驱动 Schema 索引](../../schemas/data-driven/v1/README.md)。不要从旧数据文件复制已经移除的字段。

注册型文件先经过无副作用预检。handler 的 `validate` 和 `claims` 阶段不能注册对象；实际方块、物品、轨道等由后续 apply 创建。多个定义产生同一个最终 registry ID 时，严格预检会拒绝整个声明 owner，而不是按 parcel 或 `order` 保留一个。

## 6. 准备普通资源

注册方块不会自动生成模型、方块状态、纹理和翻译。最小资源通常包括：

```text
assets/mytrain/blockstates/panel_lower.json
assets/mytrain/models/block/panel_lower.json
assets/mytrain/models/item/panel_lower.json
assets/mytrain/textures/block/panel_lower.png
assets/mytrain/lang/zh_cn.json
assets/mytrain/lang/en_us.json
```

同一路径可以出现在 pack 根、多个 parcel 和多个包中。启动会保留每个 source/root 层，并根据全局 rank 选择普通资源的最高优先级提供者。语言或 tag 等元素应满足 Minecraft 实际消费者的合并规则。

资源字节来自启动时冻结快照。启动后修改原目录的文件不会改变本次进程中的注册或资源结果；要测试修改，必须完整重启。

## 7. 用 `config.json` 调试

建立隔离运行目录：

```sh
rm -rf /tmp/kuayue-pack-dev
mkdir -p /tmp/kuayue-pack-dev/kuayue/packs
cp -R my-train-pack /tmp/kuayue-pack-dev/kuayue/packs/
mkdir -p /tmp/kuayue-pack-dev/kuayue
printf 'eula=true\n' > /tmp/kuayue-pack-dev/eula.txt
```

写 `/tmp/kuayue-pack-dev/kuayue/config.json`：

```json
{
  "packs": {
    "format_version": 1,
    "enabled": {
      "kuayue-core": true,
      "my-train-pack": true
    },
    "order": [
      "kuayue-core",
      "my-train-pack"
    ],
    "inventory": []
  }
}
```

启动：

```sh
KUAYUE_RUN_DIR=/tmp/kuayue-pack-dev \
  ./gradlew :modules:train-parts:runServer --console=plain
```

日志应依次出现包含以下关键词的行：

```text
Frozen 2 effective KuaYue packs; corepack=true
Prepared ... definitions across ... pack sources and ... registry claims
Applied KuaYue pack registration session
```

检查：

```text
/tmp/kuayue-pack-dev/kuayue/reports/packs/startup.json
```

确认目标包的 `requested_enabled`、`valid` 和 `effective` 为 `true`，并检查 `diagnostics` 是否为空或只有可接受的信息级记录。

## 8. 制作和验证 ZIP 包

从车包目录内部压缩，确保 `pack.json` 位于 ZIP 根：

```sh
cd my-train-pack
zip -r ../my-train-pack.zip pack.json pack.mcmeta parcels
```

`pack.mcmeta` 省略时从命令中删除。检查：

```sh
unzip -l ../my-train-pack.zip | head
```

预期第一层直接是：

```text
pack.json
parcels/
```

不能出现：

```text
my-train-pack/pack.json
```

把 ZIP 放进隔离实例的 `kuayue/packs/`，禁用或移除同 ID 的目录安装，再完整重启。目录包与 ZIP 包应由同一 `PackResourceSnapshotter` 生成等价的 `main` 和 `parcel-*` 根，得到相同定义、claims、有效集合与普通资源行为。底层 adapter 可能为一致性检查而复读源文件，但只保留一个启动基线。

## 9. 发布前检查

- `pack.json`、每个 `parcel.json`、索引和内容 JSON 均通过语法与 Schema 校验；
- parcel 以单个车型为单位，跨车型共享内容位于 `shared`；
- 所有声明索引都存在，未声明数据文件没有被误认为注册入口；
- 所有引用 ID 在当前有效包、显式直接依赖或 runtime 中可见；
- factory 和字段属于当前 Schema 与 handler 支持范围；
- 方块、物品、模型、纹理、语言和其他资源齐全；
- 目录包和 ZIP 包各完成一次隔离启动；
- `startup.json` 无阻塞诊断，目标包出现在 `effective_packs`；
- 关闭 corepack 时，自包含包仍能启动且不引用默认车型；
- 存档语义变化时更新版本或提供 `save_compatibility`；
- 包内没有符号链接、绝对路径、运行缓存、日志或整份存档；
- corepack 专用目录、`resources/common/models/block/bogey/**`、tracks、seat、toilet 和 provisions runtime 资源没有被误打包。
