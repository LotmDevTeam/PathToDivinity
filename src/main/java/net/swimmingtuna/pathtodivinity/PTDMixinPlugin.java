package net.swimmingtuna.pathtodivinity;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            "AwakenedBosses", "awakened_bosses",
            "BornInChaos", "born_in_chaos_v1",
            "Celestisynth", "celestisynth",
            "LOTMC", "lotm",
            "ObscureAPI", "obscure_api",
            "Terramity", "terramity"
    );

    private static final Map<String, String> TOP_LEVEL_MIXIN_TO_MOD_ID = Map.of(
            "SurfaceRuleManagerMixin", "terrablender"
    );

    /** Kill-switch keys in config/pathtodivinity-mixins.toml, per mixin sub-package or top-level mixin. */
    private static final Map<String, String> TOGGLE_KEYS = Map.of(
            "Aquamirae", "aquamirae",
            "AwakenedBosses", "awakened_bosses",
            "BornInChaos", "born_in_chaos",
            "Celestisynth", "celestisynth",
            "ObscureAPI", "obscure_api",
            "Terramity", "terramity",
            "SurfaceRuleManagerMixin", "terrablender_surface_rule_fix"
    );

    private final Map<String, Boolean> integrationEnabled = new HashMap<>();
    private final Set<String> disabledMixins = new HashSet<>();

    /**
     * Reads (and on first launch writes) config/pathtodivinity-mixins.toml. Only NightConfig and
     * FMLPaths are used: at this point no Forge config, nor any other class of this mod, may load.
     */
    @Override
    public void onLoad(String mixinPackage) {
        Path file = FMLPaths.CONFIGDIR.get().resolve("pathtodivinity-mixins.toml");
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
                    " Individual mixins to skip, by name relative to the mixin package, e.g. [\"Terramity.MurasamaMixin\"].",
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
    }
}
