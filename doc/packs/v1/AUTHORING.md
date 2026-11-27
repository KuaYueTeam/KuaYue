# 制作 KuaYue 车包

## 1. 确定包与车型边界

开始写文件前先确定：

- 稳定车包 ID，例如 `my-train-pack`；
- 三段式 SemVer，例如 `1.0.0`；
- 内容 namespace，例如 `mytrain`；
- 每个车型一个 parcel ID，例如 `mytrain:c22`；
- 是否依赖 `kuayue-preset-running-gear` 或其他直接依赖；
- 需要注册的方块、物品、轨道和转向架 ID；
- 目标 Minecraft、NeoForge、KuaYue、KasugaLib、Create 版本。

车包 ID 使用 `[a-z][a-z0-9_-]*`。Minecraft namespace 使用小写字母、数字、`_`、`.`、`-`。

parcel 以单个车型为单位。车厢、机车、同一车型的变体及其专用转向架放在同一个车型 parcel；多个车型共用且不属于某一车型的转向架定义、运动、模型和动画放入 `shared` 或明确的通用 `bogey-*` parcel。禁止创建 `parcels/carriages/`、`parcels/locomotives/` 之类的大类目录。

## 2. 建立目录

```text
my-train-pack/
├── pack.json
├── icon.png                     # 可选，车包图标
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
    │           ├── trains/c22.json
    │           └── c22/
│               ├── shapes.json
    │               ├── groups.json
    │               └── blocks.json
    └── bogey-mytrain/
        ├── parcel.json
        └── assets/mytrain/models/block/bogey/mytrain/
```

只有确实被多个车型共用时才建立 `shared`。只服务单一车型的转向架不得单独拆出，应留在该车型 parcel。没有特定车型归属的通用转向架可按类型建立 `bogey-*` parcel；不要把所有通用转向架塞进一个 `bogey` parcel。

仓库中的 [sample-train](../../../examples/packs/sample-train/) 是可直接加载的最小目录包。[integration-train](../../../examples/packs/integration-train/) 额外演示 blockstate、模型、语言、配方、loot table 和 tag。

### 内置预设源布局

内置包使用便于按所有权维护的源布局：

```text
modules/train-parts/resources/packs/<pack-id>/
├── pack.json
├── icon.png
├── pack.mcmeta
├── assets/kuayue/lang/
└── parcels/<vehicle>/
    ├── parcel.json
    ├── blockstates/
    ├── models/
    ├── textures/
    └── data/kuayue/
```

`preset-packs.gradle` 会把 `blockstates`、`models`、`sounds`、`textures`、`recipes`、`loots` 和 data 规范化到标准 `assets/`、`data/` 路径，并合并各 parcel 的语言和 tag。外部发布包可以直接使用标准布局，也可以复用这一源布局在发布前运行同样的生成步骤。

车型专用转向架应和车型定义放在同一 parcel。跨车型基础样式由必选 `kuayue-preset-running-gear` 提供，车型 parcel 使用：

```json
{
  "bogey_style_variants": [
    {
      "id": "mytrain:c22_loco",
      "style": "kuayue:loco",
      "variants": [
        {
          "size": "mytrain:c22",
          "block": "mytrain:c22_bogey",
          "model": "mytrain:bogey/c22"
        }
      ]
    }
  ]
}
```

贡献目标必须可见。当前实现下，车型包应声明对 `kuayue-preset-running-gear` 的直接依赖。

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
      "minecraft": ">=1.21.1",
      "neoforge": ">=21.1.0",
      "kuayue": ">=2.0.0",
      "kasuga_lib": ">=1.0.0",
      "create": ">=6.0.0"
    },
    "packs": {
      "kuayue-preset-running-gear": ">=2.0.0"
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
- 只有引用基础 Create style、共享转向架定义或运行基础资源时才依赖 `kuayue-preset-running-gear`；
- 外部包不要声明 `required: true`；该字段只由内置 catalog 使用；
- 车包图标固定为包根目录的 PNG 文件 `icon.png`，不在 `pack.json` 中声明；文件可选，其他位置不会参与车包图标读取；
- `parcels` 顺序决定包内并级对同一路径的覆盖优先级，后声明者更高；
- 未列入 `parcels` 的 parcel 不会成为该包的注册根或资源根。

版本范围接受 `*`、`>=lower`、精确三段式版本和 `[lower,upper)`。不要使用 caret、tilde、其他比较
运算符或多个范围逗号组合。

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

新内容不再在 parcel 中写 `creative_filter`，而是在 train 定义中声明 `creative.tab` 和 `members`。可用 `creative.filter_order` 明确同一创造页内的展示顺序：数值越小越靠前，未声明时排在已声明项之后。`creative_filter` 只为旧外部包保留兼容；内置预设已经迁移到 train。

## 5. 写索引和内容

索引示例：

```json
{
  "on_register": [
    "c22/shapes.json",
    "c22/groups.json",
    "c22/blocks.json",
    "trains/c22.json"
  ],
  "on_reload": []
}
```

索引中的相对路径以该索引所在的 `data/<namespace>/` 为根，不再次添加 `parcels/c22/`。
形状定义放在车型 parcel 自己的 `shapes.json` 中，并由该 parcel 的索引列入
`on_register`；未列入索引的文件不会进入形状目录。

### 数据驱动形状

形状文件顶层是 `shapes` 定义数组，不写 `format_version`：

```json
{
  "shapes": [
    {
      "id": "mytrain:c22/shapes/frame",
      "nodes": {
        "outer": [0.0, 0.0, 0.0, 32.0, 32.0, 4.0],
        "opening": [8.0, 0.0, 0.0, 24.0, 30.0, 4.0],
        "result": {
          "op": "difference",
          "args": [
            "outer",
            "opening"
          ]
        }
      },
      "output": "result"
    }
  ]
}
```

- `nodes` 的每个值必须是 Box6、空数组、局部节点名、ShapeRef 或封闭的
  `op`/`args` 运算对象。
- Box6 是六个 JSON `number`，按像素解释，并在编译时窄化为 float32；单位换算只执行一次。
- 数组不能表示运算；旧的 `["union", ...]` 形式无效。`args` 中的每个 Box6、
  局部名称、ShapeRef 和嵌套运算都是独立操作数。
- `output` 只能选择当前有效节点表中的节点。ShapeRef 取得目标定义所选
  `output` 的完整几何，不会把目标的局部名称暴露给调用方。
- `parent` 按节点名继承；子定义省略 `output` 时继承父级输出。
- `params.shape_bindings` 用于方块状态绑定。具体槽、状态覆盖、alias 和变换合同见
  [数据驱动 Schema 说明](../../schemas/data-driven/v1/README.md#数据驱动形状与状态绑定)。
- 用 `params.shape_bindings` 完成迁移后，删除同一声明里的 `params.aabb`。旧值若仍需用于
  迁移等价性检查，应放在测试 fixture 中；生产 `params.aabb` 继续只服务未迁移内容。

形状文件按 [`shapes.schema.json`](../../schemas/data-driven/v1/shapes.schema.json)
校验。只有 KuaYue 实际解析的 KasugaLib 依赖已经包含形状 handler 和 Schema 时才可交付
`params.shape_bindings`；本地 composite build 可看到库源码不代表 CI 已获得同一 API。

train 把同一车型的方块、物品、转向架和创造页信息放进一个可查询身份：

```json
{
  "trains": [
    {
      "id": "mytrain:c22",
      "category": "carriage",
      "name_key": "parcel.mytrain.c22.name",
      "description_key": "parcel.mytrain.c22.description",
      "members": {
        "blocks": ["mytrain:c22_panel"],
        "items": [],
        "item_tags": ["mytrain:c22_parts"],
        "bogey_sizes": ["mytrain:c22"],
        "bogey_motions": [],
        "bogey_models": ["mytrain:bogey/c22"],
        "bogey_blocks": ["mytrain:c22_bogey"],
        "bogey_styles": ["kuayue:loco"],
        "bogey_style_variants": ["mytrain:c22_loco"]
      },
      "creative": {
        "tab": "kuayue:kuayue_carriage",
        "icon_item": "mytrain:c22_panel"
      }
    }
  ]
}
```

车型专用转向架应列在同一 train 的 `members.bogey_*` 中。基础样式可以来自 `kuayue-preset-running-gear`，车型通过 `bogey_style_variants` 贡献 variant。覆盖其他包的 train 时，覆盖包必须直接依赖原贡献包；对象递归合并、标量覆盖、数组整组替换。最终合并结果必须补齐三个文本字段和九个成员数组，数组可为空。

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
        "tab": "kuayue:kuayue_carriage"
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

可以在客户端设置页调整已安装包的下次启动选择。命令行调试时可建立隔离运行目录：

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
      "kuayue-preset-df": false,
      "kuayue-preset-hxd": false,
      "kuayue-preset-ss": false,
      "kuayue-preset-emu": false,
      "kuayue-preset-meter-gauge": false,
      "kuayue-preset-passenger-22": false,
      "kuayue-preset-passenger-25": false,
      "kuayue-preset-freight": false,
      "kuayue-preset-industrial": false,
      "my-train-pack": true
    },
    "order": [
      "kuayue-preset-df",
      "kuayue-preset-hxd",
      "kuayue-preset-ss",
      "kuayue-preset-emu",
      "kuayue-preset-meter-gauge",
      "kuayue-preset-passenger-22",
      "kuayue-preset-passenger-25",
      "kuayue-preset-freight",
      "kuayue-preset-industrial",
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
Frozen 2 effective KuaYue packs: [kuayue-preset-running-gear, my-train-pack]
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
zip -r ../my-train-pack.zip pack.json icon.png pack.mcmeta parcels
```

`icon.png` 或 `pack.mcmeta` 省略时从命令中对应删除。检查：

```sh
unzip -l ../my-train-pack.zip | head
```

预期第一层直接是：

```text
pack.json
icon.png
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
- 如提供车包图标，文件直接位于包根并命名为 `icon.png`；
- 所有声明索引都存在，未声明数据文件没有被误认为注册入口；
- 所有引用 ID 在当前有效包、显式直接依赖或 runtime 中可见；
- factory 和字段属于当前 Schema 与 handler 支持范围；
- 每个 shapes 文件由车型 parcel 的 `on_register` 声明，引用只在当前包或直接依赖内闭合；
- 每个车型 parcel 的索引都声明 train 文件，最终合并定义包含完整成员字段和可解析的语言键；
- 方块、物品、模型、纹理、语言和其他资源齐全；
- 目录包和 ZIP 包各完成一次隔离启动；
- `startup.json` 无阻塞诊断，目标包出现在 `effective_packs`；
- 关闭全部非必选预设包时，自包含包仍能启动且只引用 running-gear 或自身内容；
- 存档语义变化时更新版本或提供 `save_compatibility`；
- 包内没有符号链接、绝对路径、运行缓存、日志或整份存档；
- 内置 `resources/packs/<owner>/` 之外的历史车型目录、runtime seat、toilet 和 provisions 资源没有被误打包。
