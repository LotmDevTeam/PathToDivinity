package net.swimmingtuna.pathtodivinity.config;

import net.minecraftforge.common.ForgeConfigSpec;

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

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static Group currentGroup;

    // ---- Terramity ----
    public static final Group TERRAMITY = group("terramity", "Terramity weapons");
    public static final Value ADVANCED_AUTOMATIC_RIFLE_DAMAGE = value("advanced_automatic_rifle_damage", 0.315, "Bullet damage (Terramity: 0.45)");
    public static final Value ADVANCED_BURST_RIFLE_DAMAGE = value("advanced_burst_rifle_damage", 0.315, "Bullet damage (Terramity: 0.45)");
    public static final Value ADVANCED_PISTOL_DAMAGE = value("advanced_pistol_damage", 0.75, "Bullet damage (Terramity: 1.25)");
    public static final Value ANTIMATTER_RIFLE_DAMAGE = value("antimatter_rifle_damage", 3.495, "Bullet damage (Terramity: 2.33)");
    public static final Value ASPHODEL_DAMAGE = value("asphodel_damage", 2.0, "Projectile damage (Terramity: 0.8)");
    public static final Value AXE_OF_UNHOLY_DIVINITY_BONUS_DAMAGE = value("axe_of_unholy_divinity_bonus_damage", 48.0, "Melee attack damage bonus");
    public static final Value AXE_OF_UNHOLY_DIVINITY_EFFECT_TICKS = value("axe_of_unholy_divinity_effect_ticks", 140, "Duration of its right-click effect, in ticks (Terramity: 80)");
    public static final Value BLASPHEMIC_RAPTURE_DAMAGE = value("blasphemic_rapture_damage", 9.0, "Projectile damage (Terramity: 2.25)");
    public static final Value BLUNDERBUSS_DAMAGE = value("blunderbuss_damage", 0.6, "Pellet damage (Terramity: 0.3)");
    public static final Value CELESTIAL_SIXTY_DAMAGE = value("celestial_sixty_damage", 0.75, "Bullet damage (Terramity: 1.5)");
    public static final Value CONDUCTITE_LASER_RIFLE_DAMAGE = value("conductite_laser_rifle_damage", 2.52, "Laser damage (Terramity: 1.2)");
    public static final Value CONDUCTITE_SWORD_BONUS_DAMAGE = value("conductite_sword_bonus_damage", 30.0, "Melee attack damage bonus");
    public static final Value COSMIC_STORM_DAMAGE = value("cosmic_storm_damage", 20.0, "Projectile damage (Terramity: 25)");
    public static final Value COSMILITE_SWORD_BONUS_DAMAGE = value("cosmilite_sword_bonus_damage", 6.0, "Melee attack damage bonus");
    public static final Value CRESCENT_MOONBLADE_BONUS_DAMAGE = value("crescent_moonblade_bonus_damage", 12.0, "Melee attack damage bonus");
    public static final Value CRESCENT_MOONBLADE_PROJECTILE_DAMAGE = value("crescent_moonblade_projectile_damage", 18.0, "Projectile damage (Terramity: 10)");
    public static final Value DAVY_JONES_DAMAGE = value("davy_jones_damage", 3.055, "Projectile damage (Terramity: 2.35)");
    public static final Value DIVINE_INTERVENTION_DAMAGE = value("divine_intervention_damage", 2.72, "Projectile damage (Terramity: 1.7)");
    public static final Value DUSKROK_GREATSWORD_BONUS_DAMAGE = value("duskrok_greatsword_bonus_damage", 9.0, "Melee attack damage bonus");
    public static final Value EXODIUM_SWORD_BONUS_DAMAGE = value("exodium_sword_bonus_damage", 34.0, "Melee attack damage bonus");
    public static final Value EXODIUM_WARAXE_BONUS_DAMAGE = value("exodium_waraxe_bonus_damage", 48.0, "Melee attack damage bonus");
    public static final Value FIVE_THOUSAND_MAGNUM_DAMAGE = value("five_thousand_magnum_damage", 7.5, "Bullet damage (Terramity: 2.5)");
    public static final Value FLINTLOCK_PISTOL_DAMAGE = value("flintlock_pistol_damage", 3.9, "Bullet damage (Terramity: 2.6)");
    public static final Value FORTUNES_FAVOR_DAMAGE_MULTIPLIER = value("fortunes_favor_damage_multiplier", 4.0, "Multiplies the bullet damage Terramity computes (Terramity: 1)");
    public static final Value GAIAS_TEMPEST_DAMAGE = value("gaias_tempest_damage", 0.01, "Projectile damage (Terramity: 0.05)");
    public static final Value GOBS_CLAYMORE_BONUS_DAMAGE = value("gobs_claymore_bonus_damage", 30.0, "Melee attack damage bonus");
    public static final Value GUIDING_MOONLIGHT_BONUS_DAMAGE = value("guiding_moonlight_bonus_damage", 13.0, "Melee attack damage bonus");
    public static final Value GUIDING_MOONLIGHT_PROJECTILE_DAMAGE = value("guiding_moonlight_projectile_damage", 24.0, "Projectile damage (Terramity: 15)");
    public static final Value HELLFIRE_FLURRY_DAMAGE = value("hellfire_flurry_damage", 0.6, "Projectile damage (Terramity: 0.4)");
    public static final Value HELLROK_GIGATON_HAMMER_BONUS_DAMAGE = value("hellrok_gigaton_hammer_bonus_damage", 14.0, "Melee attack damage bonus");
    public static final Value HELLSPEC_SWORD_BONUS_DAMAGE = value("hellspec_sword_bonus_damage", 30.0, "Melee attack damage bonus");
    public static final Value HEROS_SWORD_BONUS_DAMAGE = value("heros_sword_bonus_damage", 31.0, "Melee attack damage bonus");
    public static final Value HEROS_SWORD_PROJECTILE_DAMAGE = value("heros_sword_projectile_damage", 120.0, "Projectile damage (Terramity: 15)");
    public static final Value ICEBRAND_BONUS_DAMAGE = value("icebrand_bonus_damage", 9.0, "Melee attack damage bonus");
    public static final Value IRIDIUM_SWORD_BONUS_DAMAGE = value("iridium_sword_bonus_damage", 7.0, "Melee attack damage bonus");
    public static final Value METEOR_CANNON_DAMAGE = value("meteor_cannon_damage", 5.2, "Projectile damage (Terramity: 4)");
    public static final Value MURASAMA_BONUS_DAMAGE = value("murasama_bonus_damage", 36.0, "Melee attack damage bonus");
    public static final Value MURASAMA_PROC_DAMAGE = value("murasama_proc_damage", 120.0, "Damage of its delayed slash (Terramity: 30)");
    public static final Value NIHILITY_DAMAGE = value("nihility_damage", 0.4675, "Projectile damage (Terramity: 0.425)");
    public static final Value NYXIUM_GREATSWORD_BONUS_DAMAGE = value("nyxium_greatsword_bonus_damage", 35.5, "Melee attack damage bonus");
    public static final Value OLYMPUS_DAMAGE = value("olympus_damage", 30.0, "Projectile damage (Terramity: 20)");
    public static final Value ONYX_STORM_DAMAGE = value("onyx_storm_damage", 12.0, "Projectile damage (Terramity: 20)");
    public static final Value PLANET_BUSTER_DAMAGE = value("planet_buster_damage", 210.0, "Laser damage (Terramity: 70)");
    public static final Value RAILGUN_DAMAGE = value("railgun_damage", 60.0, "Laser damage (Terramity: 40)");
    public static final Value REVERIUM_SWORD_BONUS_DAMAGE = value("reverium_sword_bonus_damage", 34.0, "Melee attack damage bonus");
    public static final Value SIMMEREDGE_BONUS_DAMAGE = value("simmeredge_bonus_damage", 6.0, "Melee attack damage bonus");
    public static final Value STAIRWAY_TO_HEAVEN_DAMAGE = value("stairway_to_heaven_damage", 0.8, "Projectile damage (Terramity: 0.4)");
    public static final Value SUPPRESSED_ADVANCED_PISTOL_DAMAGE = value("suppressed_advanced_pistol_damage", 0.69, "Bullet damage (Terramity: 1.15)");
    public static final Value SWORD_OF_THE_IMPRISONED_BONUS_DAMAGE = value("sword_of_the_imprisoned_bonus_damage", 51.0, "Melee attack damage bonus");
    public static final Value SWORD_OF_THE_IMPRISONED_PROJECTILE_DAMAGE = value("sword_of_the_imprisoned_projectile_damage", 110.0, "Projectile damage (Terramity: 22)");
    public static final Value TITANOMACHY_DAMAGE = value("titanomachy_damage", 1.1, "Projectile damage (Terramity: 0.55)");
    public static final Value UNHOLY_LANCE_ATTACK_DAMAGE = value("unholy_lance_attack_damage", 24.0, "Attack damage attribute (Terramity: 15)");
    public static final Value VOID_SWORD_BONUS_DAMAGE = value("void_sword_bonus_damage", 30.0, "Melee attack damage bonus");
    public static final Value VULCAN_DAMAGE = value("vulcan_damage", 1.05, "Projectile damage (Terramity: 0.35)");

    // ---- Celestisynth ----
    public static final Group CELESTISYNTH = group("celestisynth", "Celestisynth weapons. Tier stats (attack damage, durability) are in config/ptd/pathtodivinity-startup.toml.");
    public static final Value AQUAFLORA_BLAST_OFF_DAMAGE = value("aquaflora_blast_off_damage", 11.0, "Damage factor (Celestisynth: 1.3)");
    public static final Value AQUAFLORA_FLOWERS_AWAY_DAMAGE = value("aquaflora_flowers_away_damage", 3.5, "Damage factor (Celestisynth: 0.5)");
    public static final Value AQUAFLORA_PETAL_PIERCES_DAMAGE = value("aquaflora_petal_pierces_damage", 0.275, "Damage factor (Celestisynth: 0.1)");
    public static final Value AQUAFLORA_SLASH_FRENZY_SINGLE_WIELD_DAMAGE = value("aquaflora_slash_frenzy_single_wield_damage", 2.0, "Damage factor when not dual-wielding (Celestisynth: 0.8)");
    public static final Value BREEZEBREAKER_DUAL_GALESTORM_DAMAGE = value("breezebreaker_dual_galestorm_damage", 3.05, "Damage factor (Celestisynth: 1.3)");
    public static final Value BREEZEBREAKER_GALESTORM_DAMAGE = value("breezebreaker_galestorm_damage", 4.0, "Damage factor (Celestisynth: 1.3)");
    public static final Value BREEZEBREAKER_WHEEL_DAMAGE = value("breezebreaker_wheel_damage", 5.2, "Damage factor (Celestisynth: 1.4)");
    public static final Value BREEZEBREAKER_WIND_ROAR_DAMAGE = value("breezebreaker_wind_roar_damage", 0.5, "Damage factor (Celestisynth: 0.075)");
    public static final Value BREEZEBREAKER_TORNADO_DAMAGE_MULTIPLIER = value("breezebreaker_tornado_damage_multiplier", 5.5, "Multiplies the tornado's damage (Celestisynth: 1)");
    public static final Toggle BREEZEBREAKER_FALL_IMMUNITY = toggle("breezebreaker_fall_immunity", true,
            "Holding Breezebreaker makes you immune to fall damage, replacing its own when-hurt passive");
    public static final Value CRESCENTIA_BARRAGE_DAMAGE = value("crescentia_barrage_damage", 0.25, "Damage factor (Celestisynth: 0.07)");
    public static final Value KERES_REND_MAX_HEALTH_DAMAGE = value("keres_rend_max_health_damage", 0.008,
            "Each hit deals base damage + target max health x base damage x this (Celestisynth: 0.015)");
    public static final Value KERES_SLASH_WAVE_SLASH_DAMAGE_MULTIPLIER = value("keres_slash_wave_slash_damage_multiplier", 0.6, "Multiplies each slash's damage (Celestisynth: 1)");
    public static final Value KERES_SLASH_WAVE_SHADOW_DAMAGE = value("keres_slash_wave_shadow_damage", 1.0, "Shadow damage factor (Celestisynth: 2)");
    public static final Value KERES_SMASH_FIRST_PULSE = value("keres_smash_first_pulse", 2.5, "Strength of the first pulse (Celestisynth: 1)");
    public static final Value KERES_SMASH_SECOND_PULSE = value("keres_smash_second_pulse", 2.0, "Strength of the second pulse (Celestisynth: 0.8)");
    public static final Value KERES_SMASH_THIRD_PULSE = value("keres_smash_third_pulse", 1.5, "Strength of the third pulse (Celestisynth: 0.6)");
    public static final Value KERES_SMASH_HIT_MULTIPLIER = value("keres_smash_hit_multiplier", 0.4, "Multiplies each pulse's damage to targets (Celestisynth: 1)");
    public static final Value KERES_SMASH_HEAL_DIVISOR = value("keres_smash_heal_divisor", 8.0, "Caster heals pulse damage divided by this (Celestisynth: 4)");
    public static final Value FROSTBOUND_ICE_CAST_DAMAGE_MULTIPLIER = value("frostbound_ice_cast_damage_multiplier", 5.5, "Multiplies the ice cast's damage (Celestisynth: 1)");
    public static final Value POLTERGEIST_BARRIER_CALL_DAMAGE = value("poltergeist_barrier_call_damage", 6.0, "Damage factor (Celestisynth: 0.8)");
    public static final Value POLTERGEIST_COSMIC_STEEL_DAMAGE_MULTIPLIER = value("poltergeist_cosmic_steel_damage_multiplier", 5.5, "Multiplies the smash damage (Celestisynth: 1)");
    public static final Value POLTERGEIST_WARD_DAMAGE = value("poltergeist_ward_damage", 6.0, "Damage of each ward hit");
    public static final Value RAINFALL_SERENITY_ARROW_DAMAGE_MULTIPLIER = value("rainfall_serenity_arrow_damage_multiplier", 0.15, "Multiplies its arrows' base damage (Celestisynth: 1)");
    public static final Value SOLARIS_FULL_ROUND_DAMAGE = value("solaris_full_round_damage", 0.46, "Damage factor (Celestisynth: 0.23)");
    public static final Value SOLARIS_SOUL_DASH_DAMAGE = value("solaris_soul_dash_damage", 0.396, "Damage factor (Celestisynth: 0.18)");
    public static final Value SOLARIS_BOMB_HIT_MULTIPLIER = value("solaris_bomb_hit_multiplier", 5.0, "Multiplies damage from Solaris bombs");
    public static final Value CRESCENTIA_DRAGON_HIT_MULTIPLIER = value("crescentia_dragon_hit_multiplier", 2.0, "Multiplies damage from Crescentia dragons");
    public static final Value FROSTBOUND_SHARD_HIT_MULTIPLIER = value("frostbound_shard_hit_multiplier", 6.5, "Multiplies damage from Frostbound shards");

    // ---- Other mods ----
    public static final Group AWAKENED_BOSSES = group("awakened_bosses", "Awakened Bosses weapons");
    public static final Value HEROBRINE_AXE_BONUS_DAMAGE = value("herobrine_axe_bonus_damage", 15.0, "Melee attack damage bonus");
    public static final Value HEROBRINE_SWORD_BONUS_DAMAGE = value("herobrine_sword_bonus_damage", 13.0, "Melee attack damage bonus");

    public static final Group BORN_IN_CHAOS = group("born_in_chaos", "Born in Chaos weapons and effects");
    public static final Value SOULBANE_BONUS_DAMAGE = value("soulbane_bonus_damage", 28.0, "Melee attack damage bonus");
    public static final Value SOUL_STRATIFICATION_MAX_DAMAGE_PERCENT = value("soul_stratification_max_damage_percent", 4.0,
            "Soul Stratification deals at most this percent of the victim's max health per tick (Born in Chaos: uncapped)");
    public static final Value PUMPKIN_PISTOL_HIT_MULTIPLIER = value("pumpkin_pistol_hit_multiplier", 7.0, "Multiplies player-fired Pumpkin Pistol damage");

    public static final Group AQUAMIRAE = group("aquamirae", "Aquamirae bosses");
    public static final Toggle CAPTAIN_CORNELIA_NO_LOW_HEALTH_REGEN = toggle("captain_cornelia_no_low_health_regen", true,
            "Remove Captain Cornelia's 10 HP/s regeneration below 16 HP");

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

    public static final Group SOULS_WEAPONRY = group("soulsweapons", "Marium's Soulslike Weaponry");
    public static final Value FREYR_SWORD_HIT_MULTIPLIER = value("freyr_sword_hit_multiplier", 1.8, "Multiplies Freyr Sword damage");

    public static final Group EEEABS_MOBS = group("eeeabsmobs", "EEEAB's Mobs");
    public static final Value GUARDIAN_LASER_MOB_HIT_MULTIPLIER = value("guardian_laser_mob_hit_multiplier", 0.6,
            "Multiplies the Nameless Guardian's laser damage when a mob fires it");

    public static final Group OBSCURE_API = group("obscure_api", "Obscure API");
    public static final Toggle DISABLE_CRIT_AND_MAGIC_RESISTANCE = toggle("disable_crit_and_magic_resistance", true,
            "Turn off Obscure API's critical hit and magic resistance attributes, which stack badly with LOTM damage");

    public static final ForgeConfigSpec SPEC = finish();

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

    private static ForgeConfigSpec finish() {
        BUILDER.pop();
        return BUILDER.build();
    }

    public static final class Group {
        private final ForgeConfigSpec.BooleanValue enabled;

        private Group(ForgeConfigSpec.BooleanValue enabled) {
            this.enabled = enabled;
        }

        public boolean enabled() {
            return !loaded() || enabled.get();
        }
    }

    public static final class Value {
        private final Group group;
        private final ForgeConfigSpec.DoubleValue value;
        private final double defaultValue;

        private Value(Group group, ForgeConfigSpec.DoubleValue value, double defaultValue) {
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
        private final ForgeConfigSpec.BooleanValue value;
        private final boolean defaultValue;

        private Toggle(Group group, ForgeConfigSpec.BooleanValue value, boolean defaultValue) {
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
