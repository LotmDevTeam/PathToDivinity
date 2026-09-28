package net.swimmingtuna.pathtodivinity.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

/**
 * Mod ids of every optional integration. LOTM is the only hard dependency; everything else may be
 * absent at runtime.
 *
 * <p>Rules for keeping a mod optional:
 * <ul>
 *   <li>Never import an optional mod's classes from code that always runs ({@code ModEvents},
 *       {@code PTDUtil}, LOTMC mixins, ...). Refer to its entities/items by registry id instead,
 *       via {@link PTDEntities} / {@link PTDItems}.</li>
 *   <li>When a mod's own classes are genuinely needed (casting to one of its entities, reading its
 *       data accessors), put that code in a {@code *Compat} class in this package and only call it
 *       behind {@link #isLoaded} or a registry-id check that can only pass when the mod is present.</li>
 *   <li>Mixins into an optional mod are gated by {@code PTDMixinPlugin}.</li>
 * </ul>
 */
public final class ModCompat {

    public static final String AQUAMIRAE = "aquamirae";
    public static final String AWAKENED_BOSSES = "awakened_bosses";
    public static final String BORN_IN_CHAOS = "born_in_chaos_v1";
    public static final String CATACLYSM = "cataclysm";
    public static final String CELESTISYNTH = "celestisynth";
    public static final String EEEABS_MOBS = "eeeabsmobs";
    public static final String LEGENDARY_MONSTERS = "legendary_monsters";
    public static final String MOWZIES_MOBS = "mowziesmobs";
    public static final String MUTANT_MONSTERS = "mutantmonsters";
    public static final String SOULS_WEAPONRY = "soulsweapons";
    public static final String TERRAMITY = "terramity";

    private ModCompat() {
    }

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** Whether the mod owning the given registry id is loaded, e.g. {@code terramity:gob}. */
    public static boolean isLoaded(ResourceLocation id) {
        return isLoaded(id.getNamespace());
    }
}
