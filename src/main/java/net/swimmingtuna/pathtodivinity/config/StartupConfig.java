package net.swimmingtuna.pathtodivinity.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.swimmingtuna.pathtodivinity.PTD;

import java.nio.file.Path;

/**
 * {@code config/pathtodivinity-startup.toml}: values needed while other mods register their items,
 * before Forge loads any config. Read once, directly from the file; changes need a restart.
 */
public final class StartupConfig {

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("pathtodivinity-startup.toml");

    public static final boolean CELESTISYNTH_TIER_ENABLED;
    public static final int CELESTISYNTH_TIER_LEVEL;
    public static final int CELESTISYNTH_TIER_USES;
    public static final float CELESTISYNTH_TIER_SPEED;
    public static final float CELESTISYNTH_TIER_ATTACK_DAMAGE_BONUS;
    public static final int CELESTISYNTH_TIER_ENCHANTMENT_VALUE;

    static {
        CommentedFileConfig config = CommentedFileConfig.builder(FILE).preserveInsertionOrder().build();
        try {
            config.load();
        } catch (RuntimeException e) {
            PTD.LOGGER.warn("Could not read {}, using defaults: {}", FILE.getFileName(), e.toString());
        }
        String section = "celestisynth_tier.";
        config.setComment("celestisynth_tier", " The item tier shared by Celestisynth's weapons (needs a restart).");
        CELESTISYNTH_TIER_ENABLED = define(config, section + "enabled", true, " false = Celestisynth's own tier");
        CELESTISYNTH_TIER_LEVEL = define(config, section + "level", 5, " Mining level");
        CELESTISYNTH_TIER_USES = define(config, section + "uses", 2550, " Durability");
        CELESTISYNTH_TIER_SPEED = define(config, section + "speed", 9.0, " Mining speed").floatValue();
        CELESTISYNTH_TIER_ATTACK_DAMAGE_BONUS = define(config, section + "attack_damage_bonus", 12.0, " Added to every Celestisynth weapon's attack damage").floatValue();
        CELESTISYNTH_TIER_ENCHANTMENT_VALUE = define(config, section + "enchantment_value", 15, " Enchantability");
        try {
            config.save();
        } catch (RuntimeException e) {
            PTD.LOGGER.warn("Could not write {}: {}", FILE.getFileName(), e.toString());
        }
        config.close();
    }

    private StartupConfig() {
    }

    @SuppressWarnings("unchecked")
    private static <T> T define(CommentedFileConfig config, String path, T defaultValue, String comment) {
        Object value = config.get(path);
        if (value == null || value.getClass() != defaultValue.getClass() && !(value instanceof Number && defaultValue instanceof Number)) {
            config.set(path, defaultValue);
            value = defaultValue;
        }
        config.setComment(path, comment);
        if (defaultValue instanceof Integer && value instanceof Number number) {
            return (T) Integer.valueOf(number.intValue());
        }
        if (defaultValue instanceof Double && value instanceof Number number) {
            return (T) Double.valueOf(number.doubleValue());
        }
        return (T) value;
    }
}
