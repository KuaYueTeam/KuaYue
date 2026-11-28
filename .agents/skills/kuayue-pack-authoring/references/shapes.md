# 数据驱动形状与方块用途绑定

本参考只记录当前 KasugaLib shapes 合同和 KuaYue 方块绑定适配器已经接受的字段。开始写文件前，先确认当前检出及 CI 实际解析的 KasugaLib 依赖包含 `ShapeTypeHandler`、`ShapeDomainCompiler` 和形状 JSON Schema；本地 composite build 出现这些类，不能证明发布依赖也已经包含该能力。依赖尚未更新时，把形状方案留在设计说明中，不要加入车包并宣称可用。

## 1. 放置与索引

形状定义属于具体车包，并应由车型 parcel 拥有。默认把它放在该车型的内容目录，不要写到 `assets/`、构建产物或另一个包的源码里：

```text
modules/train-parts/resources/packs/<pack-id>/parcels/c22/
├── parcel.json
└── data/mytrain/
    ├── kasuga_lib/data_driven/c22.json
    └── c22/
        ├── shapes.json
        ├── blocks.json
        └── groups.json
```

`parcel.json.indexes` 使用投影后的逻辑路径：

```json
{
  "indexes": [
    "data/mytrain/kasuga_lib/data_driven/c22.json"
  ]
}
```

索引内路径相对于 `data/<namespace>/`。同一车型的形状和方块都要列入 `on_register`；把形状文件放在方块前面，便于按仓库迁移示例审查：

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

内置预设的 `parcel.json` 和索引位于 parcel 源目录，但 `parcel.json.indexes` 仍写 `data/...` 逻辑路径。外部标准布局使用同一规则。文件没有独立的 `format_version`；`shapes` 必须位于索引文件通过 `on_register` 指向的内容里，未列入索引不会成为形状目录的一部分。

形状 ID 必须完整命名空间化。车型自有定义使用本包 namespace，并在 path 中带车型/parcel 前缀，例如：

```text
mytrain:c22/shapes/door_frame
```

ID 的 path 也是作者维护的稳定标识，应能看出几何的车型、部件或状态语义；不要使用坐标哈希、随机串或 `shape_1` 这类临时编号。状态确实产生不同几何时，可把关键状态写入 ID，例如 `mytrain:c22/shapes/p31_sideboard_facing_north_hinge_left_closed`。坐标微调不应迫使 ID 随之改变。

同一车包可以包含多个车型拥有者。跨 owner 的形状或 parent 引用只对当前来源和显式直接依赖可见；不要依赖恰好也安装在实例里的另一个包。

## 2. 顶层定义

内容文件顶层 `shapes` 是定义数组：

```json
{
  "shapes": [
    {
      "id": "mytrain:c22/shapes/door_frame",
      "nodes": {
        "outer": [0.0, 0.0, 0.0, 32.0, 32.0, 4.0],
        "opening": [8.0, 0.0, 0.0, 24.0, 30.0, 4.0],
        "frame": {
          "op": "difference",
          "args": [
            "outer",
            "opening"
          ]
        }
      },
      "output": "frame"
    }
  ]
}
```

一次 prepare 会从当前来源及其直接依赖中选择全部 shapes，完成验证并编译为不可变
`ShapeCatalog`，然后才进入 apply；客户端 tick 和渲染不解析形状 JSON。因此跨文件前向
引用可以成立，但引用可见性仍受来源和直接依赖限制。

每个定义只接受以下字段：

| 字段 | 规则 |
|---|---|
| `id` | 必填，完整 `namespace:path` |
| `parent` | 可选，完整 `namespace:path`，最多一个父定义 |
| `nodes` | 节点名到表达式的对象；根定义必填 |
| `output` | 非空节点名字符串；根定义必填 |

节点名大小写敏感且不能为空。`union`、`intersection`、`difference`、`op`、`args`、`ref` 等词都可以作普通节点名；只有运算对象中的 `op` 字段有运算符含义。

节点名应尽可能说明它表示的几何部件、用途或状态，避免只写 `box`、`node1` 等通用名称，也不要用哈希值命名。优先采用简洁的 `snake_case`；例如 `door_leaf`、`frame`、`opening`。模型生成的单盒形状可用方块标识作为名称，并在状态确实区分几何时附上状态条件，例如 `p31_sideboard_facing_north_hinge_left_bounds`；同一几何覆盖所有状态时省略状态后缀。共享形状采用能概括共同用途的名称。空几何可命名为 `empty_geometry` 或 `empty`。`output` 必须同步指向选定的有意义节点名。

`output` 只选择有效节点表中的一个节点。即使只有一个节点，也必须显式写 `output`。不能把 Box6、空数组、运算对象或 ShapeRef 直接写进 `output`。

## 3. 表达式

节点值只接受五种表达式：

```text
Box6       := [minX, minY, minZ, maxX, maxY, maxZ]
Empty      := []
LocalName  := 非空 JSON string
ShapeRef   := {"ref": "namespace:path", "transform": 可选对象}
Operation  := {"op": "union" | "intersection" | "difference", "args": [...]}
```

### Box6

- 恰好 6 个 JSON `number`，顺序是 `minX,minY,minZ,maxX,maxY,maxZ`。
- JSON 没有独立的 `float` 类型；`16` 和 `16.0` 都是 `number`。作者示例使用小数写法便于表明坐标合同，但整数写法同样合法。
- 字符串数字、布尔值、`null` 和非有限值都不合法。
- 坐标按像素书写，绝对上限为 `65536.0`，每轴 `min < max`。
- 编译器先检查原始数值，再窄化为 Java `float`（IEEE 754 binary32）并再次检查有限性、范围和正尺寸。
- 像素只在编译时除以 `16.0f` 一次。进入编译几何后使用方块单位；后续引用、绑定或变换不得再次除以 16。

`[]` 是合法空几何，与缺少 `nodes`、缺少 `output` 或空字符串不同。面、线、点接触的交集按常规化实心 CSG 处理为无体积。

### 匿名操作数

运算的 `args` 每项都是独立表达式，可以直接嵌入匿名 Box6、空数组、局部节点名、ShapeRef 或另一个 Operation：

```json
{
  "shapes": [
    {
      "id": "mytrain:c22/shapes/nested",
      "nodes": {
        "result": {
          "op": "difference",
          "args": [
            {
              "op": "union",
              "args": [
                [0.0, 0.0, 0.0, 16.0, 16.0, 16.0],
                [8.0, 0.0, 0.0, 24.0, 16.0, 16.0]
              ]
            },
            [4.0, 4.0, 4.0, 12.0, 12.0, 12.0]
          ]
        }
      },
      "output": "result"
    }
  ]
}
```

`args` 自身不是 Box6；只有其中每个数组项才按表达式解释。六数 Box6 不能被拆成六个独立操作数。

### 运算对象

Operation 是封闭对象，只允许 `op` 和 `args`：

```json
{
  "op": "union",
  "args": [
    "left",
    [16.0, 0.0, 0.0, 32.0, 16.0, 16.0]
  ]
}
```

- `op` 严格为小写 `union`、`intersection` 或 `difference`。
- `args` 必须有 2–64 项，并保持声明顺序。
- `union` 和 `intersection` 交换、结合；`difference` 从 `args[0]` 开始，依次扣除后续项。
- `ref`、`transform`、`type` 或任何其他字段都不能和 `op` / `args` 混用。
- 旧式 `["union", ...]` 或 `["difference", ...]` 运算数组已被拒绝。表达式数组只能是空数组或六数 Box6。

## 4. 局部名称、引用和继承

字符串表达式只在当前定义合并后的 `nodes` 中查找，可前向引用；即使字符串看起来像 `namespace:path`，它仍是局部节点名。跨定义引用必须使用 ShapeRef 对象：

```json
{
  "ref": "mytrain:c22/shapes/door_frame"
}
```

ShapeRef 只取得目标定义当前 `output` 所指节点的完整几何。目标在该定义自己的节点作用域中编译；调用方不能访问目标的任意中间节点，也不能把自己的同名节点注入目标。需要公开另一个中间结果时，定义新的 Shape ID。

子定义通过 `parent` 继承：

- 父节点的有效表先进入子定义，子定义按节点名整项替换；数组和 `args` 从不拼接。
- 子定义省略 `output` 时继承父输出名；显式写 `output` 时整体替换。
- 子定义新增或覆盖的节点引用父节点同名节点时，解析目标是合并后的子节点，不是“覆盖前父节点”。这可能形成局部循环。
- 要引用父定义的最终几何，应使用 `{"ref":"父 ID"}`。

继承、局部节点引用和 ShapeRef 共同参与循环检查。直接自引用、父子环、跨形状环以及三者混合环都会在 apply 前失败；所有节点都要验证，包括当前 `output` 不可达的节点。

## 5. ShapeRef 变换

ShapeRef 可带封闭的 `transform` 对象：

```json
{
  "ref": "mytrain:c22/shapes/door_frame",
  "transform": {
    "scale": [1.0, 1.0, 1.0],
    "rotate_y": 90,
    "pivot": [8.0, 0.0, 8.0],
    "translate": [0.0, 0.0, 0.0]
  }
}
```

字段只允许 `scale`、`rotate_y`、`pivot`、`translate`，全部可选：

- `scale` 是三轴正数，默认 `[1.0,1.0,1.0]`。
- `rotate_y` 只能为 `0`、`90`、`180` 或 `270`，默认 `0`。
- `pivot` 和 `translate` 是像素坐标三元组；默认 pivot 为 `[8.0,0.0,8.0]`，默认平移为零。
- 固定顺序为正逐轴缩放、Y 轴四分之一圈旋转、平移。
- 变换施加在目标已编译的不可变几何上。引用结果已是方块单位，不再除以 16。

## 6. 方块状态绑定

已迁移的 KuaYue 方块使用 `params.shape_bindings` 把编译后的形状选择到独立用途槽。根对象只允许：

```text
shape, collision, visual, interaction, support, occlusion, states
```

替换旧 `params.aabb` 时，把原值保留在测试 fixture 供迁移等价性校验，并从生产方块声明中
删除 `params.aabb`。同一方块同时发布两份几何会让旧值看起来仍在使用，也会在审查和后续修改中
形成第二份事实来源；只有确实依赖原生回退的未迁移内容才继续使用 `params.aabb`。

每个槽接受 `native`、`empty`、形状引用或 `{"from":"另一个槽"}`。`native` 显式交回该方块原工厂实现；`empty` 显式生成空结果；`from` 只允许一个目标槽，alias 环会失败。形状引用写出 `ref` 和可选 `transform`，其变换规则与上一节相同。KuaYue 车辆绑定不接受内联 `nodes` / `output` 图。

`states` 是规则数组。每条规则必须有非空的 `when` 对象，并可覆盖任意槽：

```json
{
  "blocks": [
    {
      "id": "mytrain:c22_openable_window",
      "type": "train_openable_window",
      "registry_group": "mytrain:c22_panels",
      "params": {
        "shape_bindings": {
          "shape": {
            "ref": "mytrain:c22/shapes/window_closed"
          },
          "collision": {
            "from": "shape"
          },
          "states": [
            {
              "when": {
                "facing": "north",
                "open": "true"
              },
              "shape": {
                "ref": "mytrain:c22/shapes/window_open"
              }
            }
          ]
        }
      }
    }
  ]
}
```

状态规则的语义：

- `when` 的键必须是该方块实际拥有的状态属性，值使用方块状态序列化后的字符串。
- 一条规则只需声明关心的属性；对每个槽分别选择“条件命中且条件最多”的规则。
- 同一槽有两条同样具体度且同时命中的规则是歧义错误，启动预检会拒绝。
- 规则覆盖该槽的默认选择；未命中或未覆盖的槽继续使用根级选择或原工厂派生行为。
- 需要枚举状态时逐一列出，不支持通配符或把布尔/数字直接写进 `when`。
- `support` / `occlusion` 未显式绑定时保留库的原生派生规则；不要用空形状掩盖 unintentional occlusion。

形状数据描述的是方块局部几何；`facing`、`hinge`、`open` 等状态决定选择哪份已经按该状态命中的几何。不要依赖 ShapeRef 变换去替代真实 blockstate 模型方向，也不要让通用 shape 字段编码车辆业务状态机。

## 7. 保留在车型包内的规则

- 把车型形状定义放在该车型 parcel 的 `data/<namespace>/...` 中，并列入该 parcel 的索引。
- 形状 ID、方块绑定和目标引用使用本包 namespace；跨车型复用内容放在明确共享的 parcel，并保留唯一 owner。
- 跨另一个车包引用形状或 parent 时，在 `pack.json.dependencies.packs` 添加该 owner 的直接依赖。运行时再可见不等于依赖闭合。
- 不复制其他包的 shape 定义来规避依赖；需要公开中间结果时让 owner 增加独立 Shape ID。
- 不把形状写入 `doc/local/`、`run/`、`build/`、`src/compiled/`，也不要从不进入 ZIP 的开发目录引用。
- 内置预设只改 `modules/train-parts/resources/packs/` 源文件，再由 `generateKuaYuePresetPacks` 投影。

## 8. 验证

先做语法和仓库检查：

```sh
jq empty MY_SHAPES.json MY_BLOCKS.json PARCEL_INDEX.json
git diff --check
```

使用车包校验器和现有 Schema；`--require-schema` 不能在没有 `jsonschema` / `referencing` 时被当作通过：

```sh
python3 .agents/skills/kuayue-pack-authoring/scripts/validate_pack.py \
  PACK_ROOT --require-schema
```

形状文件按 KasugaLib 的正式 Schema 校验。KuaYue 的 canonical copy 位于 `doc/schemas/data-driven/v1/shapes.schema.json`，来源合同是 KasugaLibNext 的 `modules/data-driven/src/main/resources/schema/kasuga-data-driven-shapes.schema.json`。Schema 会检查结构、封闭字段、Box6 数量和原始范围；它不能代替编译器检查输出节点存在性、继承/引用/局部循环、float32 窄化退化、依赖可见性或 geometry 预算。

运行受影响测试：

```sh
./gradlew :modules:core:test :modules:train-parts:test --console=plain
./gradlew :modules:train-parts:test \
  --tests '*MeterGaugeShapeMigrationTest' \
  --tests '*KuaYueShapeBindingRuntimeTest' \
  --console=plain
```

内置预设再生成一次，并检查生成的 parcel 仍包含被索引的 shapes 文件：

```sh
./gradlew :modules:train-parts:generateKuaYuePresetPacks --console=plain
python3 .agents/skills/kuayue-pack-authoring/scripts/validate_pack.py \
  modules/train-parts/resources/packs/PACK_ID \
  --require-schema --require-durable-source
```

只有全部静态检查通过后，才向用户汇报并询问是否启动客户端。客户端可见视觉检查、提交和远程写入仍分别沿用原有的显式确认 gate。
