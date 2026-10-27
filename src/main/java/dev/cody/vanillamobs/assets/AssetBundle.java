package dev.cody.vanillamobs.assets;

import dev.cody.vanillamobs.catalog.Catalog;
import dev.cody.vanillamobs.api.ModelRecord;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Finds and validates an external VanillaModels package. Nothing here writes
 * into RP Engine; validation and installation are deliberately separate.
 */
public final class AssetBundle {

    public static final String NAMESPACE = "vanillamobs";
    private static final long MAX_EXTRACTED_BYTES = 256L * 1024 * 1024;

    private AssetBundle() {
    }

    /**
     * @return the package directory (the one holding manifest.json), or null when
     *         no package is present at all.
     */
    public static Path locate(Path assetsDir, String configuredPackage, Logger log) throws AssetException {
        try {
            Files.createDirectories(assetsDir);
        } catch (IOException error) {
            throw new AssetException("cannot create assets directory " + assetsDir + ": " + error.getMessage(), error);
        }
        if (!configuredPackage.isBlank()) {
            Path configured = Path.of(configuredPackage);
            if (!configured.isAbsolute()) {
                configured = assetsDir.resolve(configured);
            }
            configured = configured.normalize();
            if (!Files.isDirectory(configured)) {
                throw new AssetException("configured package does not exist: " + configured);
            }
            return configured;
        }
        List<Path> zips;
        try (Stream<Path> stream = Files.list(assetsDir)) {
            zips = stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .sorted()
                    .toList();
        } catch (IOException error) {
            throw new AssetException("cannot list " + assetsDir + ": " + error.getMessage(), error);
        }
        if (zips.size() > 1) {
            throw new AssetException("more than one delivery ZIP in " + assetsDir
                    + "; set assets.package to choose one explicitly");
        }
        if (zips.size() == 1) {
            String name = zips.get(0).getFileName().toString();
            Path target = assetsDir.resolve(name.substring(0, name.length() - 4)).normalize();
            boolean extracted = Files.isDirectory(target.resolve("content").resolve(NAMESPACE))
                    || Files.isDirectory(target.resolve("dist").resolve("content").resolve(NAMESPACE));
            if (!extracted) {
                extract(zips.get(0), target, log);
            }
        }
        List<Path> roots = packageRoots(assetsDir);
        if (roots.isEmpty()) {
            return null;
        }
        if (roots.size() > 1) {
            throw new AssetException("more than one package in " + assetsDir + ": " + roots
                    + "; set assets.package to choose one explicitly");
        }
        return roots.get(0);
    }

    private static List<Path> packageRoots(Path assetsDir) throws AssetException {
        Set<Path> roots = new TreeSet<>(Comparator.comparing(Path::toString));
        if (Files.isRegularFile(assetsDir.resolve("manifest.json"))) {
            roots.add(assetsDir);
        }
        try (Stream<Path> children = Files.list(assetsDir)) {
            for (Path child : children.filter(Files::isDirectory).sorted().toList()) {
                if (Files.isRegularFile(child.resolve("manifest.json"))) {
                    roots.add(child);
                } else if (Files.isRegularFile(child.resolve("dist").resolve("manifest.json"))) {
                    roots.add(child.resolve("dist"));
                }
            }
        } catch (IOException error) {
            throw new AssetException("cannot list " + assetsDir + ": " + error.getMessage(), error);
        }
        return new ArrayList<>(roots);
    }

    private static void extract(Path zip, Path target, Logger log) throws AssetException {
        log.info("Extracting " + zip.getFileName() + " into " + target);
        long total = 0;
        try {
            Files.createDirectories(target);
            try (ZipInputStream input = new ZipInputStream(Files.newInputStream(zip))) {
                ZipEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.isEmpty() || entry.isDirectory()) {
                        continue;
                    }
                    if (name.indexOf('\\') >= 0 || name.startsWith("/") || name.contains("..")) {
                        throw new AssetException("unsafe archive member: " + name);
                    }
                    Path destination = target.resolve(name).normalize();
                    if (!destination.startsWith(target)) {
                        throw new AssetException("archive member escapes target: " + name);
                    }
                    Files.createDirectories(destination.getParent());
                    try (OutputStream output = Files.newOutputStream(destination)) {
                        total += input.transferTo(output);
                    }
                    if (total > MAX_EXTRACTED_BYTES) {
                        throw new AssetException("archive expands beyond " + MAX_EXTRACTED_BYTES + " bytes");
                    }
                }
            }
        } catch (IOException error) {
            throw new AssetException("cannot extract " + zip + ": " + error.getMessage(), error);
        }
    }

    public static ValidatedAssets validate(Path root, String expectedMinecraft, Logger log) throws AssetException {
        Path catalogFile = root.resolve("catalog.json");
        Path manifestFile = root.resolve("manifest.json");
        Path reportFile = root.resolve("report.json");
        for (Path required : List.of(catalogFile, manifestFile, reportFile)) {
            if (!Files.isRegularFile(required)) {
                throw new AssetException("missing " + required.getFileName() + " in " + root);
            }
        }
        JsonObject manifest = readObject(manifestFile, "manifest.json");
        JsonObject report = readObject(reportFile, "report.json");
        if (integer(manifest, "schema") != 1) {
            throw new AssetException("manifest schema is not 1");
        }
        if (integer(report, "schema") != 1) {
            throw new AssetException("report schema is not 1");
        }
        if (!NAMESPACE.equals(string(manifest, "namespace"))) {
            throw new AssetException("manifest namespace is not " + NAMESPACE);
        }
        String minecraft = string(manifest, "minecraftVersion");
        if (!expectedMinecraft.equals(minecraft)) {
            throw new AssetException("delivery is for Minecraft " + minecraft
                    + ", this server is " + expectedMinecraft + " (refusing to replace)");
        }
        String assetVersion = string(manifest, "assetVersion");
        String sourceClientSha1 = string(manifest, "sourceClientSha1");
        if (!sourceClientSha1.matches("[0-9a-f]{40}")) {
            throw new AssetException("manifest sourceClientSha1 is malformed");
        }
        String catalogSha256 = string(manifest, "catalogSha256");
        String actualCatalogHash = sha256(catalogFile);
        if (!actualCatalogHash.equalsIgnoreCase(catalogSha256)) {
            throw new AssetException("catalogSha256 mismatch: manifest " + catalogSha256
                    + " actual " + actualCatalogHash);
        }
        Catalog catalog = Catalog.load(catalogFile, expectedMinecraft, NAMESPACE);

        JsonArray array = manifest.getAsJsonArray("files");
        if (array == null || array.isEmpty()) {
            throw new AssetException("manifest has no files");
        }
        Map<String, ValidatedAssets.FileEntry> entries = new LinkedHashMap<>();
        String previous = "";
        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            String path = string(json, "path");
            if (!path.equals(path.replace('\\', '/')) || path.startsWith("/") || path.contains("..")) {
                throw new AssetException("unsafe manifest path: " + path);
            }
            if (!path.startsWith("content/" + NAMESPACE + "/")) {
                throw new AssetException("manifest path outside content/" + NAMESPACE + ": " + path);
            }
            if (path.compareTo(previous) <= 0) {
                throw new AssetException("manifest paths are not sorted and unique at " + path);
            }
            previous = path;
            String hash = string(json, "sha256");
            if (!hash.matches("[0-9a-f]{64}")) {
                throw new AssetException("bad sha256 for " + path);
            }
            JsonElement sizeElement = json.get("size");
            if (sizeElement == null || !sizeElement.isJsonPrimitive()
                    || !sizeElement.getAsJsonPrimitive().isNumber()) {
                throw new AssetException("bad size for " + path);
            }
            long size = sizeElement.getAsLong();
            Path file = root.resolve(path).normalize();
            if (!file.startsWith(root)) {
                throw new AssetException("manifest path escapes package: " + path);
            }
            if (!Files.isRegularFile(file)) {
                throw new AssetException("manifest file missing: " + path);
            }
            long actualSize;
            try {
                actualSize = Files.size(file);
            } catch (IOException error) {
                throw new AssetException("cannot stat " + path + ": " + error.getMessage(), error);
            }
            if (actualSize != size) {
                throw new AssetException(path + ": size " + actualSize + " != manifest " + size);
            }
            String actualHash = sha256(file);
            if (!actualHash.equalsIgnoreCase(hash)) {
                throw new AssetException(path + ": sha256 " + actualHash + " != manifest " + hash);
            }
            entries.put(path, new ValidatedAssets.FileEntry(path, actualHash, actualSize));
        }

        Path contentRoot = root.resolve("content").resolve(NAMESPACE);
        Set<String> actual = new TreeSet<>();
        try (Stream<Path> stream = Files.walk(contentRoot)) {
            stream.filter(Files::isRegularFile).forEach(file -> actual.add(root.relativize(file).toString()
                    .replace(file.getFileSystem().getSeparator(), "/")));
        } catch (IOException error) {
            throw new AssetException("cannot walk " + contentRoot + ": " + error.getMessage(), error);
        }
        Set<String> declared = new TreeSet<>(entries.keySet());
        if (!actual.equals(declared)) {
            Set<String> extra = new TreeSet<>(actual);
            extra.removeAll(declared);
            Set<String> missing = new TreeSet<>(declared);
            missing.removeAll(actual);
            throw new AssetException("install file set differs from manifest; extra=" + extra + " missing=" + missing);
        }

        Path packYml = contentRoot.resolve("pack.yml");
        if (!Files.isRegularFile(packYml)) {
            throw new AssetException("missing pack.yml");
        }
        String packText;
        try {
            packText = Files.readString(packYml, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new AssetException("cannot read pack.yml: " + error.getMessage(), error);
        }
        if (!packText.contains("bundles: [main]")) {
            throw new AssetException("pack.yml does not request the main bundle");
        }
        Path mobsYml = contentRoot.resolve("items").resolve("mobs.yml");
        if (!Files.isRegularFile(mobsYml)) {
            throw new AssetException("missing items/mobs.yml");
        }
        String registrations;
        try {
            registrations = Files.readString(mobsYml, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new AssetException("cannot read items/mobs.yml: " + error.getMessage(), error);
        }
        for (ModelRecord model : catalog.models()) {
            String local = model.localName();
            String expectedBlock = local + ":\n  material: PAPER\n  model: " + local + "\n";
            if (model.supported() && !registrations.contains(expectedBlock)) {
                throw new AssetException(local + ": supported model is not registered as PAPER");
            }
            if (!model.supported() && registeredExactly(registrations, local)) {
                throw new AssetException(local + ": unsupported model must not be registered");
            }
            Path modelFile = contentRoot.resolve("assets").resolve("models").resolve(local + ".bbmodel");
            if (model.supported() && !Files.isRegularFile(modelFile)) {
                throw new AssetException(local + ": supported model file missing");
            }
            if (!model.supported() && Files.exists(modelFile)) {
                throw new AssetException(local + ": unsupported model file must not be delivered");
            }
        }
        log.info("Validated VanillaModels " + assetVersion + " for Minecraft " + minecraft
                + " (" + entries.size() + " files)");
        return new ValidatedAssets(root, contentRoot, catalog, assetVersion, actualCatalogHash,
                Map.copyOf(entries));
    }

    private static boolean registeredExactly(String yaml, String local) {
        return Pattern.compile("(?m)^" + Pattern.quote(local) + ":$").matcher(yaml).find();
    }

    private static JsonObject readObject(Path file, String name) throws AssetException {
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException error) {
            throw new AssetException("cannot parse " + name + ": " + error.getMessage(), error);
        }
    }

    private static String string(JsonObject json, String key) throws AssetException {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new AssetException("missing string field " + key);
        }
        return value.getAsString();
    }

    private static int integer(JsonObject json, String key) throws AssetException {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new AssetException("missing integer field " + key);
        }
        return value.getAsInt();
    }

    public static String sha256(Path file) throws AssetException {
        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException error) {
            throw new AssetException("cannot hash " + file + ": " + error.getMessage(), error);
        }
    }

    static void copyAtomically(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".vml-", ".tmp");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
