# VanillaModelLibrary

独立 Paper 插件，负责 `vanillamobs` 静态资源的**安装、校验和选择**，并把 modelId 映射到
RP Engine 注册的 ITEM。它不做骨骼动画；官方 0.3.4 delivery ZIP 内置在 JAR 中，
首次启用释放到 assets/，外部已有资产优先。

## 内置资源与外部覆盖

插件 JAR 内置官方 **0.3.4 delivery ZIP**；首次启用会释放到 `assets/`，之后仍可按版本替换。
外部已存在的 assets 优先，插件不会覆盖服务器主人的文件。

内置 ZIP 释放后的布局：

```
plugins/VanillaModelLibrary/assets/VanillaModels-26.2-0.3.4/
  dist/
    catalog.json
    manifest.json
    report.json
    content/vanillamobs/...
```

也可以直接放 `VanillaModels-26.2-0.3.4.zip`，插件会安全解压一次。多个版本同时存在时必须用
`assets.package` 显式选择，绝不静默挑一个。


## catalog 驱动

模型清单不是代码里的白名单。插件读取 catalog.json 的 181 条记录（0.3.4 中 136 条 supported、
87 种候选实体，50 种有 supported 模型），按条件匹配后返回 modelId；新增模型只要 catalog/manifest/bbmodel 合法，就不需要
改 Java 代码。

加载时校验：重复 modelId、未知条件键、同一实体类型的重复/重叠规则都会拒绝整个包，避免
“部分规则永远匹配不到”或“两个规则同时命中”的静默错误。

## 安装流程

0. 如果 `assets/` 为空，从 JAR 释放内置的 `VanillaModels-26.2-0.3.4.zip`。
1. 读取 `manifest.json` / `catalog.json` / `report.json`，校验 schema 1。
2. 拒绝 Minecraft 版本不匹配、namespace 不是 `vanillamobs`、路径越界、SHA-256/大小不符、
   文件集合与 manifest 不一致的包。被拒绝时**不覆盖**已有安装。
3. 只把 `content/vanillamobs/` 写成 `plugins/RPEngine/content/vanillamobs/`；逐文件比较
   SHA-256，只写不同的文件。
4. 资源确实变化时才调用一次 `RPEnginePlugin.reload()`；插件代码升级不会重新分发资源。
5. 校验每个 supported modelId 在 RP Engine 里确实是 ITEM，否则 `ready=false`。

安装状态写在 `plugins/VanillaModelLibrary/installed-state.json`，不写进 RP Engine 内容目录。

## 稳定 API

```java
Optional<VanillaModelLibraryApi> opt = VanillaModelLibraryApi.get();
// 或者 Bukkit.getServicesManager().load(VanillaModelLibraryApi.class)

VanillaModelLibraryApi api = opt.orElseThrow();
if (api.ready()) {
    Optional<ModelSelection> selection = api.select(entity);
    if (selection.isPresent()) {
        ModelRecord record = selection.get().model();
        ModelDisplay display = record.display();     // scale / offset / yawOffset
        Optional<ItemStack> item = api.createItem(record.modelId());
    }
}
```

- `select(Entity)`：按实时完整外观匹配；水平行走、装备、隐身、头俯仰、当前没有对应 modelId
  的变种/颜色等都会返回空，不做基础模型降级。竖直重力不再被误判成“移动”。
- `selectForDisplay(Entity)`：供“隐藏原版、整体模型展示”的调用方使用；保留年龄、变种、颜色、
  剪毛、jeb、装备、隐身、站姿等身份条件，但把瞬时的 moving 与头部角度归一化。
- `observe(Entity)`：返回完整 `Appearance` 快照，便于诊断或自定义选择。
- `select(EntityType, Appearance)`：调用方自己提供快照，便于测试或先冻结外观。
- 条件为合取；**未知条件键一律不匹配**。匹配数为 0 或大于 1 都返回空，不做猜测。
- 支持的条件键：`age/pose/equipment/invisible/upsideDown/moving/headPitchDegrees/headYawDegrees/
  variant/color/sheared/jeb/angry/armor/baseColor/collar/decor/heldItem/markings/pattern/
  patternColor/saddle/shape/size/tame`（外加兼容旧包的 `aggressive/converting`）。
- `supported=false` 的记录永远不匹配，也不会返回 ITEM。
- `models()` / `model(id)` 暴露原始 catalog 条件与 display 数值。

## 命令

| 命令 | 作用 |
|---|---|
| `/vanillamobs status` | ready、资源版本、MC 版本、RPE 版本、安装路径 |
| `/vanillamobs verify` | 只重新校验包，不安装 |
| `/vanillamobs reload` | 重新扫描/校验/安装，必要时 reload RP Engine |
| `/vanillamobs items` | 每个 modelId 对应的 ITEM 状态 |
| `/vanillamobs selftest` | 生成 21 个已知形态生物，验证匹配矩阵后删除 |

权限：`vanillamobs.admin`（默认 op）。

## 配置

见 `src/main/resources/config.yml`：`assets.directory`、`assets.package`、`assets.auto-install`、
`assets.reload-rpengine`、`assets.verify-items`。

## 构建

```bash
export JAVA_HOME=/home/cody_test/jdks/jdk-25
export GRADLE_USER_HOME=$PWD/../.gradle-home
"$GRADLE" --no-daemon build
```
