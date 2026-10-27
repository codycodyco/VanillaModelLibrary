package dev.cody.vanillamobs.rpe;

import ai.resourcepack.engine.RPEnginePlugin;
import ai.resourcepack.engine.api.ContentId;
import ai.resourcepack.engine.api.ContentKind;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

/** The only place that talks to RP Engine; everything else uses stable API types. */
public final class RpeBridge {

    private final RPEnginePlugin engine;

    public RpeBridge(RPEnginePlugin engine) {
        this.engine = engine;
    }

    public boolean isRegisteredItem(String modelId) {
        return ContentId.parse(modelId)
                .map(id -> engine.registry().contains(id, ContentKind.ITEM))
                .orElse(false);
    }

    public Optional<ItemStack> createItem(String modelId) {
        return ContentId.parse(modelId).flatMap(engine.items()::create);
    }

    public void reload() {
        engine.reload();
    }

    public String version() {
        return engine.getPluginMeta().getVersion();
    }
}
