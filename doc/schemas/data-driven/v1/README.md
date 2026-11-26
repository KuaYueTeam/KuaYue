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
| 只含 `bogey_style_variants` | `bogey-style-variants.schema.json` |
| 同文件组合多个 `bogey_*` 类型 | `bogeys.schema.json` |
| 含 `registry_groups` 与 `tracks` | `tracks.schema.json` |
| 只含 `trains` | `trains.schema.json` |
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
| `bogey_style_variants` | 注册期，阶段 51 | `BogeyStyleVariantsTypeHandler` | `BogeyStyleVariantsDefinition` |
| `tracks` | 注册期，内容阶段 | `TrackTypeHandler` | `TrackDefinitionParser`、`TrackDefinition` |
| `trains` | 注册期，阶段 60，不注册对象 | `TrainTypeHandler` | `TrainPatch`、`KuaYueTrainDefinition` |
| `animation_clips` | reload 期 | `FsmClipsReloadHandler`、`AnimationClipLoader` | `AnimationClip` |

`block_entities` 不是可写的顶层数组。它由 `blocks[].block_entity` 提取，并以父方块 ID 作为逻辑身份。

## 3. 索引与发现

内置预设包、外部车包和 runtime source 都使用同一索引布局：

```text
data/<namespace>/kasuga_lib/data_driven/
```

路径相对 `data/<namespace>/`，必须带 `.json`，不能以 `/` 开头，不能包含反斜杠、空段、`.` 或 `..`：

```json
{
  "on_register": [
    "my_train/c22/groups.json",
    "my_train/c22/blocks.json",
    "trains/c22.json"
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

内置预设包中，数据驱动索引位于具体 parcel，例如：

```text
packs/kuayue-preset-passenger-22/parcels/c22/data/kuayue/kasuga_lib/data_driven/c22.json
packs/kuayue-preset-ss/parcels/ss3/data/kuayue/kasuga_lib/data_driven/ss3.json
packs/kuayue-preset-running-gear/parcels/shared/data/kuayue/kasuga_lib/data_driven/bogeys.json
```

车型注册入口由车型 parcel 声明；`shared` 只保留跨车型共用的运行数据。外部包仍可遵循同一 parcel 布局。

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
    "tab": "kuayue:kuayue_carriage"
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

下表的 TrainPanel 家族 factory 还可统一使用可选的 `multi_cell`；只有 `height_slab` 和 `jy290_ac` 不属于该家族。

所有 TrainPanel 家族 factory 还支持可选的 `params.aabb`：从模型几何生成的精确包围盒。键是规范化状态串（如 `facing=north,hinge=left,open=false`，无状态变体用 `""`），值是像素坐标 `[x1,y1,z1,x2,y2,z2]`，空模型用 `[]`。配置后 `getShape` 返回对应盒，`getCollisionShape` 委托 `getShape`，`getInteractionShape` 默认也委托 `getShape`，因此可见形、交互形、碰撞形三者一致；未配置时保持各 factory 原有形状。运行时从完整状态键逐级降级匹配到较短前缀，以兼容 blockstate 变体只声明部分属性的情况（例如只按 `facing` 变化的梯子）。

| factory | `params` |
|---|---|
| `train_panel`、`train_hinge_panel`、`level_panel` | 可选 `begin_x`、`begin_y`、`end_x`、`end_y`；用于自定义平面范围 |
| `slab`、`slab_top`、`height_slab`、`air_vent`、`skirt`、`level_window`、`train_unopenable_small_window` | 无字段 |
| `ladder` | 可选 `is_meter` |
| `train_small_window` | 可选 `height` |
| `train_openable_window` | 旧式 `wide`，或 `left`、`right`、`height` 组合；后者出现时优先 |
| `train_unopenable_window` | 可选 `wide`、`begin_x`、`end_x`、`begin_y`、`end_y` |
| `hinge_slab` | 可选 `is_carport`、`width` |
| `custom_rendered_door` | 必须提供 `params`，其中必填 `left_models`、`right_models`；可选 `offset`、`open_offset`、`render_shape`、`is_slide_door` |
| `custom_rendered_end_face` | 可选 `door_type`、`left_model`、`right_model`、`frame_model` |
| `double_rotate_door` | 可选 `door_type`、`left_model`、`right_model`、`frame_model` |
| `freight_end_face` | 可选 `freight_type`：`c70` 或 `nx70`，缺省为 `c70` |
| `meter_carriage_end_face` | 可选 `door_type`、`outside`、`left_model`、`right_model`、`frame_model` |
| `full_shape_directional`、`hxd3d_cowcatcher`、`single_arm_pantograph`、`head`、`chimney`、`df5_chimney` | 无其他字段 |
| `jy290_ac` | 无字段 |
| `train_door`、`jy290_door` | 可选 `begin_x`、`begin_y`、`end_x`、`end_y`；`jy290_door` 使用 JY290 专用门形和玩家通行碰撞 |
| `proxy_model` | 通用 proxy 模型宿主，方块本身无行为参数；配合 `proxy_model_be`，其 `params.model` 或 `params.model_groups` 必填，`default_model`、`model_name`、`yaw_offset`、`facing_yaw`、`model_by_state` 可选；支持 `state_properties` |
| `jy_maintenance_outrigger` | KuaYue 专用 JY 工程车支腿，使用 `train_panel_proxy_be`；右键和动画由专用方块类实现，碰撞为完整方块 |
| `jy_maintenance_floor` | KuaYue 专用 JY 工程车地板与梯子地板，保留半砖碰撞和扳手旋转，并可当作梯子攀爬 |
| `jy_maintenance_carport` | KuaYue 专用 JY 工程车顶，使用 `train_panel_proxy_be`；保留半砖碰撞和扳手旋转，并直接加载 BBModel |
| `jy_maintenance_console` | KuaYue 专用金鹰工程车控制台，使用通用 `ProxyModelBlock` 与 `proxy_model_be`；具有 `facing`，选择框和碰撞箱共用一个模型包围盒 |
| `jy_maintenance_endface` | KuaYue 专用 JY 工程车端面，使用 `train_panel_proxy_be`；不声明 multi-cell，门和端面的选中框复用状态化碰撞箱 |
| `jy_maintenance_door` | KuaYue 专用 JY 工程车车门，使用 `train_panel_proxy_be`；支持左右模型、开关状态、播放 `door_open` 和状态化碰撞箱 |
| `jy_maintenance_panel` | KuaYue 专用 JY 工程车箱板，使用 `train_panel_proxy_be`；通过 `hinge` 选择左右模型 |
| `jy_maintenance_tail` | KuaYue 专用 JY 工程车尾部，使用 `train_panel_proxy_be`；碰撞由 BBModel 几何生成并忽略 y 高度为零的辅助 cube |
| `diet_box` | 可选 `food_type`、`has_tooltip`、`remainder` |
| `drink` | 可选 `food_type`、`drink`、`has_particle`、`remainder` |
| `placement_food`、`dried_noodles` | 可选 `food_type`、`has_tooltip` |
| `soaked_noodles` | 可选 `has_tooltip` |

门类型枚举为 `fold`、`slide`、`slide_2`、`slide_3`、`rotate`、`rotate_single_sided`、`no_door`。`custom_rendered_door` 的 `render_shape` 为 `invisible`、`model`、`entityblock_animated`。其中的模型值是显式命名空间 ResourceLocation，运行时保留 namespace 并解析为其 `models/block/` 资源。

所有由 KuaYue `TrainPanelBlock` 家族工厂创建的方块都支持可选的多格选择占用：

```json
{
  "params": {
    "multi_cell": {
      "cells": [[-1, 0, 0], [0, 1, 0], [1, 0, 1]]
    }
  }
}
```

`cells` 是北向 bind pose 下相对 anchor 的整数格偏移，不包含 `[0,0,0]`；方向方块会随 `HORIZONTAL_FACING` 旋转。companion 只代理准星拾取、轮廓和交互，自身无碰撞、无模型、无物品；目标格已被占用时跳过并记录诊断，不移除已有方块。该能力属于 KuaYue 的 `TrainPanelBlock`，不属于 `proxy_model_be`；需要自定义 model composition 的面板使用 `TrainPanelProxyBlock` 与 `train_panel_proxy_be`。

当占用格随方块状态变化时，使用 `cells_by_state`。映射键是状态属性名，属性值必须使用方块状态序列化名：

```json
{
  "params": {
    "multi_cell": {
      "cells_by_state": {
        "hinge": {
          "left": [[1, 0, 0], [0, 1, 0], [1, 1, 0]],
          "right": [[-1, 0, 0], [0, 1, 0], [-1, 1, 0]]
        }
      }
    }
  }
}
```

状态切换时 manager 会删除不再需要的旧 companion 并创建新 companion。`cells` 可作为状态规则未命中时的默认值；每个状态规则仍排除 anchor，并随 `facing` 旋转。

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

`block_entity` 结构为 `{"type": "...", "params": {...}}`。KasugaLibNext 提供的 `proxy_model` 工厂实现了 `StatefulBlockFactory`；与 `proxy_model_be` 配合时，可直接声明 `facing` 等状态属性并加载 `.bbmodel` 等运行时模型。KuaYue 专用 panel proxy 使用 `train_panel_proxy_be`，以保留 `TrainPanelBlock` 交互并在 KuaYue 侧提供 composition。声明中未使用 `facing` 时，模型保持默认朝向。

```json
{
  "id": "example:vehicle_head",
  "type": "proxy_model",
  "state_properties": {
    "facing": {
      "type": "vanilla",
      "property": "HORIZONTAL_FACING",
      "default": "north"
    }
  },
  "block_entity": {
    "type": "proxy_model_be",
    "params": {
      "model": "example:models/block/vehicle_head.bbmodel",
      "yaw_offset": 0
    }
  }
}
```

`model_groups` 用于同时加载多个原生模型。每个组内的 `parts` 有稳定 `id`、模型资源、可选 `local` 变换；注册期会编译为不可变的 `CompiledModelGroup`，`model_by_state` 选择的是整个编译组。同一方块的所有组必须保持相同且同序的 part ID，才能在状态切换时复用动画 target 地址：

```json
{
  "type": "proxy_model",
  "state_properties": {
    "facing": {
      "type": "vanilla",
      "property": "HORIZONTAL_FACING",
      "default": "north"
    },
    "hinge": {
      "type": "vanilla",
      "property": "DOOR_HINGE",
      "default": "left"
    }
  },
  "block_entity": {
    "type": "proxy_model_be",
    "params": {
      "default_model": "left",
      "model_groups": {
        "left": {
          "parts": [
            {
              "id": "body",
              "model": "example:models/block/left.bbmodel"
            }
          ]
        },
        "right": {
          "parts": [
            {
              "id": "body",
              "model": "example:models/block/right.bbmodel"
            }
          ]
        }
      },
      "model_by_state": {
        "hinge": {
          "left": "left",
          "right": "right"
        }
      }
    }
  }
}
```

声明 `DOOR_HINGE` 时，`proxy_model` 会按玩家点击位置选择左右形态。通用 proxy 不定义状态到动画片段的映射；需要动画交互的方块应在消费 mod 中实现专用方块类，用 Java `ProxyControllerPort` 读取状态、发出 `PlaybackCommand`，并用 target evaluator 把已提交的播放时间采样到模型姿态。

## 5. 转向架与跨包样式贡献

转向架定义按依赖阶段拆分：

1. `bogey_sizes` 定义轮径；
2. `bogey_motions` 定义 `wheel_spin`、`rod_rotation` 或 `formula` 运动；
3. `bogey_models` 组合模型部件、局部变换和运动引用；
4. `bogey_blocks` 关联 size、默认 style、物理参数和方块属性；
5. `bogey_styles` 通过 variants 把 size、block、model 组合成 Create 样式；
6. `bogey_style_variants` 从车型包向已有 style 追加 variants。

重要约束：

- `formula` motion 必须有 `clip`，其他 motion 不允许 `clip`；
- model 每个 `local` 操作对象只能含 `translate`、`rotate`、`scale` 之一；
- block 的 `physics.wheel_point_spacing` 必须存在且为正；
- 每个 block 必须出现在一个 style variant 或 style contribution 中，且只能出现一次；
- variant 的 size 必须与 block 的 size 一致；
- block 的 `default_style` 必须指向包含它的 style；
- `bogey_styles[].variants` 可以为空，用作只提供样式壳的基础 definition；
- style contribution 的 `variants` 不能为空，贡献 ID 在整个注册集合中必须唯一；
- 同一目标 style 内，size 和 block 不能同时由基础 variants 与 contribution 重复提供。

车型包向基础包样式追加 variants 的示例：

```json
{
  "bogey_style_variants": [
    {
      "id": "mytrain:df11g_loco",
      "style": "kuayue:loco",
      "variants": [
        {
          "size": "mytrain:df11g",
          "block": "mytrain:df11g_bogey",
          "model": "mytrain:bogey/df11g"
        }
      ]
    }
  ]
}
```

贡献包必须通过 `pack.json.dependencies.packs` 依赖 style 所有者的包。当前内置车型包都依赖必选的 `kuayue-preset-running-gear`。引用存在性、ID 唯一性和上述交叉约束由 Java 严格预检执行。

所有 registry ID、registry group、尺寸、转向架、模型、motion、clip 和轨道模型引用都必须写成显式 `namespace:path`。Schema 和 Java parser 都会拒绝裸 ID，避免 claim 使用宿主 namespace 而 apply 落到 `minecraft:`。

当前资源按所有权拆成多种组合：

```text
running-gear/shared:
  sizes + blocks + styles + 通用 style variants

running-gear/bogey-*:
  具体通用转向架的 blockstate、模型、贴图和语言

车型 parcel:
  sizes + blocks + models + 车型 style variants
```

因此不能再用单一 `bogeys.json` 路径判断转向架内容是否完整。查看对应 parcel 的 `indexes` 与 `on_register` 引用。

## 6. Train 集合定义

`trains` 是 KuaYue 独有的 overlay 类型。它把同一车型的方块、物品、转向架、创造页显示信息和系统标签关联起来，供创造筛选、反向索引及后续模块查询。它不注册新的 Minecraft registry 对象。

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

单来源可以只声明需要修改的字段。相同 train ID 会按包优先级从低到高合并：

- 对象字段递归合并；
- 标量字段由高优先级覆盖；
- 数组整体替换，`[]` 会显式清空低优先级内容；
- 高优先级来源只声明 `members.items` 时，其余字段继续继承低优先级来源；
- 覆盖者必须通过 `pack.json.dependencies.packs` 直接依赖低优先级贡献包；
- 同一 owner 不允许为同一 train ID 声明两次。

合并后的完整定义必须包含非空 `category`、`name_key`、`description_key`，以及 `members` 中九个成员数组；数组可以为空。`creative` 可省略；出现时 `creative.tab` 必填，`icon_item` 和 `filter_order` 可选。同一创造页按 `filter_order` 升序排列，未声明该字段的筛选项排在已声明项之后。

注册成功后，每个 train 会生成：

```text
kuayue:system/train/<train namespace>/<train path>
```

该 item tag 会合并 `members.blocks` 对应的 BlockItem、`members.items` 和 `members.item_tags`；同一身份另生成 block tag。`creative` 存在时，创造筛选页会使用这个 train tag。旧 `parcel.json.creative_filter` 只作为外部兼容入口，新内容应声明 train。

## 7. 轨道

当前只有 `simple_track` factory。轨道定义必须提供：

- `registry_group`；
- `default_bogey`；
- `material.id`、`name`、`particle`、正数 `half_distance`、非空 `sleepers`；
- `material.models.tie`、`left`、`right`。

轨道同时声明方块和物品 claim，因此注册时会产生两个 registry claims。`sleepers` 会在应用阶段解析为已注册方块；资源存在但注册不可见的 ID 仍会失败。

## 8. Provisions

provisions 不是新的顶层类型。实际文件 `provisions.json` 复用 `registry_groups`、`blocks`、`items`，外部布局为：

```text
provisions/resources/provisions/data/kuayue/kasugalib/provisions.json
provisions/resources/provisions/data/kuayue/kasuga_lib/data_driven/provisions.json
```

其方块 factory 收窄为 `diet_box`、`drink`、`placement_food`、`dried_noodles`、`soaked_noodles`，物品 factory 为 `simple_item`。这些限制来自 `ProvisionsFactories`。

## 9. 动画剪辑

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

## 10. 校验

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
python3 validate.py path/to/trains.json trains.schema.json
python3 validate.py path/to/qj_mechanism.json animation-clips.schema.json
```

## 11. 运行时边界

Schema 主动拒绝未知字段，适合让拼写错误在提交前失败。以下内容仍由 Java 或宿主环境决定：

- factory 是否在当前客户端/服务端注册；
- `registry_group`、bogey size/model/block/style 等跨文件引用是否存在；
- 同一 ID 是否与必选预设包、runtime 或其他包冲突；
- `state_properties` 是否被目标 factory 支持；
- 状态组合是否超过运行时上限；
- 轨道 sleeper、bogey 默认方块等注册对象是否真的可见；
- 世界包集版本与 `save_compatibility`；
- 车包依赖、顺序和注册归属。

第三方 mod 增加 factory 时，应发布自己的扩展 Schema，不要放宽公共 factory 枚举。
