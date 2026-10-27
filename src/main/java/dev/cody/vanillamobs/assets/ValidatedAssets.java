package dev.cody.vanillamobs.assets;

import dev.cody.vanillamobs.catalog.Catalog;
import java.nio.file.Path;
import java.util.Map;

/** A delivery that passed every receiver-side check and may be installed. */
public record ValidatedAssets(
        Path root,
        Path contentRoot,
        Catalog catalog,
        String assetVersion,
        String catalogSha256,
        Map<String, FileEntry> files) {

    public record FileEntry(String path, String sha256, long size) {

        /** Path relative to content/vanillamobs/. */
        public String relativeToContent() {
            return path.substring("content/vanillamobs/".length());
        }
    }
}
