package net.swimmingtuna.pathtodivinity;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

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

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String modId = requiredModId(mixinClassName);
        if (modId == null || isModPresent(modId)) {
            return true;
        }
        LOGGER.debug("Skipping mixin {}: optional mod '{}' is not installed", mixinClassName, modId);
        return false;
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
