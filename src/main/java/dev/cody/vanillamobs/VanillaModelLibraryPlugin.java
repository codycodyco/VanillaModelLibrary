package dev.cody.vanillamobs;

import ai.resourcepack.engine.RPEnginePlugin;
import dev.cody.vanillamobs.api.Appearance;
import dev.cody.vanillamobs.api.ModelRecord;
import dev.cody.vanillamobs.api.ModelSelection;
import dev.cody.vanillamobs.api.VanillaModelLibraryApi;
import dev.cody.vanillamobs.assets.AssetBundle;
import dev.cody.vanillamobs.assets.AssetException;
import dev.cody.vanillamobs.assets.AssetInstaller;
import dev.cody.vanillamobs.assets.ValidatedAssets;
import dev.cody.vanillamobs.catalog.Catalog;
import dev.cody.vanillamobs.catalog.ConditionMatcher;
import dev.cody.vanillamobs.catalog.EntityAppearance;
import dev.cody.vanillamobs.rpe.RpeBridge;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class VanillaModelLibraryPlugin extends JavaPlugin implements VanillaModelLibraryApi {

    private volatile boolean ready;
    private volatile String lastError = "not initialised";
    private volatile Catalog catalog;
    private volatile ValidatedAssets assets;
    private volatile String serverMinecraftVersion = "unknown";
    private RpeBridge rpe;
    private Path assetsDirectory;
    private Path stateFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        serverMinecraftVersion = Bukkit.getMinecraftVersion();
        RPEnginePlugin engine = (RPEnginePlugin) Bukkit.getPluginManager().getPlugin("RPEngine");
        if (engine == null) {
            lastError = "RP Engine is not installed";
            getLogger().severe(lastError);
            return;
        }
        rpe = new RpeBridge(engine);
        assetsDirectory = getDataFolder().toPath()
                .resolve(getConfig().getString("assets.directory", "assets"));
        stateFile = getDataFolder().toPath().resolve("installed-state.json");
        ensureBundledAssets();

        getServer().getServicesManager().register(VanillaModelLibraryApi.class, this, this,
                ServicePriority.Normal);
        VanillaMobsCommand command = new VanillaMobsCommand(this);
        if (getCommand("vanillamobs") != null) {
            getCommand("vanillamobs").setExecutor(command);
            getCommand("vanillamobs").setTabCompleter(command);
        }
        reloadLibrary();
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        ready = false;
    }

    /** Validates and (when enabled) installs the package, reloading RP Engine only on change. */
    public synchronized void reloadLibrary() {
        ready = false;
        try {
            String configured = getConfig().getString("assets.package", "");
            Path root = AssetBundle.locate(assetsDirectory, configured, getLogger());
            if (root == null) {
                lastError = "no VanillaModels package in " + assetsDirectory;
                getLogger().warning(lastError + " - drop the delivery ZIP or its dist/ folder there");
                return;
            }
            ValidatedAssets validated = AssetBundle.validate(root, serverMinecraftVersion, getLogger());
            Path contentRoot = Bukkit.getPluginManager().getPlugin("RPEngine").getDataFolder().toPath()
                    .resolve("content").resolve(AssetBundle.NAMESPACE);
            if (getConfig().getBoolean("assets.auto-install", true)) {
                AssetInstaller.Result result = new AssetInstaller(getLogger())
                        .install(validated, contentRoot, stateFile);
                if (result.changed() && getConfig().getBoolean("assets.reload-rpengine", true)) {
                    getLogger().info("Content changed; reloading RP Engine");
                    rpe.reload();
                }
            }
            assets = validated;
            catalog = validated.catalog();
            boolean verified = verifyItems();
            if (!verified && getConfig().getBoolean("assets.reload-rpengine", true)) {
                getLogger().info("Items not visible yet; asking RP Engine to reload once");
                rpe.reload();
                verified = verifyItems();
            }
            if (!verified && getConfig().getBoolean("assets.verify-items", true)) {
                lastError = "package is valid but one or more ITEMs are absent from RP Engine";
                getLogger().severe(lastError);
                return;
            }
            ready = true;
            lastError = "ok";
            getLogger().info("VanillaModels " + assetVersion() + " ready for Minecraft "
                    + minecraftVersion() + " (" + catalog.models().stream()
                    .filter(ModelRecord::supported).count() + " supported models)");
        } catch (AssetException error) {
            lastError = error.getMessage();
            ready = false;
            getLogger().severe("Refusing to install VanillaModels: " + error.getMessage());
        } catch (RuntimeException error) {
            lastError = error.getClass().getSimpleName() + ": " + error.getMessage();
            ready = false;
            getLogger().severe("VanillaModels setup failed: " + lastError);
        }
    }

    /**
     * The delivery ZIP is bundled so a fresh install needs no manual asset copy.
     * Any existing file or directory in assets/ is treated as server-owner
     * content and is left alone, so external packages still take precedence.
     */
    private void ensureBundledAssets() {
        try {
            Files.createDirectories(assetsDirectory);
            try (var entries = Files.list(assetsDirectory)) {
                if (entries.findAny().isPresent()) {
                    return;
                }
            }
            saveResource("assets/VanillaModels-26.2-0.3.4.zip", false);
            getLogger().info("Released bundled VanillaModels 0.3.4 delivery into " + assetsDirectory);
        } catch (IOException error) {
            getLogger().warning("Cannot prepare bundled VanillaModels assets: " + error.getMessage());
        } catch (IllegalArgumentException error) {
            getLogger().warning("Bundled VanillaModels delivery is missing from the JAR");
        }
    }

    private boolean verifyItems() {
        if (catalog == null) {
            return false;
        }
        if (!getConfig().getBoolean("assets.verify-items", true)) {
            return true;
        }
        boolean ok = true;
        for (ModelRecord model : catalog.models()) {
            if (!model.supported()) {
                continue;
            }
            if (!rpe.isRegisteredItem(model.modelId())) {
                getLogger().severe("RP Engine does not expose ITEM " + model.modelId());
                ok = false;
            } else if (rpe.createItem(model.modelId()).isEmpty()) {
                getLogger().severe("RP Engine could not create " + model.modelId());
                ok = false;
            }
        }
        return ok;
    }

    @Override
    public boolean ready() {
        return ready;
    }

    @Override
    public String assetVersion() {
        ValidatedAssets current = assets;
        return current == null ? "" : current.assetVersion();
    }

    @Override
    public String minecraftVersion() {
        ValidatedAssets current = assets;
        return current == null ? serverMinecraftVersion : current.catalog().minecraftVersion();
    }

    @Override
    public List<ModelRecord> models() {
        Catalog current = catalog;
        return current == null ? List.of() : current.models();
    }

    @Override
    public Optional<ModelRecord> model(String modelId) {
        return models().stream().filter(model -> model.modelId().equals(modelId)).findFirst();
    }

    @Override
    public Optional<ModelSelection> select(Entity entity) {
        if (!ready) {
            return Optional.empty();
        }
        return select(entity.getType(), EntityAppearance.of(entity));
    }

    @Override
    public Optional<ModelSelection> select(EntityType type, Appearance appearance) {
        Catalog current = catalog;
        if (!ready || current == null) {
            return Optional.empty();
        }
        List<ModelSelection> matches = current.models().stream()
                .filter(model -> ConditionMatcher.matches(model, type, appearance))
                .map(ModelSelection::new)
                .toList();
        // Ambiguity is not a guess: both zero and two matches return no result.
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    @Override
    public Optional<ModelSelection> selectForDisplay(Entity entity) {
        if (!ready) {
            return Optional.empty();
        }
        return select(entity.getType(), EntityAppearance.forWholeModelDisplay(EntityAppearance.of(entity)));
    }

    @Override
    public Appearance observe(Entity entity) {
        return EntityAppearance.of(entity);
    }

    @Override
    public Optional<String> modelId(Entity entity) {
        return select(entity).map(ModelSelection::modelId);
    }

    @Override
    public Optional<ItemStack> createItem(String modelId) {
        if (rpe == null || !ready) {
            return Optional.empty();
        }
        return model(modelId).filter(ModelRecord::supported).flatMap(model -> rpe.createItem(modelId));
    }

    public String lastError() {
        return lastError;
    }

    public ValidatedAssets assets() {
        return assets;
    }

    public String rpeVersion() {
        return rpe == null ? "absent" : rpe.version();
    }

    public Path assetsDirectory() {
        return assetsDirectory;
    }

    public Path contentRoot() {
        return Bukkit.getPluginManager().getPlugin("RPEngine").getDataFolder().toPath()
                .resolve("content").resolve(AssetBundle.NAMESPACE);
    }
}
