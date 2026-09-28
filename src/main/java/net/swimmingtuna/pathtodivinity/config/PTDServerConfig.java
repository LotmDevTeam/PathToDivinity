package net.swimmingtuna.pathtodivinity.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.swimmingtuna.pathtodivinity.PTD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server rules: {@code serverconfig/ptd/pathtodivinity-server.toml} in each world, synced to clients.
 * Modpacks can ship defaults for new worlds in {@code defaultconfigs/ptd/pathtodivinity-server.toml}.
 *
 * <p>Values are only readable while a server is running. Every default equals the behaviour this
 * mod had before the options existed.
 */
public final class PTDServerConfig {

    /** Every config file Path to Divinity creates lives in a {@code ptd} folder of its config directory. */
    public static final String FOLDER = "ptd";
    public static final String FILE_NAME = FOLDER + "/pathtodivinity-server.toml";

    public static final ForgeConfigSpec SPEC;

    // [combat]
    public static final ForgeConfigSpec.BooleanValue COMBAT_LOCKS_COMMANDS;
    public static final ForgeConfigSpec.IntValue COMBAT_DURATION_TICKS;
    public static final ForgeConfigSpec.BooleanValue COMBAT_TAG_ATTACKER;
    public static final ForgeConfigSpec.BooleanValue COMBAT_TAG_ON_ENVIRONMENTAL_DAMAGE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COMBAT_BLOCKED_COMMANDS;
    public static final ForgeConfigSpec.ConfigValue<String> COMBAT_MESSAGE;

    // [commands]
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCKED_COMMAND_PREFIXES;
    public static final ForgeConfigSpec.ConfigValue<String> BLOCKED_COMMAND_MESSAGE;

    // [items]
    public static final ForgeConfigSpec.IntValue ITEM_SCAN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BANNED_ENCHANTMENTS;
    public static final ForgeConfigSpec.EnumValue<BannedItemAction> BANNED_ITEM_ACTION;

    // [bosses]
    public static final ForgeConfigSpec.BooleanValue BOSS_REGEN_ENABLED;
    public static final ForgeConfigSpec.DoubleValue BOSS_REGEN_PERCENT;
    public static final ForgeConfigSpec.IntValue BOSS_REGEN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue BOSS_BLOCK_BREAKING_INTERVAL_TICKS;
    public static final ForgeConfigSpec.BooleanValue BOSS_BLOCK_BREAKING_DROPS;
    public static final ForgeConfigSpec.BooleanValue BOSS_TARGET_LOCK;
    public static final ForgeConfigSpec.DoubleValue BOSS_PROJECTILE_DAMAGE_MULTIPLIER;

    // [scaling]
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;

    // [world]
    public static final ForgeConfigSpec.BooleanValue DISABLE_KRAMPUS;
    public static final ForgeConfigSpec.IntValue BEYONDER_REFRESH_INTERVAL_TICKS;

    public enum BannedItemAction {
        DELETE,
        DROP
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Combat tag: damage puts players (and mobs) \"in combat\" for a short time.",
                "The timer also decides when bosses may regenerate, so it runs even if command locking is off.").push("combat");
        COMBAT_LOCKS_COMMANDS = builder
                .comment("Block the commands listed in blocked_commands while a player is in combat.")
                .define("enabled", true);
        COMBAT_DURATION_TICKS = builder
                .comment("How long the combat tag lasts after taking or dealing damage, in ticks (20 = 1 second).")
                .defineInRange("duration_ticks", 200, 1, 72000);
        COMBAT_TAG_ATTACKER = builder
                .comment("Also tag whoever dealt the damage (the shooter, for projectiles).")
                .define("tag_attacker", true);
        COMBAT_TAG_ON_ENVIRONMENTAL_DAMAGE = builder
                .comment("Tag on damage with no attacker, such as falling, fire or drowning.")
                .define("tag_on_environmental_damage", true);
        COMBAT_BLOCKED_COMMANDS = builder
                .comment("Commands players cannot use while in combat, without the leading slash.",
                        "Namespaced forms are matched too: \"home\" also blocks \"essentials:home\".")
                .defineListAllowEmpty(List.of("blocked_commands"), () -> List.of("home", "spawn"), PTDServerConfig::isString);
        COMBAT_MESSAGE = builder
                .comment("Shown when a command is blocked. %s is replaced by the command name.")
                .define("message", "You are in combat and cannot use /%s!");
        builder.pop();

        builder.comment("Commands blocked for every player at all times.").push("commands");
        BLOCKED_COMMAND_PREFIXES = builder
                .comment("A command is blocked when it starts with one of these (lower case, no leading slash).",
                        "The defaults stop FTB Teams parties, which would share Sequence quest progress.")
                .defineListAllowEmpty(List.of("blocked_prefixes"),
                        () -> List.of("ftbteams party create", "ftbteams party join", "ftbteams party invite"),
                        PTDServerConfig::isString);
        BLOCKED_COMMAND_MESSAGE = builder
                .comment("Shown when one of blocked_prefixes is used.")
                .define("blocked_message", "Teams are disabled on this pack — each player must progress their own pathway.");
        builder.pop();

        builder.comment("Item rules, checked on each player every scan_interval_ticks.").push("items");
        ITEM_SCAN_INTERVAL_TICKS = builder
                .comment("How often players' items are checked, in ticks.")
                .defineInRange("scan_interval_ticks", 200, 1, 72000);
        BANNED_ENCHANTMENTS = builder
                .comment("Enchantments removed from the item in a player's main hand.")
                .defineListAllowEmpty(List.of("banned_enchantments"), () -> List.of("minecraft:piercing"), PTDServerConfig::isString);
        BANNED_ITEM_ACTION = builder
                .comment("What happens to a banned item a player carries: DELETE removes it, DROP drops it at their feet.",
                        "Banned items on mobs are always deleted.")
                .defineEnum("banned_item_action", BannedItemAction.DELETE);
        builder.pop();

        builder.comment("Behaviour of Beyonder bosses.",
                "Whether bosses break blocks to reach players follows LOTM's \"Mobs Destroy Blocks\" option in lotm-common.toml,",
                "and they never break blocks harder than LOTM allows or inside protected faction claims.").push("bosses");
        BOSS_REGEN_ENABLED = builder
                .comment("Bosses with no target and no combat tag slowly regenerate health.")
                .define("regen_enabled", true);
        BOSS_REGEN_PERCENT = builder
                .comment("Percent of max health healed every regen_interval_ticks.")
                .defineInRange("regen_percent", 2.0, 0.0, 100.0);
        BOSS_REGEN_INTERVAL_TICKS = builder
                .comment("Ticks between regeneration steps.")
                .defineInRange("regen_interval_ticks", 10, 1, 72000);
        BOSS_BLOCK_BREAKING_INTERVAL_TICKS = builder
                .comment("Ticks between block-breaking checks when a boss's target is above it.")
                .defineInRange("block_breaking_interval_ticks", 10, 1, 72000);
        BOSS_BLOCK_BREAKING_DROPS = builder
                .comment("Blocks broken by bosses drop as items.")
                .define("block_breaking_drops", true);
        BOSS_TARGET_LOCK = builder
                .comment("Bosses fighting a player won't switch to a healthier target.")
                .define("target_lock", true);
        BOSS_PROJECTILE_DAMAGE_MULTIPLIER = builder
                .comment("Multiplier for damage dealt by bosses' projectiles.")
                .defineInRange("projectile_damage_multiplier", 0.6, 0.0, 100.0);
        builder.pop();

        builder.comment("Global multipliers applied on top of each boss's own health and damage multipliers.").push("scaling");
        HEALTH_MULTIPLIER = builder
                .comment("1.0 = unchanged, 0.5 = half, 2.5 = two and a half times.")
                .defineInRange("health_multiplier", 0.85, 0.0, 10.0);
        DAMAGE_MULTIPLIER = builder
                .comment("1.0 = unchanged, 0.5 = half, 2.5 = two and a half times.")
                .defineInRange("damage_multiplier", 0.85, 0.0, 10.0);
        builder.pop();

        builder.push("world");
        DISABLE_KRAMPUS = builder
                .comment("Stop Born in Chaos's Krampus and his henchmen from spawning.")
                .define("disable_krampus", true);
        BEYONDER_REFRESH_INTERVAL_TICKS = builder
                .comment("Ticks between re-registering bosses with LOTM (/beyonderentity) and re-rolling the Ultra Sniffer's pathway.",
                        "0 = only once, at server start.")
                .defineInRange("beyonder_refresh_interval_ticks", 1200, 0, 1728000);
        builder.pop();

        SPEC = builder.build();
    }

    private PTDServerConfig() {
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }

    // ---- Migration from the old COMMON config ----

    private static final Path LEGACY_COMMON_FILE = FMLPaths.CONFIGDIR.get().resolve(FOLDER).resolve("pathtodivinity-common.toml");

    /**
     * Released versions wrote config/pathtodivinity-common.toml. It is kept (each world copies its
     * multipliers once, see {@link #onConfigLoading}) but moved into config/ptd/ with the other files.
     */
    public static void relocateLegacyCommonFile() {
        Path old = FMLPaths.CONFIGDIR.get().resolve("pathtodivinity-common.toml");
        try {
            if (Files.exists(old) && !Files.exists(LEGACY_COMMON_FILE)) {
                Files.createDirectories(LEGACY_COMMON_FILE.getParent());
                Files.move(old, LEGACY_COMMON_FILE);
            }
        } catch (IOException e) {
            PTD.LOGGER.warn("Could not move {} into config/{}/: {}", old.getFileName(), FOLDER, e.toString());
        }
    }

    /**
     * Health/damage multipliers used to live in {@code config/ptd/pathtodivinity-common.toml}. The first
     * time a world's server config loads, copy them over, then leave a marker beside the server config
     * so later edits to it are never overwritten. The legacy file is left alone so every world migrates.
     */
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        ModConfig config = event.getConfig();
        if (config.getSpec() != SPEC || ServerLifecycleHooks.getCurrentServer() == null) {
            return; // not ours, or a synced copy on a remote client (no file to migrate into)
        }
        try {
            Path marker = config.getFullPath().resolveSibling("pathtodivinity-server.migrated");
            if (Files.exists(marker)) {
                return;
            }
            if (Files.exists(LEGACY_COMMON_FILE)) {
                try (CommentedFileConfig legacy = CommentedFileConfig.of(LEGACY_COMMON_FILE)) {
                    legacy.load();
                    Number damage = legacy.get(List.of("PTD Configs", "Damage Multiplier"));
                    Number health = legacy.get(List.of("PTD Configs", "Health Multiplier"));
                    if (damage != null) {
                        DAMAGE_MULTIPLIER.set(damage.doubleValue());
                    }
                    if (health != null) {
                        HEALTH_MULTIPLIER.set(health.doubleValue());
                    }
                }
                config.save();
                PTD.LOGGER.info("Copied health/damage multipliers from {} into this world's {}",
                        LEGACY_COMMON_FILE.getFileName(), config.getFileName());
            }
            Files.createFile(marker);
        } catch (IOException | RuntimeException e) {
            PTD.LOGGER.warn("Could not migrate legacy multipliers into {}: {}", config.getFileName(), e.toString());
        }
    }
}
