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
    public static final ResourceLocation PUMPKIN_PISTOL_PROJECTILE = id(ModCompat.BORN_IN_CHAOS, "pumpkin_pistol_projectile");

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
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
