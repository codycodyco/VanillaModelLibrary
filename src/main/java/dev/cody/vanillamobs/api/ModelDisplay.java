package dev.cody.vanillamobs.api;

/** The display transform recorded in catalog.json, before any entity yaw is added. */
public record ModelDisplay(float scale, float offsetX, float offsetY, float offsetZ, float yawOffsetDegrees) {
}
