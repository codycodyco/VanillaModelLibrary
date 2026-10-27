package dev.cody.vanillamobs.assets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Installs a validated package into RP Engine's content folder, writing only
 * files whose bytes differ. The state file lives in this plugin's data folder,
 * never inside the content namespace.
 */
public final class AssetInstaller {

    public record Result(boolean changed, int written, int deleted, List<String> kept) {
    }

    private final Logger log;

    public AssetInstaller(Logger log) {
        this.log = log;
    }

    public Result install(ValidatedAssets assets, Path contentRoot, Path stateFile) throws AssetException {
        JsonObject previous = readState(stateFile);
        Set<String> previousFiles = new LinkedHashSet<>();
        if (previous != null && previous.has("files")) {
            for (var element : previous.getAsJsonArray("files")) {
                previousFiles.add(element.getAsString());
            }
        }

        int written = 0;
        int deleted = 0;
        List<String> kept = new ArrayList<>();
        for (ValidatedAssets.FileEntry entry : assets.files().values()) {
            Path destination = contentRoot.resolve(entry.relativeToContent()).normalize();
            if (!destination.startsWith(contentRoot)) {
                throw new AssetException("refusing to write outside content folder: " + entry.path());
            }
            String relative = entry.path();
            long existingSize = -1;
            try {
                if (Files.isRegularFile(destination)) {
                    existingSize = Files.size(destination);
                }
            } catch (IOException error) {
                existingSize = -1;
            }
            if (existingSize == entry.size()
                    && AssetBundle.sha256(destination).equalsIgnoreCase(entry.sha256())) {
                kept.add(relative);
                continue;
            }
            try {
                AssetBundle.copyAtomically(assets.root().resolve(entry.path()), destination);
            } catch (IOException error) {
                throw new AssetException("cannot install " + relative + ": " + error.getMessage(), error);
            }
            written++;
        }

        Set<String> current = new LinkedHashSet<>(assets.files().keySet());
        for (String old : previousFiles) {
            if (current.contains(old)) {
                continue;
            }
            if (!old.startsWith("content/vanillamobs/") || old.contains("..")) {
                continue;
            }
            Path destination = contentRoot.resolve(stateRelativePath(old)).normalize();
            if (!destination.startsWith(contentRoot)) {
                continue;
            }
            try {
                if (Files.deleteIfExists(destination)) {
                    deleted++;
                }
            } catch (IOException error) {
                log.warning("cannot remove stale file " + destination + ": " + error.getMessage());
            }
        }

        boolean changed = written > 0 || deleted > 0;
        writeState(stateFile, assets, current);
        if (changed) {
            log.info("Installed VanillaModels " + assets.assetVersion() + ": " + written
                    + " written, " + deleted + " removed, " + kept.size() + " unchanged");
        } else {
            log.info("VanillaModels " + assets.assetVersion() + " already installed; nothing to copy");
        }
        return new Result(changed, written, deleted, kept);
    }

    private static String stateRelativePath(String manifestPath) {
        return manifestPath.substring("content/vanillamobs/".length());
    }

    private JsonObject readState(Path stateFile) throws AssetException {
        if (!Files.isRegularFile(stateFile)) {
            return null;
        }
        try {
            return JsonParser.parseString(Files.readString(stateFile, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException error) {
            log.warning("Ignoring unreadable install state " + stateFile + ": " + error.getMessage());
            return null;
        }
    }

    private void writeState(Path stateFile, ValidatedAssets assets, Set<String> files) {
        JsonObject state = new JsonObject();
        state.addProperty("assetVersion", assets.assetVersion());
        state.addProperty("catalogSha256", assets.catalogSha256());
        state.addProperty("minecraftVersion", assets.catalog().minecraftVersion());
        state.addProperty("installedAt", java.time.Instant.now().toString());
        JsonArray array = new JsonArray();
        files.forEach(array::add);
        state.add("files", array);
        try {
            Files.createDirectories(stateFile.getParent());
            Files.writeString(stateFile, state.toString(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            log.warning("cannot write install state " + stateFile + ": " + error.getMessage());
        }
    }
}
