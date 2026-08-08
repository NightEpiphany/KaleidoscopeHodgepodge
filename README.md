# KaleidoscopeHodgepodge
# 森罗物语：杂烩
> Create your own custom dish by mixing various of ingredients together!

![Minecraft](https://img.shields.io/badge/Minecraft-Java%20Edition-brightgreen) 
![Fabric](https://img.shields.io/badge/Fabric-1.21.1|%2026.1.2|%2026.2-orange) 
![License](https://img.shields.io/badge/License-MIT-blue)

## Introduction

Kaleidoscope: Hodgepodge is an expansion mod made for Kaleidoscope: Cookery, it adds special soup and dishes for players, you can customize them by adding different ingredients in bowls and plates. The ingredients are obtainable by collecting the original dishes inside a wrapping bag. When you're trying to design your own dish ,you can unwrap them and manage their exact position to create many decorative models!

![Kaleidoscope Hodgepodge showcase](img/banner.png)

## Features | 功能

### Customizable Feasts | 可自定义食物方块

**English**

Kaleidoscope: Hodgepodge adds three customizable containers: a wooden plate, a porcelain plate, and a porcelain soup bowl. Place ingredients at the exact point selected on the container's top surface. Each ingredient keeps its pixel-space position and size, so multiple models can be arranged as a compact decorative feast without overlapping.

**中文**

森罗物语：杂烩新增三种可自定义容器：木盘、瓷盘和瓷汤碗。玩家可以根据指针在容器顶面选中的精确位置放置材料。每个材料都会保存像素坐标和尺寸，多个模型可以组合成紧凑的装饰性菜品，并自动避免模型之间重叠。

### Wrapping Bag | 打包纸袋

**English**

The Wrapping Bag collects one ingredient from a placeable food block. It supports both `FoodBiteBlock` and `StackableFoodBlock`; the source block is reduced by one bite or one count. If a source food has multiple registered ingredients, one candidate is selected at random. A filled bag uses a distinct full-bag model and shows the contained ingredient ID in its tooltip.

**中文**

打包纸袋可以从可放置食物方块中收集一份材料，同时兼容 `FoodBiteBlock` 和 `StackableFoodBlock`，来源方块会减少一口或一个数量。如果同一种来源食物对应多个材料，系统会随机选择其中一种。装有材料的纸袋会使用独立的满袋模型，并在提示信息中显示材料 ID。

### Placement and Recovery | 放置与回收

**English**

Use a filled Wrapping Bag on a compatible plate or soup bowl. Ingredients marked for dishes can only be placed on plates, ingredients marked for soups can only be placed in bowls, and `BOTH` ingredients work in either container. A full plate accepts up to 12 models and a soup bowl accepts up to 9. An empty bag can be aimed at a visible ingredient inside a customized container and used to recover that ingredient into the bag.

**中文**

拿着装有材料的纸袋对兼容的盘子或汤碗右键即可放置。标记为 `DISH` 的材料只能放入盘子，标记为 `SOUP` 的材料只能放入汤碗，`BOTH` 材料两者都兼容。盘子最多容纳 12 个模型，汤碗最多容纳 9 个模型。拿着空纸袋对准自定义容器内可见的材料右键，可以将该材料回收到纸袋中。

### Dynamic Models and Item Drops | 动态模型与物品掉落

**English**

All ingredient models are loaded through one shared `INGREDIENT_DISPLAY` item and selected by the ingredient data component; individual ingredient items do not need to be registered. Customized plates and bowls render the same saved ingredient snapshot in the world and in the item GUI. Breaking a non-empty customized container, including in Creative Mode, drops a complete item containing its container type, orientation, ingredient IDs, pixel positions, and size snapshots. Empty containers intentionally drop nothing.

**中文**

所有材料模型都通过统一的 `INGREDIENT_DISPLAY` 物品动态加载，并由材料数据组件选择模型，不需要为每种材料单独注册物品。自定义盘子和汤碗在世界中以及物品 GUI 中使用同一份材料快照渲染。破坏非空自定义容器时，即使处于创造模式，也会掉落保留容器类型、朝向、材料 ID、像素坐标和尺寸快照的完整物品；空容器则不会掉落物品。

### Tooltip Preview | 提示信息预览

**English**

When the Wrapping Bag contains an ingredient, its tooltip displays `Contained Ingredients: <ingredient id>` with styled text. Hover the bag in an inventory and hold **Shift** to view the ingredient's GUI item model in the tooltip.

**中文**

纸袋装有材料时，提示信息会显示 `Contained Ingredients: <材料 ID>`，并使用特殊字体颜色和样式。将鼠标悬停在背包中的纸袋上并按住 **Shift**，即可在提示框中查看该材料的 GUI 物品模型。

## Configuration | 配置

**English**

The JSON5 configuration file is `config/kaleidoscope_hodgepodge.json5`. It controls plate and bowl capacity, their base heights, the maximum ingredient model height, and optional debug logging. The configuration can be reloaded without replacing the last valid snapshot when a new file is invalid.

**中文**

JSON5 配置文件位于 `config/kaleidoscope_hodgepodge.json5`，可以调整盘子和汤碗的容量、基准高度、材料模型最大高度以及调试日志开关。配置重载失败时会保留上一份有效配置，不会影响正在运行的配置。

## Current Scope | 当前范围

**English:** Customized ingredient arrangements are currently decorative and cannot be eaten directly. Ingredients can still be individually recovered with an empty Wrapping Bag.

**中文：** 当前自定义材料组合主要用于装饰，暂时不能直接食用；玩家仍然可以使用空纸袋逐个回收材料。

## Compatibility | 兼容性

**English**

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API
- Kaleidoscope: Cookery
- Java 25 or newer

**中文**

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 或更高版本
- Fabric API
- 森罗物语：厨房
- Java 25 或更高版本
