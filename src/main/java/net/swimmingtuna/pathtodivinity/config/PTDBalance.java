package net.swimmingtuna.pathtodivinity.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Weapon and mob balance for the mods Path to Divinity adjusts:
 * {@code serverconfig/ptd/pathtodivinity-balance.toml} in each world, synced to clients so tooltips match.
 *
 * <p>Every number is the absolute value the weapon uses, except keys ending in {@code _multiplier},
 * which scale a value the other mod computes at runtime. Each mod's {@code enabled = false} makes all
 * of its entries return that mod's own, unmodified values. Defaults are what Path to Divinity has
 * always used; comments give the other mod's original value.
 *
 * <p>Values are safe to read anywhere: before a world is loaded (e.g. tooltips in the main menu) they
 * return their defaults.
 */
public final class PTDBalance {

    public static final String FILE_NAME = PTDServerConfig.FOLDER + "/pathtodivinity-balance.toml";

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static Group currentGroup;

    public static final Group BORN_IN_CHAOS = group("born_in_chaos", "Born in Chaos weapons and effects");
    public static final Value SOULBANE_BONUS_DAMAGE = value("soulbane_bonus_damage", 28.0, "Melee attack damage bonus");
    public static final Value SOUL_STRATIFICATION_MAX_DAMAGE_PERCENT = value("soul_stratification_max_damage_percent", 4.0,
            "Soul Stratification deals at most this percent of the victim's max health per tick (Born in Chaos: uncapped)");
    public static final Value PUMPKIN_PISTOL_HIT_MULTIPLIER = value("pumpkin_pistol_hit_multiplier", 7.0, "Multiplies player-fired Pumpkin Pistol damage");

    public static final Group AQUAMIRAE = group("aquamirae", "Aquamirae bosses");
    public static final Toggle CAPTAIN_CORNELIA_NO_LOW_HEALTH_REGEN = toggle("captain_cornelia_no_low_health_regen", true,
            "Remove Captain Cornelia's 2 HP/s regeneration below 20 HP");

    public static final Group CATACLYSM = group("cataclysm", "Cataclysm weapons fired by players");
    public static final Value TIDAL_TENTACLE_HIT_MULTIPLIER = value("tidal_tentacle_hit_multiplier", 4.0, "Multiplies Tidal Tentacle damage");
    public static final Value WITHER_HOWITZER_HIT_MULTIPLIER = value("wither_howitzer_hit_multiplier", 1.3, "Multiplies Wither Howitzer damage");
    public static final Value WITHER_HOWITZER_VOID_ASSAULT_HIT_MULTIPLIER = value("wither_howitzer_void_assault_hit_multiplier", 1.7,
            "Wither Howitzer multiplier while the shooter carries a Void Assault Shoulder Weapon");
    public static final Value WITHER_MISSILE_HIT_MULTIPLIER = value("wither_missile_hit_multiplier", 1.3, "Multiplies Wither Missile damage");
    public static final Value SANDSTORM_HIT_MULTIPLIER = value("sandstorm_hit_multiplier", 1.5, "Multiplies Sandstorm damage");
    public static final Value PHANTOM_HALBERD_HIT_MULTIPLIER = value("phantom_halberd_hit_multiplier", 1.5, "Multiplies Phantom Halberd damage");
    public static final Value WAVE_HIT_MULTIPLIER = value("wave_hit_multiplier", 1.5, "Multiplies Wave damage");
    public static final Value VOID_VORTEX_HIT_MULTIPLIER = value("void_vortex_hit_multiplier", 2.0, "Multiplies Void Vortex damage");
    public static final Value CURSED_SANDSTORM_HIT_MULTIPLIER = value("cursed_sandstorm_hit_multiplier", 1.5, "Multiplies Cursed Sandstorm damage");

    public static final ModConfigSpec SPEC = finish();

    private PTDBalance() {
    }

    private static boolean loaded() {
        return SPEC != null && SPEC.isLoaded();
    }

    private static Group group(String name, String comment) {
        if (currentGroup != null) {
            BUILDER.pop();
        }
        BUILDER.comment(comment).push(name);
        currentGroup = new Group(BUILDER.comment("false = this mod's own, unmodified values for everything below").define("enabled", true));
        return currentGroup;
    }

    private static Value value(String key, double defaultValue, String comment) {
        return new Value(currentGroup, BUILDER.comment(comment).defineInRange(key, defaultValue, 0.0, 1_000_000.0), defaultValue);
    }

    private static Toggle toggle(String key, boolean defaultValue, String comment) {
        return new Toggle(currentGroup, BUILDER.comment(comment).define(key, defaultValue), defaultValue);
    }

    private static ModConfigSpec finish() {
        BUILDER.pop();
        return BUILDER.build();
    }

    public static final class Group {
        private final ModConfigSpec.BooleanValue enabled;

        private Group(ModConfigSpec.BooleanValue enabled) {
            this.enabled = enabled;
        }

        public boolean enabled() {
            return !loaded() || enabled.get();
        }
    }

    public static final class Value {
        private final Group group;
        private final ModConfigSpec.DoubleValue value;
        private final double defaultValue;

        private Value(Group group, ModConfigSpec.DoubleValue value, double defaultValue) {
            this.group = group;
            this.value = value;
            this.defaultValue = defaultValue;
        }

        public double get() {
            return loaded() ? value.get() : defaultValue;
        }

        /** Whether this value's mod balance is on; when off, leave the other mod's behaviour alone. */
        public boolean enabled() {
            return group.enabled();
        }

        /** The configured value, or {@code original} when the mod's balance is disabled. */
        public double apply(double original) {
            return group.enabled() ? get() : original;
        }

        public float apply(float original) {
            return group.enabled() ? (float) get() : original;
        }

        public int apply(int original) {
            return group.enabled() ? (int) Math.round(get()) : original;
        }

        /** {@code original} scaled by this multiplier, or unchanged when the mod's balance is disabled. */
        public float scale(float original) {
            return group.enabled() ? (float) (original * get()) : original;
        }

        public double scale(double original) {
            return group.enabled() ? original * get() : original;
        }
    }

    public static final class Toggle {
        private final Group group;
        private final ModConfigSpec.BooleanValue value;
        private final boolean defaultValue;

        private Toggle(Group group, ModConfigSpec.BooleanValue value, boolean defaultValue) {
            this.group = group;
            this.value = value;
            this.defaultValue = defaultValue;
        }

        /** On only while both this and its mod's balance are enabled. */
        public boolean isOn() {
            return group.enabled() && (loaded() ? value.get() : defaultValue);
        }
    }
}
