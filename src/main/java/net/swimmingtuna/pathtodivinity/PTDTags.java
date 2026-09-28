package net.swimmingtuna.pathtodivinity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tags that datapacks use to configure item and block rules. Defaults live in
 * {@code data/pathtodivinity/tags/}; every entry there is optional, so missing mods are skipped.
 */
public final class PTDTags {

    /** Removed from players and mobs on each item scan ({@code [items] banned_item_action}). */
    public static final TagKey<Item> BANNED = item("banned");
    /** Silently deleted from inventories and never allowed to exist as a dropped item. */
    public static final TagKey<Item> DESTROYED = item("destroyed");
    /** Blocks bosses never break while climbing to a player. */
    public static final TagKey<Block> BOSS_UNBREAKABLE = TagKey.create(Registries.BLOCK, new ResourceLocation(PTD.MOD_ID, "boss_unbreakable"));

    private PTDTags() {
    }

    static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(PTD.MOD_ID, path));
    }
}
