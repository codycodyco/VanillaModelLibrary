package dev.cody.vanillamobs.catalog;

import dev.cody.vanillamobs.api.Appearance;
import dev.cody.vanillamobs.api.ModelRecord;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.EntityType;

/**
 * Strict, conjunctive schema-1 matcher.
 *
 * <p>Every catalog key must be understood and observed. An unknown key, a null
 * observation, or a value outside the conditioned range fails the record; the
 * selector then returns no result rather than guessing.
 */
public final class ConditionMatcher {

    private static final double ANGLE_TOLERANCE_DEGREES = 0.5;

    private ConditionMatcher() {
    }

    public static boolean matches(ModelRecord record, EntityType type, Appearance appearance) {
        if (!record.supported() || record.entityType() != type) {
            return false;
        }
        for (Map.Entry<String, Object> entry : record.conditions().entrySet()) {
            if (!matchesOne(entry.getKey(), entry.getValue(), appearance)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesOne(String key, Object expected, Appearance appearance) {
        return switch (key) {
            case "age" -> appearance.age() != null
                    && appearance.age() != Appearance.Age.UNKNOWN
                    && text(expected).equals(appearance.age().name().toLowerCase(Locale.ROOT));
            case "pose" -> appearance.pose() != null
                    && text(expected).equalsIgnoreCase(appearance.pose());
            case "equipment" -> {
                String wanted = text(expected);
                if (!"none".equals(wanted)) {
                    yield false;
                }
                yield Boolean.TRUE.equals(appearance.equipmentEmpty());
            }
            case "variant" -> textEquals(appearance.variant(), expected);
            case "color" -> textEquals(appearance.color(), expected);
            case "sheared" -> boolEquals(appearance.sheared(), expected);
            case "jeb" -> boolEquals(appearance.jeb(), expected);
            case "invisible" -> boolEquals(appearance.invisible(), expected);
            case "moving" -> boolEquals(appearance.moving(), expected);
            case "upsideDown" -> boolEquals(appearance.upsideDown(), expected);
            case "aggressive" -> boolEquals(appearance.aggressive(), expected);
            case "converting" -> boolEquals(appearance.converting(), expected);
            case "angry" -> boolEquals(appearance.angry(), expected);
            case "armor" -> textEquals(appearance.armor(), expected);
            case "baseColor" -> textEquals(appearance.baseColor(), expected);
            case "collar" -> textEquals(appearance.collar(), expected);
            case "decor" -> textEquals(appearance.decor(), expected);
            case "heldItem" -> textEquals(appearance.heldItem(), expected);
            case "markings" -> textEquals(appearance.markings(), expected);
            case "pattern" -> textEquals(appearance.pattern(), expected);
            case "patternColor" -> textEquals(appearance.patternColor(), expected);
            case "saddle" -> boolEquals(appearance.saddle(), expected);
            case "shape" -> textEquals(appearance.shape(), expected);
            case "size" -> sizeEquals(appearance.size(), expected);
            case "tame" -> boolEquals(appearance.tame(), expected);
            case "profession" -> textEquals(appearance.profession(), expected);
            case "professionLevel" -> numberEquals(appearance.professionLevel(), expected);
            case "villagerType" -> textEquals(appearance.villagerType(), expected);
            case "unhappy" -> boolEquals(appearance.unhappy(), expected);
            case "headPitchDegrees" -> angleMatches(expected, appearance.headPitchDegrees());
            case "headYawDegrees" -> angleMatches(expected, appearance.headYawDegrees());
            default -> false;
        };
    }

    private static boolean textEquals(String observed, Object expected) {
        return observed != null && text(expected).equals(observed);
    }

    private static boolean boolEquals(Boolean observed, Object expected) {
        return observed != null && observed == Boolean.parseBoolean(text(expected));
    }

    private static boolean numberEquals(Integer observed, Object expected) {
        return observed != null && expected instanceof Number number
                && observed.intValue() == number.intValue();
    }

    private static boolean sizeEquals(Integer observed, Object expected) {
        if (observed == null || !(expected instanceof Number number)) {
            return false;
        }
        return observed == number.intValue();
    }

    private static boolean angleMatches(Object expected, Double observed) {
        if (observed == null || !(expected instanceof Number number)) {
            return false;
        }
        return Math.abs(observed - number.doubleValue()) <= ANGLE_TOLERANCE_DEGREES;
    }

    private static String text(Object value) {
        return String.valueOf(value).toLowerCase(Locale.ROOT);
    }
}
