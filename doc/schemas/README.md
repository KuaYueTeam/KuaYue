# KuaYue Schema 索引

`doc/schemas/` 保存可以脱离游戏运行的 JSON Schema，用于编辑器和提交前检查。运行时 Java 解析器仍是最终权威；Schema 不替代注册、资源加载或世界兼容性检查。

| 版本目录 | 覆盖范围 | 使用说明 |
|---|---|---|
| [`packs/v1/`](packs/v1/) | `pack.json`、`parcel.json`、`kuayue/config.json` | 预设包、外部车包、parcel 清单和实例开关 |
| [`data-driven/v1/`](data-driven/v1/) | KasugaLib 索引、注册组、方块、物品、转向架、轨道、餐食和动画剪辑 | 注册期内容与 KuaYue 使用的 reload 内容 |

`packs/v1` 和 `data-driven/v1` 是两套协议，不应交叉套用：

- 车包清单描述“安装什么、依赖什么、包含哪些 parcel”；
- data-driven 索引描述“当前命名空间要读取哪些内容文件”；
- `content.schema.json` 只校验注册期内容，`animation_clips` 使用独立的 reload Schema。

当前内置内容拆成 10 个 `kuayue-preset-*` 预设包，`pack.schema.json` 同时覆盖内置预设包和外部车包。必选的 `kuayue-preset-running-gear` 通过 `required: true` 标记；其他预设包依赖它并在默认配置中启用。

版本提升只应发生在现有字段语义改变、必需字段改变或运行时不再接受旧形状时。增加可选字段可以在同一主版本内完成，但必须同步本文、对应 Schema 和真实样例。
