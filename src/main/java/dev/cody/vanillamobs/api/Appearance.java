package dev.cody.vanillamobs.api;

/**
 * A full appearance snapshot. A {@code null} field means "not observed", and a
 * condition naming that field therefore cannot match. Nothing is assumed.
 */
public record Appearance(
        Age age,
        String pose,
        Boolean equipmentEmpty,
        String variant,
        String color,
        Boolean sheared,
        Boolean jeb,
        Boolean invisible,
        Boolean moving,
        Boolean upsideDown,
        Double headPitchDegrees,
        Double headYawDegrees,
        Boolean aggressive,
        Boolean converting,
        Boolean angry,
        String armor,
        String baseColor,
        String collar,
        String decor,
        String heldItem,
        String markings,
        String pattern,
        String patternColor,
        Boolean saddle,
        String shape,
        Integer size,
        Boolean tame,
        String profession,
        Integer professionLevel,
        String villagerType,
        Boolean unhappy) {

    public enum Age {
        ADULT,
        BABY,
        UNKNOWN
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Age age;
        private String pose;
        private Boolean equipmentEmpty;
        private String variant;
        private String color;
        private Boolean sheared;
        private Boolean jeb;
        private Boolean invisible;
        private Boolean moving;
        private Boolean upsideDown;
        private Double headPitchDegrees;
        private Double headYawDegrees;
        private Boolean aggressive;
        private Boolean converting;
        private Boolean angry;
        private String armor;
        private String baseColor;
        private String collar;
        private String decor;
        private String heldItem;
        private String markings;
        private String pattern;
        private String patternColor;
        private Boolean saddle;
        private String shape;
        private Integer size;
        private Boolean tame;
        private String profession;
        private Integer professionLevel;
        private String villagerType;
        private Boolean unhappy;

        public Builder age(Age value) {
            this.age = value;
            return this;
        }

        public Builder pose(String value) {
            this.pose = value;
            return this;
        }

        public Builder equipmentEmpty(Boolean value) {
            this.equipmentEmpty = value;
            return this;
        }

        public Builder variant(String value) {
            this.variant = value;
            return this;
        }

        public Builder color(String value) {
            this.color = value;
            return this;
        }

        public Builder sheared(Boolean value) {
            this.sheared = value;
            return this;
        }

        public Builder jeb(Boolean value) {
            this.jeb = value;
            return this;
        }

        public Builder invisible(Boolean value) {
            this.invisible = value;
            return this;
        }

        public Builder moving(Boolean value) {
            this.moving = value;
            return this;
        }

        public Builder upsideDown(Boolean value) {
            this.upsideDown = value;
            return this;
        }

        public Builder headPitchDegrees(Double value) {
            this.headPitchDegrees = value;
            return this;
        }

        public Builder headYawDegrees(Double value) {
            this.headYawDegrees = value;
            return this;
        }

        public Builder aggressive(Boolean value) {
            this.aggressive = value;
            return this;
        }

        public Builder converting(Boolean value) {
            this.converting = value;
            return this;
        }

        public Builder angry(Boolean value) {
            this.angry = value;
            return this;
        }

        public Builder armor(String value) {
            this.armor = value;
            return this;
        }

        public Builder baseColor(String value) {
            this.baseColor = value;
            return this;
        }

        public Builder collar(String value) {
            this.collar = value;
            return this;
        }

        public Builder decor(String value) {
            this.decor = value;
            return this;
        }

        public Builder heldItem(String value) {
            this.heldItem = value;
            return this;
        }

        public Builder markings(String value) {
            this.markings = value;
            return this;
        }

        public Builder pattern(String value) {
            this.pattern = value;
            return this;
        }

        public Builder patternColor(String value) {
            this.patternColor = value;
            return this;
        }

        public Builder saddle(Boolean value) {
            this.saddle = value;
            return this;
        }

        public Builder shape(String value) {
            this.shape = value;
            return this;
        }

        public Builder size(Integer value) {
            this.size = value;
            return this;
        }

        public Builder tame(Boolean value) {
            this.tame = value;
            return this;
        }

        public Builder profession(String value) {
            this.profession = value;
            return this;
        }

        public Builder professionLevel(Integer value) {
            this.professionLevel = value;
            return this;
        }

        public Builder villagerType(String value) {
            this.villagerType = value;
            return this;
        }

        public Builder unhappy(Boolean value) {
            this.unhappy = value;
            return this;
        }

        public Appearance build() {
            return new Appearance(age, pose, equipmentEmpty, variant, color, sheared, jeb,
                    invisible, moving, upsideDown, headPitchDegrees, headYawDegrees,
                    aggressive, converting, angry, armor, baseColor, collar, decor,
                    heldItem, markings, pattern, patternColor, saddle, shape, size, tame,
                    profession, professionLevel, villagerType, unhappy);
        }
    }
}
