package net.swimmingtuna.pathtodivinity.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Registry ids of items from optional mods that code needs directly (boss drops). Item rules such as
 * bans and sequence gates are tags instead: see {@code PTDTags} and {@code SequenceGates}.
 */
public final class PTDItems {

    // Terramity
    public static final ResourceLocation POCKET_UNIVERSE = id(ModCompat.TERRAMITY, "pocket_universe");

    // Marium's Soulslike Weaponry
    public static final ResourceLocation LORD_SOUL_DARK = id(ModCompat.SOULS_WEAPONRY, "lord_soul_dark");
    public static final ResourceLocation SHARD_OF_UNCERTAINTY = id(ModCompat.SOULS_WEAPONRY, "shard_of_uncertainty");

    private PTDItems() {
    }

    /** The registered item, or {@code null} when its mod is not installed. */
    @Nullable
    public static Item get(ResourceLocation itemId) {
        return ForgeRegistries.ITEMS.containsKey(itemId) ? ForgeRegistries.ITEMS.getValue(itemId) : null;
    }

    private static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
