package net.swimmingtuna.pathtodivinity.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Locale;

/**
 * Registry ids of entities from optional mods. Comparing ids instead of {@code SomeModEntities.X.get()}
 * means none of those mods' classes are loaded just to check what an entity is, so the check simply
 * never matches when the mod is absent.
 */
public final class PTDEntities {

    // Born in Chaos
    public static final ResourceLocation KRAMPUS = id(ModCompat.BORN_IN_CHAOS, "krampus");
    public static final ResourceLocation KRAMPUS_HENCHMAN = id(ModCompat.BORN_IN_CHAOS, "krampus_henchman");
    public static final ResourceLocation PUMPKIN_PISTOL_PROJECTILE = id(ModCompat.BORN_IN_CHAOS, "pumpkin_pistol_projectile");

    // Terramity
    public static final ResourceLocation ULTRA_SNIFFER = id(ModCompat.TERRAMITY, "ultra_sniffer");

    // Marium's Soulslike Weaponry
    public static final ResourceLocation DAY_STALKER = id(ModCompat.SOULS_WEAPONRY, "day_stalker");
    public static final ResourceLocation NIGHT_PROWLER = id(ModCompat.SOULS_WEAPONRY, "night_prowler");
    public static final ResourceLocation FREYR_SWORD = id(ModCompat.SOULS_WEAPONRY, "freyr_sword_entity");

    // Celestisynth
    public static final ResourceLocation SOLARIS_BOMB = id(ModCompat.CELESTISYNTH, "solaris_bomb");
    public static final ResourceLocation CRESCENTIA_DRAGON = id(ModCompat.CELESTISYNTH, "crescentia_dragon");
    public static final ResourceLocation FROSTBOUND_SHARD = id(ModCompat.CELESTISYNTH, "frostbound_shard");

    private PTDEntities() {
    }

    public static ResourceLocation idOf(Entity entity) {
        return EntityType.getKey(entity.getType());
    }

    /**
     * Lower-cased registry id plus type name of an entity, for identifying mobs from mods this
     * project doesn't compile against (e.g. "vessel", "plague_bringer").
     *
     * <p>Deliberately built from the entity's <em>type</em>, never {@link Entity#getName()}: that
     * returns the custom name, so a name-tagged chicken called "vessel" would count as a boss.
     */
    public static String typeSearchText(Entity entity) {
        EntityType<?> type = entity.getType();
        return (EntityType.getKey(type) + " " + type.getDescription().getString()).toLowerCase(Locale.ROOT);
    }

    private static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
