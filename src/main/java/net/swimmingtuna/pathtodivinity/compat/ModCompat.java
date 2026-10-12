package net.swimmingtuna.pathtodivinity.compat;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Mod ids of every optional integration. LOTM is the only hard dependency; everything else may be
 * absent at runtime.
 *
 * <p>Awakened Bosses, Celestisynth, EEEAB's Mobs, Marium's Soulslike Weaponry, Obscure API and Terramity
 * have no 1.21.1 NeoForge release, so the code PtD had for them did not survive the port. Their boss
 * profiles are still in the datapack, gated by {@code neoforge:mod_loaded}.
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
    public static final String BORN_IN_CHAOS = "born_in_chaos_v1";
    public static final String CATACLYSM = "cataclysm";
    public static final String LEGENDARY_MONSTERS = "legendary_monsters";
    public static final String MOWZIES_MOBS = "mowziesmobs";
    public static final String MUTANT_MONSTERS = "mutantmonsters";

    private ModCompat() {
    }

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** Whether the mod owning the given registry id is loaded, e.g. {@code cataclysm:ignis}. */
    public static boolean isLoaded(ResourceLocation id) {
        return isLoaded(id.getNamespace());
    }
}
