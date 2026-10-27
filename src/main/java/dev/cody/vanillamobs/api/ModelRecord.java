package dev.cody.vanillamobs.api;

import java.util.List;
import java.util.Map;
import org.bukkit.entity.EntityType;

/**
 * One catalog record. {@code conditions} is the raw, conjunctive schema-1 map:
 * a selector that does not understand every key in it must not match.
 */
public record ModelRecord(
        String modelId,
        EntityType entityType,
        boolean supported,
        Map<String, Object> conditions,
        ModelDisplay display,
        List<String> limitations) {

    public String localName() {
        int colon = modelId.indexOf(':');
        return colon < 0 ? modelId : modelId.substring(colon + 1);
    }

    public String namespace() {
        int colon = modelId.indexOf(':');
        return colon < 0 ? "" : modelId.substring(0, colon);
    }
}
