# VanillaModelLibrary

**一个给 Minecraft 原版生物提供模型资源的 Paper 插件。**

它会把原版生物的**静态 3D 模型**安装到 [RP Engine](https://github.com/resourcepack-ai/rp-engine)，并提供接口，让其他插件找到并使用这些模型。

例如，想做一个“让原版生物跳舞”的插件，就可以通过 VanillaModelLibrary 找到对应生物的模型，再由跳舞插件负责动画。

> **注意：** 本插件是模型资源库，**不会直接让生物跳舞**，也不负责播放动画。

## 它能做什么？

- **自动安装模型：** JAR 已内置适用于 Minecraft 26.2 的模型包（0.3.4），通常不需要手动下载资源。
- **对接 RP Engine：** 把模型交给 RP Engine 管理；只有模型文件变化时才更新对应资源。
- **识别生物外观：** 根据生物类型、年龄、颜色等信息，匹配模型包中已有的模型。
- **提供开发接口：** 其他插件可以查询模型，并获取对应的 RP Engine 物品。

目前只覆盖模型包中已支持的生物和外观，**并非所有原版生物都有可用模型**。

## 安装

需要 **Paper 26.2、Java 25** 和前置插件 **RP Engine**（项目目前基于 RP Engine 0.2.20 构建）。

1. 在服务器上安装 RP Engine。
2. 把 `VanillaModelLibrary.jar` 放进服务器的 `plugins/` 文件夹。
3. 重启服务器。插件会自动释放内置模型包，并安装到 RP Engine。
4. 在游戏或控制台输入 `/vml status`，看到 `ready=true` 就表示模型库已就绪。

默认情况下，模型资源会保存在 `plugins/VanillaModelLibrary/assets/`，并安装到 `plugins/RPEngine/content/vanillamobs/`。

想替换模型包？可以把新的模型 ZIP 或解压后的模型目录放进 `assets/`。如果同时放了多个版本，需要在 `config.yml` 的 `assets.package` 中指定要使用哪一个，然后执行 `/vml reload`。

## 命令

| 命令 | 用途 |
| --- | --- |
| `/vml status` | 查看插件、模型包和 RP Engine 是否正常 |
| `/vml reload` | 重新读取模型资源，必要时更新 RP Engine |
| `/vml verify` | 检查模型包是否完整，不会重新安装 |
| `/vml items` | 查看哪些模型已经注册到 RP Engine |
| `/vml selftest` | 临时生成测试生物，检查模型匹配后删除 |

`/vml` 是 `/vanillamobs` 的简写。以上命令默认仅 OP 可用（权限：`vanillamobs.admin`）。

## 给插件开发者

VanillaModelLibrary 提供 Java API，可以根据实体当前外观查找模型：

```java
VanillaModelLibraryApi.get().ifPresent(api -> {
    if (!api.ready()) return;

    api.selectForDisplay(entity).ifPresent(selection -> {
        String modelId = selection.modelId();
        api.createItem(modelId).ifPresent(item -> {
            // 将这个模型物品交给你的显示或动画逻辑
        });
    });
});
```

- `selectForDisplay(entity)`：适合临时隐藏原版生物、改为显示完整自定义模型的场景（例如跳舞）。
- `select(entity)`：严格按照生物的实时外观匹配。
- `models()` / `model(id)`：查询模型列表和详细信息。
- `observe(entity)`：获取生物外观信息。

找不到对应模型时会返回空结果，不会擅自用其他模型替代。

## 从源码构建

使用 **JDK 25** 和 **Gradle**：

```bash
gradle build
```

构建好的 JAR 位于 `build/libs/`。
