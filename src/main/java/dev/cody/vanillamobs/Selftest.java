package dev.cody.vanillamobs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.cody.vanillamobs.api.ModelSelection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Slime;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** Spawns known-shape entities, selects a model for each, and removes them again. */
public final class Selftest {

    private record Case(String name, EntityType type, Consumer<Entity> setup, String expected) {
    }

    private Selftest() {
    }

    public static boolean run(VanillaModelLibraryPlugin plugin, CommandSender sender) {
        World world = Bukkit.getWorlds().get(0);
        Location base = world.getSpawnLocation().clone().add(8, 0, 8);
        int chunkX = base.getBlockX() >> 4;
        int chunkZ = base.getBlockZ() >> 4;
        world.setChunkForceLoaded(chunkX, chunkZ, true);
        world.setChunkForceLoaded(chunkX + 1, chunkZ, true);

        List<Case> cases = List.of(
                new Case("cow_temperate_adult", EntityType.COW, entity -> {
                    Cow cow = (Cow) entity;
                    cow.setAdult();
                    cow.setVariant(Cow.Variant.TEMPERATE);
                }, "vanillamobs:cow_adult"),
                new Case("pig_temperate_adult", EntityType.PIG, entity -> {
                    Pig pig = (Pig) entity;
                    pig.setAdult();
                    pig.setVariant(Pig.Variant.TEMPERATE);
                }, "vanillamobs:pig_adult"),
                new Case("sheep_white_adult", EntityType.SHEEP, entity -> {
                    Sheep sheep = (Sheep) entity;
                    sheep.setAdult();
                    sheep.setColor(DyeColor.WHITE);
                    sheep.setSheared(false);
                    sheep.setCustomName(null);
                }, "vanillamobs:sheep_adult"),
                new Case("chicken_temperate_adult", EntityType.CHICKEN, entity -> {
                    Chicken chicken = (Chicken) entity;
                    chicken.setAdult();
                    chicken.setVariant(Chicken.Variant.TEMPERATE);
                }, "vanillamobs:chicken_adult"),
                new Case("zombie_adult", EntityType.ZOMBIE, entity -> {
                    Zombie zombie = (Zombie) entity;
                    zombie.setArmsRaised(false);
                }, "vanillamobs:zombie_adult"),
                new Case("sheep_orange_adult", EntityType.SHEEP, entity -> {
                    Sheep sheep = (Sheep) entity;
                    sheep.setAdult();
                    sheep.setColor(DyeColor.ORANGE);
                    sheep.setSheared(false);
                    sheep.setCustomName(null);
                }, "vanillamobs:sheep_orange_adult"),
                new Case("cow_warm_adult", EntityType.COW, entity -> {
                    Cow cow = (Cow) entity;
                    cow.setAdult();
                    cow.setVariant(Cow.Variant.WARM);
                }, "vanillamobs:cow_warm_adult"),
                new Case("llama_white_adult", EntityType.LLAMA, entity -> {
                    Llama llama = (Llama) entity;
                    llama.setAdult();
                    llama.setColor(Llama.Color.WHITE);
                }, "vanillamobs:llama_white_adult"),
                new Case("panda_brown_adult", EntityType.PANDA, entity -> {
                    Panda panda = (Panda) entity;
                    panda.setAdult();
                    panda.setMainGene(Panda.Gene.BROWN);
                    panda.setHiddenGene(Panda.Gene.BROWN);
                }, "vanillamobs:panda_brown_adult"),
                new Case("slime_size1_adult", EntityType.SLIME, entity -> ((Slime) entity).setSize(1),
                        "vanillamobs:slime_adult"),
                new Case("tropical_fish_betty_large", EntityType.TROPICAL_FISH, entity -> {
                    TropicalFish fish = (TropicalFish) entity;
                    fish.setPattern(TropicalFish.Pattern.BETTY);
                    fish.setBodyColor(DyeColor.WHITE);
                    fish.setPatternColor(DyeColor.WHITE);
                }, "vanillamobs:tropical_fish_large_betty_white_white_adult"),
                new Case("cow_baby", EntityType.COW, entity -> {
                    Cow cow = (Cow) entity;
                    cow.setBaby();
                    cow.setVariant(Cow.Variant.TEMPERATE);
                }, "vanillamobs:cow_baby"),
                new Case("cow_cold_adult", EntityType.COW, entity -> {
                    Cow cow = (Cow) entity;
                    cow.setAdult();
                    cow.setVariant(Cow.Variant.COLD);
                }, "vanillamobs:cow_cold_adult"),
                new Case("sheep_jeb", EntityType.SHEEP, entity -> {
                    Sheep sheep = (Sheep) entity;
                    sheep.setAdult();
                    sheep.setColor(DyeColor.WHITE);
                    sheep.setSheared(false);
                    sheep.setCustomName("jeb_");
                }, null),
                new Case("sheep_sheared", EntityType.SHEEP, entity -> {
                    Sheep sheep = (Sheep) entity;
                    sheep.setAdult();
                    sheep.setColor(DyeColor.WHITE);
                    sheep.setSheared(true);
                }, "vanillamobs:sheep_adult_sheared"),
                new Case("cow_moving", EntityType.COW, entity -> {
                    Cow cow = (Cow) entity;
                    cow.setAdult();
                    cow.setVariant(Cow.Variant.TEMPERATE);
                    cow.setVelocity(new Vector(0.3, 0, 0));
                }, null),
                new Case("cow_equipped", EntityType.COW, entity -> {
                    Cow cow = (Cow) entity;
                    cow.setAdult();
                    cow.setVariant(Cow.Variant.TEMPERATE);
                    if (cow.getEquipment() != null) {
                        cow.getEquipment().setItemInMainHand(new ItemStack(Material.STICK));
                    }
                }, null),
                new Case("cow_invisible", EntityType.COW, entity -> {
                    Cow cow = (Cow) entity;
                    cow.setAdult();
                    cow.setVariant(Cow.Variant.TEMPERATE);
                    cow.setInvisible(true);
                }, null),
                new Case("panda_held_item", EntityType.PANDA, entity -> {
                    Panda panda = (Panda) entity;
                    panda.setAdult();
                    panda.setMainGene(Panda.Gene.NORMAL);
                    panda.setHiddenGene(Panda.Gene.NORMAL);
                    if (panda.getEquipment() != null) {
                        panda.getEquipment().setItemInMainHand(new ItemStack(Material.BAMBOO));
                    }
                }, null),
                new Case("llama_saddled", EntityType.LLAMA, entity -> {
                    Llama llama = (Llama) entity;
                    llama.setAdult();
                    llama.setColor(Llama.Color.WHITE);
                    llama.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                }, null),
                new Case("slime_size2", EntityType.SLIME, entity -> ((Slime) entity).setSize(2), null));

        JsonObject report = new JsonObject();
        report.addProperty("when", Instant.now().toString());
        JsonArray results = new JsonArray();
        boolean allPassed = true;
        for (Case testCase : cases) {
            Entity entity = null;
            try {
                entity = world.spawnEntity(base, testCase.type());
                entity.setRotation(0f, 0f);
                entity.setGravity(false);
                entity.setVelocity(new Vector(0, 0, 0));
                if (entity instanceof LivingEntity living) {
                    living.setBodyYaw(0f);
                }
                if (entity instanceof Mob mob) {
                    mob.setAI(false);
                }
                testCase.setup().accept(entity);

                Optional<ModelSelection> selection = plugin.select(entity);
                String actual = selection.map(ModelSelection::modelId).orElse(null);
                boolean pass = java.util.Objects.equals(testCase.expected(), actual);
                boolean itemOk = true;
                if (actual != null) {
                    itemOk = plugin.createItem(actual)
                            .map(stack -> stack.getItemMeta().hasItemModel()
                                    && actual.equals(stack.getItemMeta().getItemModel().toString()))
                            .orElse(false);
                }
                pass = pass && itemOk;
                allPassed &= pass;

                JsonObject row = new JsonObject();
                row.addProperty("name", testCase.name());
                row.addProperty("entityType", testCase.type().name());
                row.addProperty("expected", testCase.expected());
                row.addProperty("actual", actual);
                row.addProperty("itemOk", itemOk);
                row.addProperty("pass", pass);
                results.add(row);
                sender.sendMessage((pass ? "PASS " : "FAIL ") + testCase.name()
                        + " expected=" + testCase.expected() + " actual=" + actual
                        + " itemOk=" + itemOk);
                plugin.getLogger().info((pass ? "PASS " : "FAIL ") + testCase.name()
                        + " expected=" + testCase.expected() + " actual=" + actual);
            } finally {
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        report.add("cases", results);
        report.addProperty("allPassed", allPassed);
        Path out = plugin.getDataFolder().toPath().resolve("selftest.json");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
        } catch (Exception error) {
            plugin.getLogger().warning("cannot write selftest.json: " + error.getMessage());
        }
        world.setChunkForceLoaded(chunkX, chunkZ, false);
        world.setChunkForceLoaded(chunkX + 1, chunkZ, false);
        return allPassed;
    }
}
