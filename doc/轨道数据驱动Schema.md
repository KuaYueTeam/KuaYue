# KuaYue 轨道数据驱动 Schema

当前入口文件：

- 索引：`modules/train-parts/resources/tracks/data/kuayue/kasuga_lib/data_driven/tracks.json`
- 内容：`modules/train-parts/resources/tracks/data/kuayue/tracks/tracks.json`

`registry_groups` 使用 KasugaLib 通用 schema；`tracks` 由 KuaYue 的
`TrackTypeHandler` 解析。

## 注册链路

1. `resources/tracks/module.toml` 将 `tracks/` 标记为资源编译模块。
2. `tracks.json` 放在 `data/kuayue/kasuga_lib/data_driven/`，只列出内容文件。
3. KasugaLib 先应用 `registry_groups`，再按内容阶段调用 KuaYue 的 `tracks` handler。
4. `TrackDefinitionParser` 校验 JSON，`TrackTypeHandler` 创建 `TrackMaterialReg` 和
   `TrackReg<SimpleTrackBlock>`。
5. `TrackReg` 同时创建轨道方块和物品；注册组提供默认属性，创造栏从
   `item_properties.tab` 继承。
6. `default_bogey` 在创建转向架锚点时从方块注册表延迟解析。

索引文件格式：

```json
{
  "on_register": [
    "tracks/tracks.json"
  ]
}
```

## 顶层结构

```json
{
  "registry_groups": [],
  "tracks": []
}
```

## track

```json
{
  "id": "kuayue:example_track",
  "type": "simple_track",
  "registry_group": "kuayue:track_blocks",
  "default_bogey": "kuayue:pk209p_bogey",
  "properties": {},
  "item_properties": {},
  "material": {}
}
```

| 字段 | 必填 | 含义 |
|---|---|---|
| `id` | 是 | 轨道方块和物品的注册 ID，必须唯一 |
| `type` | 是 | 轨道 factory；当前支持 `simple_track` |
| `registry_group` | 是 | 已由 `registry_groups` 声明的注册组 |
| `default_bogey` | 是 | 铺设后默认使用的转向架方块 ID，运行时延迟解析 |
| `properties` | 否 | 单条轨道覆盖的方块属性；继承注册组 |
| `item_properties` | 否 | 单条轨道覆盖的物品属性和创造栏；继承注册组 |
| `material` | 是 | Create `TrackMaterial` 定义 |

方块属性继续使用 KasugaLib 的字段，例如 `no_occlusion`、`strength`、
`sound_type`、`map_color`。物品属性继续使用 `JsonItemParser` 支持的字段。
`item_properties.tab` 未写入时继承注册组。

## material

```json
{
  "id": "kuayue:example",
  "name": "Example Track",
  "particle": "kuayue:block/palettes/stone_types/polished/andesite_cut_polished",
  "half_distance": 0.755,
  "sleepers": ["minecraft:andesite_slab"],
  "models": {
    "tie": "kuayue:block/track/standard/tie",
    "left": "kuayue:block/track/standard/segment_left",
    "right": "kuayue:block/track/standard/segment_right"
  }
}
```

| 字段 | 必填 | 约束 |
|---|---|---|
| `id` | 是 | Create `TrackMaterial` 注册 ID |
| `name` | 是 | TrackMaterial 显示名 |
| `particle` | 是 | 粒子纹理资源 ID |
| `half_distance` | 是 | 大于 0 的有限浮点数 |
| `sleepers` | 是 | 至少一个已存在方块 ID |
| `models.tie` | 是 | 枕木模型 ID |
| `models.left` | 是 | 左轨段模型 ID |
| `models.right` | 是 | 右轨段模型 ID |

模型路径相对于命名空间下的 `models/`。当前 schema 不负责加载 `.json`
以外的附加文件；OBJ、MTL 和纹理仍由资源包系统管理。

## 字段层级总览

```text
tracks[]
├── id
├── type
├── registry_group
├── default_bogey
├── properties?            KasugaLib block property object
├── item_properties?       KasugaLib item property object
└── material
    ├── id
    ├── name
    ├── particle
    ├── half_distance
    ├── sleepers[]
    └── models
        ├── tie
        ├── left
        └── right
```

`properties` 和 `item_properties` 不定义第二套字段；它们继续使用
`JsonPropertyParser`、`JsonItemParser` 的现有字段集合。轨道 handler 只负责把解析出的
修改器挂到 `TrackReg`，注册组属性沿 `registry_group -> track -> block/item` 链继承。

## 校验与继承

- `TrackDefinition` 和 `TrackMaterialDefinition` 在反序列化时校验必填字段、
  ResourceLocation 格式和数值范围。
- `TrackTypeHandler.parse` 拒绝未知字段、未知 `type` 和非对象嵌套结构。
- `TrackTypeHandler.apply` 拒绝未知注册组、缺失的枕木方块和非注册上下文。
- `default_bogey` 延迟到铺设时解析，允许转向架与轨道在同一个注册阶段创建。
- handler 注册 ID 使用 KasugaLib 的 `EffectiveId` 规则参与重复 ID 预检。
- `tracks[].id`、`material.id`、模型、粒子和枕木字段都必须是合法 ResourceLocation。
- `tracks`、`material`、`models` 三个对象层级都拒绝未知字段，字段拼写错误不会静默忽略。
- `default_bogey` 未解析为转向架方块时，`SimpleTrackBlock` 回退到 Create 的小型转向架。

## 当前迁移范围

已迁移 5 条轨道：

`standard_track`、`ballastless_track`、`tieless_track`、`meter_track`、
`guard_track`。

| 轨道方块 | TrackMaterial | 默认转向架 | half_distance |
|---|---|---|---:|
| `kuayue:standard_track` | `kuayue:standard` | `kuayue:pk209p_bogey` | 0.755 |
| `kuayue:ballastless_track` | `kuayue:ballastless` | `kuayue:pk209p_bogey` | 0.755 |
| `kuayue:tieless_track` | `kuayue:tieless` | `kuayue:pk209p_bogey` | 0.755 |
| `kuayue:meter_track` | `kuayue:meter` | `kuayue:mkz_bogey` | 0.525 |
| `kuayue:guard_track` | `kuayue:guard` | `kuayue:pk209p_bogey` | 0.755 |

已由 JSON 控制：

- 方块和物品注册 ID
- 注册组及方块属性
- 创造栏归属
- 默认转向架
- TrackMaterial ID、名称和粒子
- 轨距渲染偏移
- 枕木
- tie、left、right 模型

blockstate、模型文件、配方、战利品表、语言和标签仍保留原资源格式。

## 实现位置

| 职责 | 类型 |
|---|---|
| handler 注册 | `TrackDataDrivenModule` |
| JSON 字段校验 | `TrackDefinitionParser` |
| 注册副作用 | `TrackTypeHandler` |
| 顶层文档映射 | `TrackDefinitionFile` |
| 单条轨道映射 | `TrackDefinition` |
| TrackMaterial 映射 | `TrackMaterialDefinition` |
| 默认转向架行为 | `SimpleTrackBlock` |
| schema 回归测试 | `TrackDataDrivenSchemaTest` |

当前 5 个方块和 5 个物品都从 `TrackReg` 派生，不再保留旧的
`AllTracks`、`AllTrackMaterials` Java 注册表。

## 验证结果

强制编译后，两个 JSON 都进入 `modules/train-parts/build/libs/kuayue-2.0.0.jar`。
客户端启动日志应出现：

```text
Loaded 180 JSON entries across 3 types for mod 'kuayue'
Applying 9 entries for type 'registry_groups'
Applying 5 entries for type 'tracks'
Applying 166 entries for type 'blocks'
```

并能为 5 条轨道分别看到 block 与 item 的 `Posting deferred registry entry`。
