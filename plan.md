KuaYue（跨越）车包系统及BlockBench Plugin插件设想

## Background

KuaYue是基于Create和KasugaLibNext的铁路模组。在KasugaLibNext有数据驱动系统的前提下，KuaYue计划实现列车“车包”化，并且完全数据驱动。

KasugaLib-BlockBenchPlugin是KasugaLib为其消费端模组提供的开发套件，在有数据驱动系统的前提下，该插件计划实现在BlockBench内建模，补全相关信息，最终直接产出数据驱动产物，可直接在对应目录内被加载

## Definition

### 车包系统

一个“车包”即一个pack，在车包内有不同的车型，一个车型相关的资源是一个parcel

一个parcel会包含如下内容：
- 车型元数据
    - 车型名称
    - 车型介绍
    - 车型的其他数据
- 所有方块及其模型、纹理
- 所有文本的本地化映射(i18n)
- 本parcel的蓝图数据结构及其子区块（蓝图是一个树形结构，一般用于游戏内的科技树，可以任意拼装）
- 声音
- ui主题
- tag、loot_table、recipe等
- ...

一个pack会包含如下内容：
- pack元数据
    - 名称
    - pack版本
    - 协议
    - ...
- 包含的所有parcel
- 依赖信息（用于构建依赖树和联动树）
    - 依赖的pack
    - 依赖的mod及其版本
    - 依赖的KuaYue版本、游戏版本
- 本pack的蓝图数据结构
- pack所需的本地化文件
- ...

### 数据驱动

KasugaLibNext提供数据驱动能力和一批内置的schema，而客户mod可以在KasugaLibNext提供的能力的基础上，自己定义一批schema，用于加载任意方块和按照需要定义加载内容。例如KuaYue自定义了bogey和track的一系列schema，并按照这个schema编写了数据驱动文件

## 预期的效果
- 插件可以用一个比较便捷的方式加载任意客户mod提供的自定义数据驱动schema，并根据schema导出数据驱动文件
    - 例如KuaYue的场景下，插件在加载schema后能导出车包
    - 例如在某家具mod的场景下，插件在加载schema后能导出有特定建模与动画的家具定义，及其资源文件
- 以纯数据驱动的方式让玩家可以在游戏内新增列车或其他游戏元素
- 根据pack和parcel的信息，可以自动构建游戏内科技树、物品栏等要素
- 产生一个加载契约，游戏文件夹内新增一个文件夹，玩家把pack放入之后，启动游戏时进行加载
- KuaYue本身提供一个corepack，封装在KuaYue模组的jar中，作为普通内置车包参与pack级启停，默认启用，v1允许玩家禁用
- 禁用corepack只排除它自带的数据与普通资源，不卸载KuaYue代码、通用工厂、handler和注册基础设施
- 依赖corepack内容的车包在corepack禁用时必须因依赖不满足而被拒绝，不得部分加载；曾使用corepack内容的世界按世界兼容规则提示或阻止进入

## 最终的链条

玩家使用插件在blockbench制作车包 -> 导出车包的数据 -> 放入游戏文件夹 -> 在游戏内游玩
