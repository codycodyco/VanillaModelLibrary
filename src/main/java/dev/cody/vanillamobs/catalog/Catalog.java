package dev.cody.vanillamobs.catalog;

import dev.cody.vanillamobs.api.ModelDisplay;
import dev.cody.vanillamobs.api.ModelRecord;
import dev.cody.vanillamobs.assets.AssetException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.entity.EntityType;

/**
 * Parsed schema-1 catalog. The model list is data, not code: any number of
 * records and entity types is accepted as long as every rule is unambiguous.
 */
public record Catalog(int schema, String minecraftVersion, String namespace, List<ModelRecord> models) {

    /** Every key the matcher can decide; an unknown key is refused rather than ignored. */
    public static final Set<String> KNOWN_CONDITION_KEYS = Set.of(
            "age", "pose", "equipment", "invisible", "upsideDown", "moving",
            "headPitchDegrees", "headYawDegrees", "variant", "color", "sheared", "jeb",
            "aggressive", "converting", "angry", "armor", "baseColor", "collar",
            "decor", "heldItem", "markings", "pattern", "patternColor", "saddle",
            "shape", "size", "tame", "profession", "professionLevel",
            "villagerType", "unhappy");

    public static Catalog load(Path catalogFile, String expectedMinecraft, String expectedNamespace)
            throws AssetException {
        String text;
        try {
            text = Files.readString(catalogFile, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new AssetException("cannot read catalog.json: " + error.getMessage(), error);
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException error) {
            throw new AssetException("catalog.json is not a JSON object: " + error.getMessage(), error);
        }
        int schema = integer(root, "schema");
        if (schema != 1) {
            throw new AssetException("catalog schema " + schema + " is not the supported schema 1");
        }
        String minecraft = string(root, "minecraftVersion");
        if (!expectedMinecraft.equals(minecraft)) {
            throw new AssetException("catalog is for Minecraft " + minecraft
                    + ", this server is " + expectedMinecraft);
        }
        String namespace = string(root, "namespace");
        if (!expectedNamespace.equals(namespace)) {
            throw new AssetException("catalog namespace " + namespace + " != " + expectedNamespace);
        }
        JsonArray array = root.getAsJsonArray("models");
        if (array == null || array.isEmpty()) {
            throw new AssetException("catalog has no models");
        }
        List<ModelRecord> records = new ArrayList<>();
        Set<String> modelIds = new LinkedHashSet<>();
        for (JsonElement element : array) {
            ModelRecord record = record(element.getAsJsonObject(), namespace);
            if (!modelIds.add(record.modelId())) {
                throw new AssetException("duplicate modelId " + record.modelId());
            }
            records.add(record);
        }
        checkNoAmbiguousRules(records);
        return new Catalog(schema, minecraft, namespace, List.copyOf(records));
    }

    /**
     * A rule that can match the same entity appearance as another rule makes the
     * result ambiguous. The selector refuses ambiguity at runtime; refusing it at
     * load time names both rules instead of silently disabling both.
     */
    private static void checkNoAmbiguousRules(List<ModelRecord> records) throws AssetException {
        Map<EntityType, List<ModelRecord>> byType = new LinkedHashMap<>();
        for (ModelRecord record : records) {
            if (record.supported()) {
                byType.computeIfAbsent(record.entityType(), ignored -> new ArrayList<>()).add(record);
            }
        }
        for (Map.Entry<EntityType, List<ModelRecord>> entry : byType.entrySet()) {
            List<ModelRecord> group = entry.getValue();
            for (int first = 0; first < group.size(); first++) {
                for (int second = first + 1; second < group.size(); second++) {
                    ModelRecord a = group.get(first);
                    ModelRecord b = group.get(second);
                    if (compatible(a.conditions(), b.conditions())) {
                        throw new AssetException("overlapping rules for " + entry.getKey() + ": "
                                + a.modelId() + " and " + b.modelId()
                                + " can both match the same appearance");
                    }
                }
            }
        }
    }

    private static boolean compatible(Map<String, Object> first, Map<String, Object> second) {
        for (Map.Entry<String, Object> entry : first.entrySet()) {
            if (second.containsKey(entry.getKey())
                    && !Objects.equals(entry.getValue(), second.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static ModelRecord record(JsonObject json, String namespace) throws AssetException {
        String modelId = string(json, "modelId");
        String prefix = namespace + ":";
        if (!modelId.startsWith(prefix) || modelId.length() == prefix.length()) {
            throw new AssetException("modelId outside namespace " + namespace + ": " + modelId);
        }
        String entityName = string(json, "entityType").toUpperCase(Locale.ROOT);
        EntityType type;
        try {
            type = EntityType.valueOf(entityName);
        } catch (IllegalArgumentException error) {
            throw new AssetException("unknown entity type " + entityName);
        }
        boolean supported = bool(json, "supported");
        JsonObject conditionsJson = json.getAsJsonObject("conditions");
        if (conditionsJson == null) {
            throw new AssetException(modelId + ": no conditions object");
        }
        Map<String, Object> conditions = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : conditionsJson.entrySet()) {
            if (!KNOWN_CONDITION_KEYS.contains(entry.getKey())) {
                throw new AssetException(modelId + ": unknown condition key " + entry.getKey());
            }
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                conditions.put(entry.getKey(), value.getAsDouble());
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                conditions.put(entry.getKey(), value.getAsBoolean());
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                conditions.put(entry.getKey(), value.getAsString());
            } else {
                throw new AssetException(modelId + ": condition " + entry.getKey() + " is not a scalar");
            }
        }
        JsonObject displayJson = json.getAsJsonObject("display");
        if (displayJson == null) {
            throw new AssetException(modelId + ": no display object");
        }
        ModelDisplay display = new ModelDisplay(
                number(displayJson, "scale"),
                number(displayJson, "offsetX"),
                number(displayJson, "offsetY"),
                number(displayJson, "offsetZ"),
                number(displayJson, "yawOffsetDegrees"));
        List<String> limitations = new ArrayList<>();
        JsonArray limitationArray = json.getAsJsonArray("limitations");
        if (limitationArray != null) {
            limitationArray.forEach(value -> limitations.add(value.getAsString()));
        }
        return new ModelRecord(modelId, type, supported, Map.copyOf(conditions), display,
                List.copyOf(limitations));
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

    private static float number(JsonObject json, String key) throws AssetException {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new AssetException("missing numeric field " + key);
        }
        return value.getAsFloat();
    }

    private static boolean bool(JsonObject json, String key) throws AssetException {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new AssetException("missing boolean field " + key);
        }
        return value.getAsBoolean();
    }
}
