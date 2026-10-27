package dev.cody.vanillamobs.catalog;

import dev.cody.vanillamobs.api.Appearance;
import java.util.Locale;
import org.bukkit.Keyed;
import org.bukkit.entity.AbstractCubeMob;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Frog;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Nautilus;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.entity.ZombieNautilus;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/** Observes the full appearance once; the matcher never looks at a live entity. */
public final class EntityAppearance {

    private EntityAppearance() {
    }

    public static Appearance of(Entity entity) {
        Appearance.Builder builder = Appearance.builder()
                .pose(entity.getPose() == Pose.STANDING
                        ? "standing"
                        : entity.getPose().name().toLowerCase(Locale.ROOT))
                .invisible(entity.isInvisible())
                // Gravity leaves a downward velocity on a perfectly still mob;
                // only horizontal locomotion counts as "moving".
                .moving(horizontallyMoving(entity))
                // Bukkit exposes no upside-down state for these mobs; the baked
                // catalogue only ever describes the upright default.
                .upsideDown(Boolean.FALSE)
                .age(entity instanceof Ageable ageable
                        ? (ageable.isAdult() ? Appearance.Age.ADULT : Appearance.Age.BABY)
                        // A mob with no baby form is an adult by definition.
                        : Appearance.Age.ADULT)
                .variant(entity.getType().getKey().toString());

        if (entity instanceof LivingEntity living) {
            builder.equipmentEmpty(equipmentEmpty(living.getEquipment()));
            // getYaw() is the head yaw, getBodyYaw() the body: the difference is
            // the only head turn the baked snapshot can reproduce (zero).
            builder.headPitchDegrees((double) entity.getPitch());
            builder.headYawDegrees(wrapDegrees(entity.getYaw() - living.getBodyYaw()));
            builder.heldItem(itemKey(living.getEquipment() == null
                    ? null
                    : living.getEquipment().getItemInMainHand()));
        }

        switch (entity) {
            case Cow cow -> builder.variant(key(cow.getVariant()));
            case Pig pig -> builder.variant(key(pig.getVariant())).saddle(pig.hasSaddle());
            case Chicken chicken -> builder.variant(key(chicken.getVariant()));
            case Villager villager -> builder
                    .variant(key(villager.getVillagerType()))
                    .profession(profession(villager.getProfession()))
                    .professionLevel(villager.getVillagerLevel())
                    .villagerType(villager.getVillagerType().getKey().getKey())
                    .unhappy(false);
            case ZombieVillager zombieVillager -> builder
                    .variant(key(zombieVillager.getVillagerType()))
                    .profession(profession(zombieVillager.getVillagerProfession()))
                    .professionLevel(1)
                    .villagerType(zombieVillager.getVillagerType().getKey().getKey())
                    .unhappy(false);
            case Frog frog -> builder.variant(key(frog.getVariant()));
            case Sheep sheep -> builder
                    .variant("minecraft:sheep")
                    .color(sheep.getColor().name().toLowerCase(Locale.ROOT))
                    .sheared(sheep.isSheared())
                    .jeb("jeb_".equals(entity.getCustomName()));
            case Llama llama -> {
                Llama.Color color = llama.getColor();
                builder.variant("minecraft:" + color.name().toLowerCase(Locale.ROOT))
                        .color(color.name().toLowerCase(Locale.ROOT))
                        .saddle(!empty(llama.getInventory().getSaddle()))
                        .decor(itemKey(llama.getInventory().getDecor()));
            }
            case Camel camel -> builder.saddle(!empty(camel.getInventory().getSaddle()));
            case Nautilus nautilus -> builder
                    .variant("minecraft:temperate")
                    .armor(itemKey(nautilus.getInventory().getArmor()))
                    .saddle(!empty(nautilus.getInventory().getSaddle()));
            case ZombieNautilus zombieNautilus -> builder
                    .variant(key(zombieNautilus.getVariant()))
                    .armor(itemKey(zombieNautilus.getInventory().getArmor()))
                    .saddle(!empty(zombieNautilus.getInventory().getSaddle()));
            case Panda panda -> builder
                    .variant("minecraft:" + panda.getCombinedGene().name().toLowerCase(Locale.ROOT))
                    .heldItem(itemKey(entity instanceof LivingEntity living
                            && living.getEquipment() != null
                            ? living.getEquipment().getItemInMainHand()
                            : null));
            case Rabbit rabbit -> builder.variant(rabbitVariant(rabbit.getRabbitType()));
            case TropicalFish fish -> builder
                    .pattern(fish.getPattern().name().toLowerCase(Locale.ROOT))
                    .baseColor(fish.getBodyColor().name().toLowerCase(Locale.ROOT))
                    .patternColor(fish.getPatternColor().name().toLowerCase(Locale.ROOT))
                    .shape(fishShape(fish.getPattern()));
            case AbstractCubeMob cube -> builder.size(cube.getSize());
            default -> {
                // A fixed variant is already the registry key; no further field
                // is conditioned by the catalogue for this entity.
            }
        }
        return builder.build();
    }

    /**
     * The catalog snapshot is a standing pose with a straight head. A whole-model
     * display hides the original and replaces it, so transient head angles and
     * locomotion are not identity: normalize them, keep every stable trait.
     */
    public static Appearance forWholeModelDisplay(Appearance observed) {
        return new Appearance(observed.age(), observed.pose(), observed.equipmentEmpty(),
                observed.variant(), observed.color(), observed.sheared(), observed.jeb(),
                observed.invisible(), Boolean.FALSE, observed.upsideDown(),
                0.0, 0.0, observed.aggressive(), observed.converting(),
                observed.angry(), observed.armor(), observed.baseColor(), observed.collar(),
                observed.decor(), observed.heldItem(), observed.markings(), observed.pattern(),
                observed.patternColor(), observed.saddle(), observed.shape(), observed.size(),
                observed.tame(), observed.profession(), observed.professionLevel(),
                observed.villagerType(), observed.unhappy());
    }

    private static String key(Keyed keyed) {
        return keyed.getKey().toString();
    }

    private static String profession(Villager.Profession profession) {
        return profession == null ? "none" : profession.getKey().getKey();
    }

    private static String rabbitVariant(Rabbit.Type type) {
        return switch (type) {
            case BROWN -> "minecraft:brown";
            case WHITE -> "minecraft:white";
            case BLACK -> "minecraft:black";
            case BLACK_AND_WHITE -> "minecraft:white_splotched";
            case GOLD -> "minecraft:gold";
            case SALT_AND_PEPPER -> "minecraft:salt";
            case THE_KILLER_BUNNY -> "minecraft:killer";
        };
    }

    private static String fishShape(TropicalFish.Pattern pattern) {
        return switch (pattern) {
            case FLOPPER, STRIPEY, GLITTER, BLOCKFISH, BETTY, CLAYFISH -> "large";
            default -> "small";
        };
    }

    private static String itemKey(ItemStack stack) {
        return empty(stack) ? "none" : stack.getType().getKey().toString();
    }

    private static boolean horizontallyMoving(Entity entity) {
        var velocity = entity.getVelocity();
        return velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ() > 1.0E-4;
    }

    private static boolean equipmentEmpty(EntityEquipment equipment) {
        if (equipment == null) {
            return true;
        }
        return empty(equipment.getItemInMainHand())
                && empty(equipment.getItemInOffHand())
                && empty(equipment.getHelmet())
                && empty(equipment.getChestplate())
                && empty(equipment.getLeggings())
                && empty(equipment.getBoots());
    }

    private static boolean empty(ItemStack stack) {
        return stack == null || stack.getType().isAir() || stack.getAmount() <= 0;
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) {
            wrapped -= 360.0;
        }
        if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }
}
