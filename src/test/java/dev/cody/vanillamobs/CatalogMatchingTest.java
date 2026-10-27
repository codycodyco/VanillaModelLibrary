package dev.cody.vanillamobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.cody.vanillamobs.api.Appearance;
import dev.cody.vanillamobs.api.ModelDisplay;
import dev.cody.vanillamobs.api.ModelRecord;
import dev.cody.vanillamobs.assets.AssetException;
import dev.cody.vanillamobs.catalog.Catalog;
import dev.cody.vanillamobs.catalog.ConditionMatcher;
import dev.cody.vanillamobs.catalog.EntityAppearance;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

class CatalogMatchingTest {

    private static Catalog catalog() throws Exception {
        Path file = Files.createTempFile("catalog", ".json");
        try (var input = CatalogMatchingTest.class.getResourceAsStream("/catalog.json")) {
            Files.write(file, input.readAllBytes());
        }
        return Catalog.load(file, "26.2", "vanillamobs");
    }

    private static Appearance.Builder standing(EntityType type) {
        Appearance.Builder builder = Appearance.builder()
                .age(Appearance.Age.ADULT)
                .pose("standing")
                .equipmentEmpty(true)
                .variant(type.getKey().toString())
                .invisible(false)
                .moving(false)
                .upsideDown(false)
                .headPitchDegrees(0.0)
                .headYawDegrees(0.0)
                .heldItem("none");
        return switch (type) {
            case COW, PIG, CHICKEN, FROG -> builder.variant("minecraft:temperate").saddle(false);
            case SHEEP -> builder.variant("minecraft:sheep")
                    .color("white")
                    .sheared(false)
                    .jeb(false);
            case LLAMA -> builder.variant("minecraft:creamy")
                    .color("creamy")
                    .saddle(false)
                    .decor("none");
            case PANDA -> builder.variant("minecraft:normal");
            case RABBIT -> builder.variant("minecraft:brown");
            case TROPICAL_FISH -> builder.variant("minecraft:tropical_fish")
                    .pattern("betty")
                    .baseColor("white")
                    .patternColor("white")
                    .shape("large");
            case SLIME -> builder.variant("minecraft:slime").size(1);
            default -> builder;
        };
    }

    private static ModelRecord record(Catalog catalog, String id) {
        return catalog.models().stream().filter(m -> m.modelId().equals(id)).findFirst().orElseThrow();
    }

    private static boolean matches(Catalog catalog, String id, EntityType type, Appearance appearance) {
        return ConditionMatcher.matches(record(catalog, id), type, appearance);
    }

    @Test
    void catalogIsSchemaOneWithTheFullDelivery() throws Exception {
        Catalog catalog = catalog();
        assertEquals(1, catalog.schema());
        assertEquals("26.2", catalog.minecraftVersion());
        assertEquals(181, catalog.models().size());
        assertEquals(136, catalog.models().stream().filter(ModelRecord::supported).count());
        assertTrue(record(catalog, "vanillamobs:zombie_adult").supported());
        assertFalse(record(catalog, "vanillamobs:allay_adult").supported());
    }

    @Test
    void cowPigSheepChickenAndZombieMatchTheirDefaultAppearance() throws Exception {
        Catalog catalog = catalog();
        assertTrue(matches(catalog, "vanillamobs:cow_adult", EntityType.COW,
                standing(EntityType.COW).build()));
        assertTrue(matches(catalog, "vanillamobs:pig_adult", EntityType.PIG,
                standing(EntityType.PIG).build()));
        assertTrue(matches(catalog, "vanillamobs:sheep_adult", EntityType.SHEEP,
                standing(EntityType.SHEEP).build()));
        assertTrue(matches(catalog, "vanillamobs:chicken_adult", EntityType.CHICKEN,
                standing(EntityType.CHICKEN).build()));
        assertTrue(matches(catalog, "vanillamobs:zombie_adult", EntityType.ZOMBIE,
                standing(EntityType.ZOMBIE).build()));
    }

    @Test
    void villagerProfessionTypeAndLevelMatch() throws Exception {
        Catalog catalog = catalog();
        Appearance farmer = standing(EntityType.VILLAGER)
                .variant("minecraft:plains")
                .profession("farmer")
                .professionLevel(1)
                .villagerType("plains")
                .unhappy(false)
                .build();
        assertTrue(matches(catalog, "vanillamobs:villager_plains_farmer_level_1_adult",
                EntityType.VILLAGER, farmer));
        Appearance librarian = standing(EntityType.VILLAGER)
                .variant("minecraft:plains")
                .profession("librarian")
                .professionLevel(1)
                .villagerType("plains")
                .unhappy(false)
                .build();
        assertFalse(matches(catalog, "vanillamobs:villager_plains_farmer_level_1_adult",
                EntityType.VILLAGER, librarian));
    }

    @Test
    void climateAndColourVariantsSelectTheirOwnRecords() throws Exception {
        Catalog catalog = catalog();
        assertTrue(matches(catalog, "vanillamobs:cow_warm_adult", EntityType.COW,
                standing(EntityType.COW).variant("minecraft:warm").build()));
        assertTrue(matches(catalog, "vanillamobs:sheep_orange_adult", EntityType.SHEEP,
                standing(EntityType.SHEEP).color("orange").build()));
        assertFalse(matches(catalog, "vanillamobs:cow_adult", EntityType.COW,
                standing(EntityType.COW).variant("minecraft:warm").build()));
        assertFalse(matches(catalog, "vanillamobs:sheep_adult", EntityType.SHEEP,
                standing(EntityType.SHEEP).color("orange").build()));
    }

    @Test
    void llamaSaddleDecorAndColourAreConjunctive() throws Exception {
        Catalog catalog = catalog();
        assertTrue(matches(catalog, "vanillamobs:llama_white_adult", EntityType.LLAMA,
                standing(EntityType.LLAMA).variant("minecraft:white").color("white").build()));
        assertFalse(matches(catalog, "vanillamobs:llama_white_adult", EntityType.LLAMA,
                standing(EntityType.LLAMA).variant("minecraft:white").color("white")
                        .saddle(true).build()));
        assertFalse(matches(catalog, "vanillamobs:llama_white_adult", EntityType.LLAMA,
                standing(EntityType.LLAMA).variant("minecraft:white").color("white")
                        .decor("minecraft:chest").build()));
    }

    @Test
    void pandaGeneAndHeldItemAreConjunctive() throws Exception {
        Catalog catalog = catalog();
        assertTrue(matches(catalog, "vanillamobs:panda_brown_adult", EntityType.PANDA,
                standing(EntityType.PANDA).variant("minecraft:brown").build()));
        assertFalse(matches(catalog, "vanillamobs:panda_brown_adult", EntityType.PANDA,
                standing(EntityType.PANDA).variant("minecraft:brown")
                        .heldItem("minecraft:bamboo").build()));
    }

    @Test
    void tropicalFishPatternShapeAndColoursAreConjunctive() throws Exception {
        Catalog catalog = catalog();
        assertTrue(matches(catalog, "vanillamobs:tropical_fish_large_betty_white_white_adult",
                EntityType.TROPICAL_FISH, standing(EntityType.TROPICAL_FISH).build()));
        assertFalse(matches(catalog, "vanillamobs:tropical_fish_large_betty_white_white_adult",
                EntityType.TROPICAL_FISH, standing(EntityType.TROPICAL_FISH)
                        .pattern("kob").build()));
        assertFalse(matches(catalog, "vanillamobs:tropical_fish_large_betty_white_white_adult",
                EntityType.TROPICAL_FISH, standing(EntityType.TROPICAL_FISH)
                        .shape("small").build()));
    }

    @Test
    void babyColdAndShearedRecordsAreReachable() throws Exception {
        Catalog catalog = catalog();
        assertTrue(matches(catalog, "vanillamobs:cow_baby", EntityType.COW,
                standing(EntityType.COW).age(Appearance.Age.BABY).build()));
        assertTrue(matches(catalog, "vanillamobs:cow_cold_adult", EntityType.COW,
                standing(EntityType.COW).variant("minecraft:cold").build()));
        assertTrue(matches(catalog, "vanillamobs:sheep_adult_sheared", EntityType.SHEEP,
                standing(EntityType.SHEEP).sheared(true).build()));
    }

    @Test
    void cubeSizeIsObserved() throws Exception {
        Catalog catalog = catalog();
        assertTrue(matches(catalog, "vanillamobs:slime_adult", EntityType.SLIME,
                standing(EntityType.SLIME).build()));
        assertFalse(matches(catalog, "vanillamobs:slime_adult", EntityType.SLIME,
                standing(EntityType.SLIME).size(2).build()));
    }

    @Test
    void everyConditionIsConjunctiveForTheOldCases() throws Exception {
        Catalog catalog = catalog();
        ModelRecord cow = record(catalog, "vanillamobs:cow_adult");
        assertFalse(ConditionMatcher.matches(cow, EntityType.COW,
                standing(EntityType.COW).age(Appearance.Age.BABY).build()));
        assertFalse(ConditionMatcher.matches(cow, EntityType.COW,
                standing(EntityType.COW).variant("minecraft:cold").build()));
        assertFalse(ConditionMatcher.matches(cow, EntityType.COW,
                standing(EntityType.COW).moving(true).build()));
        assertFalse(ConditionMatcher.matches(cow, EntityType.COW,
                standing(EntityType.COW).equipmentEmpty(false).build()));
        assertFalse(ConditionMatcher.matches(cow, EntityType.COW,
                standing(EntityType.COW).invisible(true).build()));
        assertFalse(ConditionMatcher.matches(cow, EntityType.COW,
                standing(EntityType.COW).headPitchDegrees(15.0).build()));
        assertFalse(ConditionMatcher.matches(cow, EntityType.COW,
                standing(EntityType.COW).pose("swimming").build()));
    }

    @Test
    void sheepRejectsJebShearedAndNonWhite() throws Exception {
        Catalog catalog = catalog();
        assertFalse(matches(catalog, "vanillamobs:sheep_adult", EntityType.SHEEP,
                standing(EntityType.SHEEP).jeb(true).build()));
        assertFalse(matches(catalog, "vanillamobs:sheep_adult", EntityType.SHEEP,
                standing(EntityType.SHEEP).sheared(true).build()));
        assertFalse(matches(catalog, "vanillamobs:sheep_adult", EntityType.SHEEP,
                standing(EntityType.SHEEP).color("black").build()));
    }

    @Test
    void unknownConditionAndMissingObservationFailClosed() throws Exception {
        ModelRecord odd = new ModelRecord("vanillamobs:cow_adult", EntityType.COW, true,
                Map.of("mystery", true),
                new ModelDisplay(2.5f, 0f, 0.001f, 0f, 180f), List.of());
        assertFalse(ConditionMatcher.matches(odd, EntityType.COW,
                standing(EntityType.COW).build()));
        assertFalse(matches(catalog(), "vanillamobs:cow_adult", EntityType.COW,
                Appearance.builder().build()));
    }

    @Test
    void wholeModelDisplayNormalizesTransientPoseButKeepsIdentity() throws Exception {
        Catalog catalog = catalog();
        Appearance live = standing(EntityType.SHEEP)
                .moving(true)
                .headPitchDegrees(-6.93)
                .headYawDegrees(12.0)
                .build();
        Appearance normalized = EntityAppearance.forWholeModelDisplay(live);
        assertEquals(Boolean.FALSE, normalized.moving());
        assertEquals(0.0, normalized.headPitchDegrees());
        assertEquals(0.0, normalized.headYawDegrees());
        assertEquals("white", normalized.color());
        assertTrue(matches(catalog, "vanillamobs:sheep_adult", EntityType.SHEEP, normalized));

        Appearance black = EntityAppearance.forWholeModelDisplay(
                standing(EntityType.SHEEP).color("black").build());
        assertFalse(matches(catalog, "vanillamobs:sheep_adult", EntityType.SHEEP, black));
    }

    @Test
    void duplicateModelIdIsRejected() throws Exception {
        String json = minimalCatalog("""
                {"modelId":"vanillamobs:cow_adult","entityType":"COW","supported":true,
                 "conditions":{"age":"adult"},"display":%s,"limitations":[]},
                {"modelId":"vanillamobs:cow_adult","entityType":"COW","supported":true,
                 "conditions":{"age":"baby"},"display":%s,"limitations":[]}
                """.formatted(display(), display()));
        assertThrows(AssetException.class, () -> load(json));
    }

    @Test
    void overlappingSupportedRulesAreRejected() throws Exception {
        String json = minimalCatalog("""
                {"modelId":"vanillamobs:cow_adult","entityType":"COW","supported":true,
                 "conditions":{"age":"adult"},"display":%s,"limitations":[]},
                {"modelId":"vanillamobs:cow_baby","entityType":"COW","supported":true,
                 "conditions":{"age":"adult","pose":"standing"},"display":%s,"limitations":[]}
                """.formatted(display(), display()));
        assertThrows(AssetException.class, () -> load(json));
    }

    private static String display() {
        return "{\"scale\":2.5,\"offsetX\":0,\"offsetY\":0.001,\"offsetZ\":0,\"yawOffsetDegrees\":180}";
    }

    private static String minimalCatalog(String models) {
        return """
                {"schema":1,"minecraftVersion":"26.2","namespace":"vanillamobs","models":[%s]}
                """.formatted(models);
    }

    private static Catalog load(String json) throws Exception {
        Path file = Files.createTempFile("catalog-overlap", ".json");
        Files.writeString(file, json);
        return Catalog.load(file, "26.2", "vanillamobs");
    }
}
