package net.swimmingtuna.pathtodivinity.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Registry ids of items from optional mods that code needs directly (boss drops). Item rules such as
 * bans and sequence gates are tags instead: see {@code PTDTags} and {@code SequenceGates}.
 */
public final class PTDItems {

    private PTDItems() {
    }

    /** The registered item, or {@code null} when its mod is not installed. */
    @Nullable
    public static Item get(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
    }
}
