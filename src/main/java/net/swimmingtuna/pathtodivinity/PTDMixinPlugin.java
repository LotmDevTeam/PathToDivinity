package net.swimmingtuna.pathtodivinity;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skips mixins into mods that are not installed, so every integration except LOTM is optional.
 *
 * <p>Each sub-package of {@code net.swimmingtuna.pathtodivinity.mixin} targets one mod; top-level
 * mixins are mapped individually. A mixin that is not listed here is always applied.
 *
 * <p>This runs before mods are constructed, so it must stay self-contained: only
 * {@link LoadingModList} is available at this point, not {@code ModList} or any of this mod's
 * other classes.
 */
public class PTDMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("pathtodivinity");
    private static final String MIXIN_PACKAGE = "net.swimmingtuna.pathtodivinity.mixin.";

    private static final Map<String, String> PACKAGE_TO_MOD_ID = Map.of(
            "Aquamirae", "aquamirae",
            "BornInChaos", "born_in_chaos_v1",
            "LOTMC", "lotm"
    );

    private static final Map<String, String> TOP_LEVEL_MIXIN_TO_MOD_ID = Map.of(
            "SurfaceRuleManagerMixin", "terrablender"
    );

    /** Kill-switch keys in config/ptd/pathtodivinity-mixins.toml, per mixin sub-package or top-level mixin. */
    private static final Map<String, String> TOGGLE_KEYS = Map.of(
            "Aquamirae", "aquamirae",
            "BornInChaos", "born_in_chaos",
            "SurfaceRuleManagerMixin", "terrablender_surface_rule_fix"
    );

    // "mixin -> target" pairs Mixin was told to apply, and those that did. A GameTest forces every target to
    // load and compares the two, so a mixin broken by a mod update fails the tests instead of a player's game.
    private static final Set<String> SELECTED = ConcurrentHashMap.newKeySet();
    private static final Set<String> APPLIED = ConcurrentHashMap.newKeySet();

    private final Map<String, Boolean> integrationEnabled = new HashMap<>();
    private final Set<String> disabledMixins = new HashSet<>();

    /**
     * Reads (and on first launch writes) config/ptd/pathtodivinity-mixins.toml. Only NightConfig and
     * FMLPaths are used: at this point no Forge config, nor any other class of this mod, may load.
     */
    @Override
    public void onLoad(String mixinPackage) {
        Path file = FMLPaths.CONFIGDIR.get().resolve("ptd").resolve("pathtodivinity-mixins.toml");
        Path oldFile = FMLPaths.CONFIGDIR.get().resolve("pathtodivinity-mixins.toml");
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(oldFile) && !Files.exists(file)) {
                Files.move(oldFile, file); // written there by earlier development builds
            }
        } catch (IOException e) {
            LOGGER.warn("Could not prepare {}: {}", file, e.toString());
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(file).preserveInsertionOrder().build()) {
            config.load();
            config.setComment("integrations", String.join("\n",
                    " Turn off all of Path to Divinity's mixins into one mod, e.g. to rule out or work around a conflict.",
                    " false restores that mod's own code; the matching [balance] settings then do nothing. Needs a restart."));
            for (String key : TOGGLE_KEYS.values().stream().sorted().toList()) {
                String path = "integrations." + key;
                if (!(config.get(path) instanceof Boolean)) {
                    config.set(path, true);
                }
                integrationEnabled.put(key, config.get(path));
            }
            if (!(config.get("disabled_mixins") instanceof List)) {
                config.set("disabled_mixins", new ArrayList<String>());
            }
            config.setComment("disabled_mixins", String.join("\n",
                    " Individual mixins to skip, by name relative to the mixin package, e.g. [\"Aquamirae.CaptainCorneliaMixin\"].",
                    " Names are listed in pathtodivinity.mixins.json inside the jar. Needs a restart."));
            for (Object name : config.<List<?>>get("disabled_mixins")) {
                disabledMixins.add(MIXIN_PACKAGE + name);
            }
            config.save();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not read {}; applying every mixin: {}", file.getFileName(), e.toString());
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (disabledMixins.contains(mixinClassName)) {
            LOGGER.info("Skipping mixin {}: listed in disabled_mixins", mixinClassName);
            return false;
        }
        String toggle = toggleKey(mixinClassName);
        if (toggle != null && !integrationEnabled.getOrDefault(toggle, true)) {
            LOGGER.info("Skipping mixin {}: integrations.{} is off", mixinClassName, toggle);
            return false;
        }
        String modId = requiredModId(mixinClassName);
        if (modId == null || isModPresent(modId)) {
            SELECTED.add(mixinClassName + " -> " + targetClassName);
            return true;
        }
        LOGGER.debug("Skipping mixin {}: optional mod '{}' is not installed", mixinClassName, modId);
        return false;
    }

    private static String toggleKey(String mixinClassName) {
        if (!mixinClassName.startsWith(MIXIN_PACKAGE)) {
            return null;
        }
        String relative = mixinClassName.substring(MIXIN_PACKAGE.length());
        int dot = relative.indexOf('.');
        return TOGGLE_KEYS.get(dot < 0 ? relative : relative.substring(0, dot));
    }

    private static String requiredModId(String mixinClassName) {
        if (!mixinClassName.startsWith(MIXIN_PACKAGE)) {
            return null;
        }
        String relative = mixinClassName.substring(MIXIN_PACKAGE.length());
        int dot = relative.indexOf('.');
        return dot < 0
                ? TOP_LEVEL_MIXIN_TO_MOD_ID.get(relative)
                : PACKAGE_TO_MOD_ID.get(relative.substring(0, dot));
    }

    private static boolean isModPresent(String modId) {
        LoadingModList modList = LoadingModList.get();
        // If FML can't tell us yet, apply the mixin as before rather than silently dropping it.
        return modList == null || modList.getModFileById(modId) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        APPLIED.add(mixinClassName + " -> " + targetClassName);
    }

    /** How many mixin/target pairs were selected; lets the GameTest tell "all applied" from "saw none". */
    public static int selectedMixinCount() {
        return SELECTED.size();
    }

    /** Selected mixins that have not applied (yet); only meaningful after every target class has loaded. */
    public static Set<String> unappliedMixins() {
        Set<String> missing = new HashSet<>(SELECTED);
        missing.removeAll(APPLIED);
        return missing;
    }
}
