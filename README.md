# VanillaModelLibrary

**A Paper plugin that provides 3D models of vanilla Minecraft mobs for other plugins to use.**

VanillaModelLibrary installs a collection of **static mob models** into [RP Engine](https://github.com/resourcepack-ai/rp-engine) and exposes an API to find the right model for a mob.

For example, a dancing-mobs plugin can use VanillaModelLibrary to get a mob's model, then handle the animation itself.

> **Note:** This is a model library, **not an animation plugin**. Installing it won't make mobs dance on its own.

## Features

- **Ready-to-use models:** Includes a model pack for Minecraft 26.2 (asset version 0.3.4). No separate model download is needed for a fresh installation.
- **RP Engine integration:** Installs model assets automatically and updates them when needed.
- **Appearance matching:** Finds models based on a mob's type and appearance, such as its age or color.
- **Developer API:** Lets other plugins look up models and create their corresponding RP Engine items.

Not every vanilla mob or appearance is supported yet.

## Installation

**Requirements:** Paper 26.2, Java 25, and [RP Engine](https://github.com/resourcepack-ai/rp-engine). The plugin is currently built against RP Engine 0.2.20.

1. Install RP Engine on your server.
2. Place `VanillaModelLibrary.jar` in the `plugins/` folder.
3. Restart the server. The bundled models will be installed automatically.
4. Run `/vml status` in-game or from the console. **`ready=true`** means the library is ready to use.

By default, model packages are stored in `plugins/VanillaModelLibrary/assets/`, while the installed RP Engine content goes to `plugins/RPEngine/content/vanillamobs/`.

**Using your own model pack:** Place a compatible ZIP or extracted package in the `assets/` folder. If you have multiple packages, set `assets.package` in `config.yml` to select one, then run `/vml reload`.

## Commands

| Command | Description |
| --- | --- |
| `/vml status` | Check the library and installed model pack |
| `/vml reload` | Reload the model pack and update RP Engine if needed |
| `/vml verify` | Check the model pack without installing it |
| `/vml items` | List model IDs and their RP Engine item status |
| `/vml selftest` | Spawn temporary test mobs, check model matching, and remove them |

`/vml` is an alias for `/vanillamobs`. Commands require OP by default (permission: `vanillamobs.admin`).

## For developers

Use the Java API to find a model that matches a mob:

```java
import dev.cody.vanillamobs.api.VanillaModelLibraryApi;

// 'entity' is a Bukkit Entity.
VanillaModelLibraryApi.get().ifPresent(api -> {
    if (!api.ready()) return;

    api.selectForDisplay(entity).ifPresent(selection -> {
        api.createItem(selection.modelId()).ifPresent(item -> {
            // Use this item in your display or animation code.
        });
    });
});
```

- `selectForDisplay(entity)` — Find a model for displaying the whole mob, such as in a dance animation.
- `select(entity)` — Match the mob's current appearance more strictly.
- `models()` / `model(id)` — Browse available model records.
- `observe(entity)` — Read the mob's current appearance.

If there is no suitable model, selection returns an empty result instead of guessing.

## Building from source

Requires **JDK 25** and **Gradle**:

```bash
gradle build
```

The resulting JAR will be in `build/libs/`.
