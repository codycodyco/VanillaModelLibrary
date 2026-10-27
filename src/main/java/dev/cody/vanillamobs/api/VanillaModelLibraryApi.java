package dev.cody.vanillamobs.api;

import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

/**
 * Stable API of VanillaModelLibrary. Obtain it with {@link #get()} or through
 * Bukkit's ServicesManager; store model IDs, never the resolved ItemStacks.
 *
 * <p>Selectors are conjunctive and fail closed: a record matches only when
 * every catalog condition is understood and observed, and exactly one record
 * matches. An unsupported record (the delivered zombie) never matches.
 */
public interface VanillaModelLibraryApi {

    static Optional<VanillaModelLibraryApi> get() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(VanillaModelLibraryApi.class));
    }

    /** True when the asset package validated, installed, and its ITEMs exist in RP Engine. */
    boolean ready();

    /** Asset package version (independent of this plugin's version). */
    String assetVersion();

    /** Minecraft version the installed package was built for. */
    String minecraftVersion();

    /** Every catalog record, in catalog order, including unsupported ones. */
    List<ModelRecord> models();

    Optional<ModelRecord> model(String modelId);

    /** Full live-appearance selection for an entity; empty when not deterministic. */
    Optional<ModelSelection> select(Entity entity);

    /** Selection from an explicit appearance snapshot, for tests and for callers that already froze one. */
    Optional<ModelSelection> select(EntityType type, Appearance appearance);

    /**
     * Selection for a client-side display that replaces the whole model while the
     * original entity is hidden (a dance prop). Identity and appearance conditions
     * still apply, but transient head angles and locomotion are normalized: they
     * describe the pose of a body nobody will see, not the model that is shown.
     */
    Optional<ModelSelection> selectForDisplay(Entity entity);

    /** The full live appearance snapshot, for diagnostics and custom selectors. */
    Appearance observe(Entity entity);

    Optional<String> modelId(Entity entity);

    /** The RP Engine ITEM for a supported model ID, created through the engine's own API. */
    Optional<ItemStack> createItem(String modelId);
}
