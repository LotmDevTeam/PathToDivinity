package net.swimmingtuna.pathtodivinity.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Registry ids of items from optional mods. See {@link PTDEntities} for why ids are used instead of
 * the mods' own registry classes.
 */
public final class PTDItems {

    // Terramity
    public static final ResourceLocation ULTRA_SNIFFER_FUR = id(ModCompat.TERRAMITY, "ultra_sniffer_fur");
    public static final ResourceLocation POKER_CHIP_BRACELETS = id(ModCompat.TERRAMITY, "poker_chip_bracelets");
    public static final ResourceLocation FATEFUL_COIN = id(ModCompat.TERRAMITY, "fateful_coin");
    public static final ResourceLocation LUCKY_DICE = id(ModCompat.TERRAMITY, "lucky_dice");
    public static final ResourceLocation MUSIC_SHEET_OF_UNTIMELY_DEATH = id(ModCompat.TERRAMITY, "music_sheet_of_untimely_death");
    public static final ResourceLocation POCKET_UNIVERSE = id(ModCompat.TERRAMITY, "pocket_universe");

    // Cataclysm
    public static final ResourceLocation CURSIUM_CHESTPLATE = id(ModCompat.CATACLYSM, "cursium_chestplate");

    // Celestisynth
    public static final ResourceLocation AQUAFLORA = id(ModCompat.CELESTISYNTH, "aquaflora");
    public static final ResourceLocation KERES = id(ModCompat.CELESTISYNTH, "keres");
    public static final ResourceLocation BREEZEBREAKER = id(ModCompat.CELESTISYNTH, "breezebreaker");
    public static final ResourceLocation SOLARIS = id(ModCompat.CELESTISYNTH, "solaris");
    public static final ResourceLocation CRESCENTIA = id(ModCompat.CELESTISYNTH, "crescentia");
    public static final ResourceLocation POLTERGEIST = id(ModCompat.CELESTISYNTH, "poltergeist");
    public static final ResourceLocation RAINFALL_SERENITY = id(ModCompat.CELESTISYNTH, "rainfall_serenity");
    public static final ResourceLocation FROSTBOUND = id(ModCompat.CELESTISYNTH, "frostbound");

    // Marium's Soulslike Weaponry
    public static final ResourceLocation MEHRUNES_RAZOR = id(ModCompat.SOULS_WEAPONRY, "mehrunes_razor");
    public static final ResourceLocation LORD_SOUL_DARK = id(ModCompat.SOULS_WEAPONRY, "lord_soul_dark");
    public static final ResourceLocation SHARD_OF_UNCERTAINTY = id(ModCompat.SOULS_WEAPONRY, "shard_of_uncertainty");

    private PTDItems() {
    }

    public static boolean is(ItemStack stack, ResourceLocation itemId) {
        return itemId.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
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
