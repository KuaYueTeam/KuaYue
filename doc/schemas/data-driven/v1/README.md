# KuaYue 数据驱动 Schema v1

本目录覆盖 KuaYue 当前实际使用的注册期和 reload 期 JSON 形状。Schema 依据当前 Java handler、record、解析器和仓库中的真实资源编写，用于编辑器补全、提交前检查和车包作者自测；运行时 Java 解析器仍是最终权威。

## 1. Schema 文件

| 文件或内容 | 使用文件 |
|---|---|
| `data/<namespace>/kasuga_lib/data_driven/*.json` | `index.schema.json` |
| `on_register` 指向的联合内容文件 | `content.schema.json` |
| 只含 `registry_groups` | `registry-groups.schema.json` |
| 只含 `blocks` | `blocks.schema.json` |
| 只含 `items` | `items.schema.json` |
| `blocks[].block_entity` 内嵌对象 | `block-entities.schema.json` |
| 只含 `bogey_sizes` | `bogey-sizes.schema.json` |
| 只含 `bogey_motions` | `bogey-motions.schema.json` |
| 只含 `bogey_models` | `bogey-models.schema.json` |
| 只含 `bogey_blocks` | `bogey-blocks.schema.json` |
| 只含 `bogey_styles` | `bogey-styles.schema.json` |
| 同文件含 `bogey_sizes`、`bogey_blocks`、`bogey_styles` | `bogeys.schema.json` |
| 含 `registry_groups` 与 `tracks` | `tracks.schema.json` |
| provisions 内容包 | `provisions.schema.json` |
| `on_reload` 指向的动画剪辑 | `animation-clips.schema.json` |

`content.schema.json` 是注册期联合 Schema，**不接受** `animation_clips` 顶层字段。动画剪辑必须从索引的 `on_reload` 进入，由 `animation-clips.schema.json` 校验。两个 wrapper 都引用 `content.schema.json#/$defs/*`，因此修改基础定义时只需维护一份结构。

## 2. 顶层类型与 Java 映射

| 顶层字段 | 生命周期 | Java handler/加载器 | 数据模型 |
|---|---|---|---|
| `registry_groups` | 注册期，阶段 0 | `RegistryGroupHandler` | `RegistryGroupDef` |
| `blocks` | 注册期，内容阶段 | `BlockTypeHandler` | `BlockDef` |
| 内嵌 `blocks[].block_entity` | 注册期，内嵌阶段 | `BlockEntityTypeHandler` | `BlockEntityDef` |
| `items` | 注册期，内容阶段 | `ItemTypeHandler` | `ItemDef` |
| `bogey_sizes` | 注册期，阶段 10 | `BogeySizeTypeHandler` | `BogeySizeDefinition` |
| `bogey_motions` | 注册期，阶段 20 | `BogeyMotionTypeHandler` | `BogeyMotionDefinition` |
| `bogey_models` | 注册期，阶段 30 | `BogeyModelTypeHandler` | `BogeyModelDefinition` |
| `bogey_blocks` | 注册期，阶段 40 | `BogeyBlockTypeHandler` | `BogeyBlockDefinition` |
| `bogey_styles` | 注册期，阶段 50 | `BogeyStyleTypeHandler` | `BogeyStyleDefinition` |
| `tracks` | 注册期，内容阶段 | `TrackTypeHandler` | `TrackDefinitionParser`、`TrackDefinition` |
| `animation_clips` | reload 期 | `FsmClipsReloadHandler`、`AnimationClipLoader` | `AnimationClip` |

`block_entities` 不是可写的顶层数组。它由 `blocks[].block_entity` 提取，并以父方块 ID 作为逻辑身份。

## 3. 索引与发现

内置 corepack、外部车包和 runtime source 都使用同一索引布局：

```text
data/<namespace>/kasuga_lib/data_driven/
```

路径相对 `data/<namespace>/`，必须带 `.json`，不能以 `/` 开头，不能包含反斜杠、空段、`.` 或 `..`：

```json
{
  "on_register": [
    "my_train/c22/groups.json",
    "my_train/c22/blocks.json"
  ],
  "on_reload": [
    "animation_clips/my_mechanism.json"
  ]
}
```

规则：

- `on_register` 与 `on_reload` 至少出现一个，数组可以为空；
- 同一路径不能同时出现在两个数组中；
- 注册期文件在启动注册前严格预检，reload 文件在预检时验证解码，在 `/reload` 时刷新；
- `animation_clips/` 没有目录 glob，必须显式列入 `on_reload`；
- KasugaLib 支持 `state_machines/` glob，但 KuaYue 当前没有对应数据，因此不维护 KuaYue 专用状态机 Schema。

在 corepack 中，索引必须位于具体 parcel，例如：

```text
parcels/c22/data/kuayue/kasuga_lib/data_driven/c22.json
parcels/shared/data/kuayue/kasuga_lib/data_driven/bogeys.json
```

corepack 根的 `data/kuayue/kasuga_lib/data_driven/` 是错误结构。

## 4. 注册组、方块和物品

`registry_groups` 提供方块属性和物品属性的继承起点。`parent` 必须引用另一注册组；`properties` 使用方块属性字段，`item_properties` 使用物品属性字段。方块和物品也可声明局部覆盖。

方块顶层字段：

```json
{
  "id": "mytrain:panel_lower",
  "type": "train_panel",
  "registry_group": "mytrain:panels",
  "properties": {
    "no_occlusion": true,
    "strength": [1.5, 3.0]
  },
  "item_properties": {
    "tab": "kuayue:train_panel_tab"
  },
  "params": {
    "begin_x": 0,
    "begin_y": 0,
    "end_x": 1,
    "end_y": 1
  }
}
```

当前 KuaYue 方块 factory 与 `params`：

| factory | `params` |
|---|---|
| `train_panel`、`train_hinge_panel`、`level_panel` | 可选 `begin_x`、`begin_y`、`end_x`、`end_y`；用于自定义平面范围 |
| `slab`、`slab_top`、`air_vent`、`skirt`、`level_window`、`train_unopenable_small_window` | 无字段 |
| `ladder` | 可选 `is_meter` |
| `train_small_window` | 可选 `height` |
| `train_openable_window` | 旧式 `wide`，或 `left`、`right`、`height` 组合；后者出现时优先 |
| `train_unopenable_window` | 可选 `wide`、`begin_y`、`end_y` |
| `hinge_slab` | 可选 `is_carport`、`width` |
| `custom_rendered_door` | 必须提供 `params`，其中必填 `left_models`、`right_models`；可选 `offset`、`open_offset`、`render_shape`、`is_slide_door` |
| `custom_rendered_end_face` | 可选 `door_type`、`left_model`、`right_model`、`frame_model` |
| `double_rotate_door` | 可选 `door_type`、`left_model`、`right_model`、`frame_model` |
| `freight_end_face` | 可选 `freight_type`：`c70` 或 `nx70`，缺省为 `c70` |
| `meter_carriage_end_face` | 可选 `door_type`、`outside`、`left_model`、`right_model`、`frame_model` |
| `diet_box` | 可选 `food_type`、`has_tooltip`、`remainder` |
| `drink` | 可选 `food_type`、`drink`、`has_particle`、`remainder` |
| `placement_food`、`dried_noodles` | 可选 `food_type`、`has_tooltip` |
| `soaked_noodles` | 可选 `has_tooltip` |

门类型枚举为 `fold`、`slide`、`slide_2`、`slide_3`、`rotate`、`rotate_single_sided`、`no_door`。`custom_rendered_door` 的 `render_shape` 为 `invisible`、`model`、`entityblock_animated`。其中的模型值是显式命名空间 ResourceLocation，运行时保留 namespace 并解析为其 `models/block/` 资源。

物品目前使用 `simple_item`。其 `params` 可包含：

```json
{
  "food": {
    "nutrition": 4,
    "saturation": 0.6,
    "fast": false,
    "always_eat": false
  }
}
```

`block_entity` 结构为 `{"type": "...", "params": {...}}`。当前 KuaYue 没有注册可供 JSON 使用的 block entity factory，因此 Schema 可以检查形状，实际声明仍会被严格预检拒绝。`state_properties` 同理：当前 KuaYue factory 没有实现 `StatefulBlockFactory`。

## 5. 转向架

转向架定义按依赖阶段拆分：

1. `bogey_sizes` 定义轮径；
2. `bogey_motions` 定义 `wheel_spin`、`rod_rotation` 或 `formula` 运动；
3. `bogey_models` 组合模型部件、局部变换和运动引用；
4. `bogey_blocks` 关联 size、默认 style、物理参数和方块属性；
5. `bogey_styles` 通过 variants 把 size、block、model 组合成 Create 样式。

重要约束：

- `formula` motion 必须有 `clip`，其他 motion 不允许 `clip`；
- model 每个 `local` 操作对象只能含 `translate`、`rotate`、`scale` 之一；
- block 的 `physics.wheel_point_spacing` 必须存在且为正；
- 每个 block 必须出现在一个 style variant 中，且只能出现一次；
- variant 的 size 必须与 block 的 size 一致；
- block 的 `default_style` 必须指向包含它的 style；
- 引用存在性、ID 唯一性和上述交叉约束由 Java 严格预检执行。

所有 registry ID、registry group、尺寸、转向架、模型、motion、clip 和轨道模型引用都必须写成显式 `namespace:path`。Schema 和 Java parser 都会拒绝裸 ID，避免 claim 使用宿主 namespace 而 apply 落到 `minecraft:`。

KuaYue 的 `bogeys.json` 只包含 sizes、blocks、styles；motions 和 models 位于独立文件，但由同一个 corepack `shared` 索引引用。

## 6. 轨道

当前只有 `simple_track` factory。轨道定义必须提供：

- `registry_group`；
- `default_bogey`；
- `material.id`、`name`、`particle`、正数 `half_distance`、非空 `sleepers`；
- `material.models.tie`、`left`、`right`。

轨道同时声明方块和物品 claim，因此注册时会产生两个 registry claims。`sleepers` 会在应用阶段解析为已注册方块；资源存在但注册不可见的 ID 仍会失败。

## 7. Provisions

provisions 不是新的顶层类型。实际文件 `provisions.json` 复用 `registry_groups`、`blocks`、`items`，外部布局为：

```text
provisions/resources/provisions/data/kuayue/kasugalib/provisions.json
provisions/resources/provisions/data/kuayue/kasuga_lib/data_driven/provisions.json
```

其方块 factory 收窄为 `diet_box`、`drink`、`placement_food`、`dried_noodles`、`soaked_noodles`，物品 factory 为 `simple_item`。这些限制来自 `ProvisionsFactories`。

## 8. 动画剪辑

`animation-clips.schema.json` 校验：

```json
{
  "animation_clips": [
    {
      "id": "mytrain:mechanism",
      "duration_seconds": 1.0,
      "variables": [],
      "formula_transforms": []
    }
  ]
}
```

剪辑还支持 `bones`、`morphs`、`frames` 和 `cameras`。已删除的 `functions`、`formula_parts` 不在 Schema 中，加载器会拒绝这两个旧字段。公式变量按声明顺序求值，`formula_transforms` 的目标可以是整个 model part 或其中的 bone。

## 9. 校验

JSON 语法检查：

```sh
jq empty doc/schemas/packs/v1/*.json \
  doc/schemas/data-driven/v1/*.json

find my-train-pack -name '*.json' -print0 \
  | xargs -0 -n1 jq empty
```

安装 Schema 校验器：

```sh
python3 -m pip install jsonschema referencing
```

以下脚本注册本目录全部 `$id`，再校验目标文件：

```python
from pathlib import Path
import json
import sys
from jsonschema import Draft202012Validator
from referencing import Registry, Resource

schema_dir = Path("doc/schemas")
schemas = {
    path.name: json.loads(path.read_text())
    for path in schema_dir.rglob("*.schema.json")
}
registry = Registry().with_resources(
    (schema["$id"], Resource.from_contents(schema))
    for schema in schemas.values()
)

document = json.loads(Path(sys.argv[1]).read_text())
validator = Draft202012Validator(schemas[sys.argv[2]], registry=registry)
errors = sorted(validator.iter_errors(document), key=lambda error: list(error.absolute_path))

for error in errors:
    pointer = "/" + "/".join(map(str, error.absolute_path))
    print(f"{pointer}: {error.message}")

raise SystemExit(1 if errors else 0)
```

示例：

```sh
python3 validate.py path/to/index.json index.schema.json
python3 validate.py path/to/content.json content.schema.json
python3 validate.py path/to/bogeys.json bogeys.schema.json
python3 validate.py path/to/qj_mechanism.json animation-clips.schema.json
```

## 10. 运行时边界

Schema 主动拒绝未知字段，适合让拼写错误在提交前失败。以下内容仍由 Java 或宿主环境决定：

- factory 是否在当前客户端/服务端注册；
- `registry_group`、bogey size/model/block/style 等跨文件引用是否存在；
- 同一 ID 是否与 corepack、runtime 或其他包冲突；
- `state_properties` 是否被目标 factory 支持；
- 状态组合是否超过运行时上限；
- 轨道 sleeper、bogey 默认方块等注册对象是否真的可见；
- 世界包集版本与 `save_compatibility`；
- 车包依赖、顺序和注册归属。

第三方 mod 增加 factory 时，应发布自己的扩展 Schema，不要放宽公共 factory 枚举。
