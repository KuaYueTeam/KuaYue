# 车包与数据驱动 Schema 使用说明

## 1. 文件选择

| 要校验的文件 | Schema |
|---|---|
| 外部 `pack.json` | [`../../schemas/packs/v1/pack.schema.json`](../../schemas/packs/v1/pack.schema.json) |
| `parcel.json` | [`../../schemas/packs/v1/parcel.schema.json`](../../schemas/packs/v1/parcel.schema.json) |
| 实例 `kuayue/config.json` | [`../../schemas/packs/v1/config.schema.json`](../../schemas/packs/v1/config.schema.json) |
| `data/<namespace>/kasuga_lib/data_driven/*.json` | [`../../schemas/data-driven/v1/index.schema.json`](../../schemas/data-driven/v1/index.schema.json) |
| `on_register` 指向的联合内容 | [`../../schemas/data-driven/v1/content.schema.json`](../../schemas/data-driven/v1/content.schema.json) |
| 单一注册类型 | `data-driven/v1/*.schema.json` 中的对应文件 |
| 转向架组合文件 | [`../../schemas/data-driven/v1/bogeys.schema.json`](../../schemas/data-driven/v1/bogeys.schema.json) |
| 餐食 `provisions.json` | [`../../schemas/data-driven/v1/provisions.schema.json`](../../schemas/data-driven/v1/provisions.schema.json) |
| `animation_clips` reload 文件 | [`../../schemas/data-driven/v1/animation-clips.schema.json`](../../schemas/data-driven/v1/animation-clips.schema.json) |

Schema 负责静态 JSON 形状、字段枚举、基本类型和可由单文件表达的模式。运行时的严格预检仍是最终权威，负责来源身份、跨文件引用、依赖可见性、registry claims、handler 能力和 finalizer。

## 2. 车包清单

`pack.schema.json` 约束：

- `format_version = 1`；
- 稳定 pack ID；
- 三段式 SemVer；
- 非空 namespace 数组；
- 五个宿主依赖；
- 直接 pack 依赖；
- parcel 清单路径；
- 可选的存档兼容声明。

外部包不能声明保留 ID：

```text
kuayue-core
```

内置 `kuayue-core/pack.json` 由内部 `parseCore` 规则允许该 ID，因此不能用只面向外部作者的 `pack.schema.json` 直接校验其 `id` 字段。内置包的 parcel 清单仍可使用 `parcel.schema.json`。

`pack.mcmeta` 是 Minecraft metadata，不是 `pack.json` 的字段，也没有独立的 KuaYue 车包 Schema。它可以省略，由资源适配层生成宿主 metadata。

## 3. Parcel

`parcel.schema.json` 要求：

```json
{
  "format_version": 1,
  "id": "sample_train:c22",
  "name_key": "parcel.sample_train.c22.name",
  "indexes": [
    "data/sample_train/kasuga_lib/data_driven/c22.json"
  ]
}
```

`description_key` 和 `icon` 可选。`indexes` 必须是非重复字符串数组，但可以为空。每个索引必须匹配：

```text
data/<namespace>/kasuga_lib/data_driven/...json
```

namespace 必须在同一包的 `pack.json.namespaces` 中声明。Schema 检查路径形状；运行时还检查 parcel ID 的 namespace、图标 namespace 和索引文件是否真实存在。

parcel 按单个车型组织。`shared` 是跨车型共享内容的 parcel 名称，不是独立开关或保留 namespace。Schema 无法判断 parcel 是否按车型划分，作者必须遵守该结构规则。

## 4. 实例配置

`config.schema.json` 描述当前 `packs` 子域：

```json
{
  "packs": {
    "format_version": 1,
    "enabled": {
      "kuayue-core": true,
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

约束：

- `packs.format_version` 必须为 `1`；
- `enabled` 的值必须是布尔值；
- `order` 从低优先级到高优先级；
- `inventory` 是扫描结果的派生字段；
- `packs` 内不允许未知字段；
- pack ID 必须使用当前 ID 语法。

当前 `config.schema.json` 在顶层只允许 `packs`，而运行时解析器会保留 `packs` 之外的其他顶层字段。如果实例配置同时包含其他系统字段，应先抽取 `packs` 子域，或根据统一配置协议调整 Schema；在此之前不要把 Schema 的顶层限制误解成运行时已会删除这些字段。

启动时程序会补全 `enabled`、`order` 和 `inventory` 并重写配置。提交给 Schema 的 `inventory` 可以是空数组或当前扫描结果；不要把它当作手工授权列表。

## 5. 数据驱动索引

索引 Schema：

```json
{
  "on_register": [
    "c22/groups.json",
    "c22/blocks.json"
  ],
  "on_reload": []
}
```

`on_register` 与 `on_reload` 至少出现一个，数组可为空。路径相对索引所在的 `data/<namespace>/`，不能是绝对路径，不能包含反斜杠、空段、`.` 或 `..`。

Schema 能检查数组形状和路径格式，主要运行时检查包括：

- 同一路径不能同时属于两个生命周期；
- 引用文件必须在来源快照中存在；
- `on_register` 内容必须能被注册 handler 解析；
- `on_reload` 内容在注册前完成解码预检；
- 声明索引的 parcel 文件必须存在；
- 只有当前来源和显式直接依赖中的定义可用。

在 corepack 中，不得把索引放到车包根：

```text
kuayue-packs/kuayue-core/data/kuayue/kasuga_lib/data_driven/
```

索引应位于：

```text
kuayue-packs/kuayue-core/parcels/c22/data/kuayue/kasuga_lib/data_driven/c22.json
kuayue-packs/kuayue-core/parcels/shared/data/kuayue/kasuga_lib/data_driven/bogeys.json
```

外部包推荐把车型索引放进对应车型 parcel。`main` 根仍受平台包根索引发现规则支持，但车型内容不应借此绕过 parcel 组织。

## 6. 注册期内容

`content.schema.json` 是注册期联合 Schema。它覆盖当前 KuaYue 使用的顶层类型：

- `registry_groups`
- `blocks`
- `items`
- 内嵌 `blocks[].block_entity`
- `bogey_sizes`
- `bogey_motions`
- `bogey_models`
- `bogey_blocks`
- `bogey_styles`
- `tracks`

它不接受 `animation_clips`，避免把注册对象和 reload 数据混在同一生命周期。

单类型 Schema 适合编辑器补全和精确错误定位。`bogeys.schema.json` 用于同时包含 sizes、blocks、styles 的转向架聚合文件。`provisions.schema.json` 用于餐食模块复用的 groups、blocks、items 子集。

完整字段、factory、`params` 和业务约束见 [数据驱动 Schema 说明](../../schemas/data-driven/v1/README.md)。

## 7. 动画剪辑

动画剪辑只从索引的 `on_reload` 进入：

```text
animation_clips/<file>.json
```

使用 `animation-clips.schema.json` 校验。KasugaLib 状态机支持目录 glob，但 KuaYue 当前没有状态机数据，因此没有对应的 KuaYue 专用状态机 Schema。

`on_reload` 内容会在启动预检阶段解码，避免等到第一次 `/reload` 才发现静态格式错误。预检成功不会让 reload 内容新增 registry 对象。

## 8. JSON 语法检查

```sh
find my-train-pack -name '*.json' -print0 \
  | xargs -0 -n1 jq empty

jq empty \
  doc/schemas/packs/v1/*.json \
  doc/schemas/data-driven/v1/*.json
```

这只检查 JSON 语法，不检查 Schema 字段、跨文件引用或 Java 业务约束。

## 9. JSON Schema 校验

安装依赖：

```sh
python3 -m pip install jsonschema referencing
```

保存为 `validate_schema.py`：

```python
from pathlib import Path
import json
import sys

from jsonschema import Draft202012Validator
from referencing import Registry, Resource

schema_root = Path("doc/schemas")
schemas = {}
for path in schema_root.rglob("*.schema.json"):
    schema = json.loads(path.read_text(encoding="utf-8"))
    schemas[path.name] = schema

registry = Registry().with_resources(
    (schema["$id"], Resource.from_contents(schema))
    for schema in schemas.values()
)

document_path = Path(sys.argv[1])
schema_name = sys.argv[2]
document = json.loads(document_path.read_text(encoding="utf-8"))
validator = Draft202012Validator(schemas[schema_name], registry=registry)
errors = sorted(
    validator.iter_errors(document),
    key=lambda error: list(error.absolute_path),
)

for error in errors:
    pointer = "/" + "/".join(map(str, error.absolute_path))
    print(f"{pointer}: {error.message}")

raise SystemExit(1 if errors else 0)
```

示例：

```sh
python3 validate_schema.py pack.json pack.schema.json
python3 validate_schema.py parcels/c22/parcel.json parcel.schema.json
python3 validate_schema.py kuayue/config.json config.schema.json
python3 validate_schema.py content.json content.schema.json
python3 validate_schema.py animation.json animation-clips.schema.json
```

## 10. 编辑器映射

VS Code 可设置：

```json
{
  "json.schemas": [
    {
      "fileMatch": ["**/kuayue/config.json"],
      "url": "./doc/schemas/packs/v1/config.schema.json"
    },
    {
      "fileMatch": ["**/parcels/*/parcel.json"],
      "url": "./doc/schemas/packs/v1/parcel.schema.json"
    },
    {
      "fileMatch": ["**/kasuga_lib/data_driven/*.json"],
      "url": "./doc/schemas/data-driven/v1/index.schema.json"
    }
  ]
}
```

可用更细的文件名规则把 `groups.json`、`blocks.json`、`bogeys.json` 和 `tracks.json` 指向对应单类型 Schema。因为索引与被索引内容都位于 `data_driven/` 下，编辑器匹配时要避免把内容文件误判为索引。

## 11. Schema 不能替代的检查

Schema 不能证明：

- 目录包或 ZIP 包在运行时能安全读取；
- 索引指向的文件存在；
- factory 已在当前环境注册；
- handler 具备严格预检能力；
- 引用 ID 在允许的 owner 或直接依赖中可见；
- registry claim 与宿主或其他包冲突；
- apply 和 finalizer 能成功；
- 世界历史版本兼容；
- parcel 是否真的按车型组织、资源是否完整可渲染。

发布前至少执行：

1. JSON 语法和 Schema 校验；
2. 目录包隔离启动；
3. 同内容 ZIP 包隔离启动；
4. 检查 `startup.json` 无阻塞诊断；
5. 检查日志中的 `Prepared`、`Applied` 与 `kasuga/...` 资源层；
6. 关闭 corepack 再测自包含包。

目录与 ZIP 使用同一 Schema、清单映射和固定来源管线；压缩方式不会改变 JSON 契约。
