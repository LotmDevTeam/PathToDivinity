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

    // Legendary Monsters
    public static final ResourceLocation OVERGROWN_COLOSSUS = id(ModCompat.LEGENDARY_MONSTERS, "overgrown_colossus");
    public static final ResourceLocation WARPED_FUNGUSSUS = id(ModCompat.LEGENDARY_MONSTERS, "warped_fungussus");
    public static final ResourceLocation SKELETOSAURUS = id(ModCompat.LEGENDARY_MONSTERS, "skeletosaurus");
    public static final ResourceLocation BLAST_CANNON = id(ModCompat.LEGENDARY_MONSTERS, "dune_sentinel");
    public static final ResourceLocation FROSTBITTEN_GOLEM = id(ModCompat.LEGENDARY_MONSTERS, "frostbitten_golem");
    public static final ResourceLocation ENDERSENT = id(ModCompat.LEGENDARY_MONSTERS, "endersent");
    public static final ResourceLocation ANCIENT_GUARDIAN = id(ModCompat.LEGENDARY_MONSTERS, "ancient_guardian");
    public static final ResourceLocation WITHERED_ABOMINATION = id(ModCompat.LEGENDARY_MONSTERS, "withered_abomination");
    public static final ResourceLocation LAVA_EATER = id(ModCompat.LEGENDARY_MONSTERS, "lava_eater");
    public static final ResourceLocation POSESSED_PALADIN = id(ModCompat.LEGENDARY_MONSTERS, "posessed_paladin");
    public static final ResourceLocation CLOUD_GOLEM = id(ModCompat.LEGENDARY_MONSTERS, "cloud_golem");

    // Cataclysm
    public static final ResourceLocation KOBOLEDIATOR = id(ModCompat.CATACLYSM, "kobolediator");
    public static final ResourceLocation NETHERITE_MONSTROSITY = id(ModCompat.CATACLYSM, "netherite_monstrosity");
    public static final ResourceLocation THE_HARBINGER = id(ModCompat.CATACLYSM, "the_harbinger");
    public static final ResourceLocation ENDER_GUARDIAN = id(ModCompat.CATACLYSM, "ender_guardian");
    public static final ResourceLocation IGNIS = id(ModCompat.CATACLYSM, "ignis");
    public static final ResourceLocation SCYLLA = id(ModCompat.CATACLYSM, "scylla");
    public static final ResourceLocation MALEDICTUS = id(ModCompat.CATACLYSM, "maledictus");
    public static final ResourceLocation THE_LEVIATHAN = id(ModCompat.CATACLYSM, "the_leviathan");
    public static final ResourceLocation ANCIENT_REMNANT = id(ModCompat.CATACLYSM, "ancient_remnant");

    // Mowzie's Mobs
    public static final ResourceLocation UMVUTHI = id(ModCompat.MOWZIES_MOBS, "umvuthi");
    public static final ResourceLocation WROUGHTNAUT = id(ModCompat.MOWZIES_MOBS, "ferrous_wroughtnaut");
    public static final ResourceLocation FROSTMAW = id(ModCompat.MOWZIES_MOBS, "frostmaw");

    // Aquamirae
    public static final ResourceLocation MAW = id(ModCompat.AQUAMIRAE, "maw");
    public static final ResourceLocation MAZE_MOTHER = id(ModCompat.AQUAMIRAE, "maze_mother");
    public static final ResourceLocation CAPTAIN_CORNELIA = id(ModCompat.AQUAMIRAE, "captain_cornelia");

    // Born in Chaos
    public static final ResourceLocation NIGHTMARE_STALKER = id(ModCompat.BORN_IN_CHAOS, "nightmare_stalker");
    public static final ResourceLocation GLUTTON_FISH = id(ModCompat.BORN_IN_CHAOS, "glutton_fish");
    public static final ResourceLocation DIRE_HOUND_LEADER = id(ModCompat.BORN_IN_CHAOS, "dire_hound_leader");
    public static final ResourceLocation SPIRITOF_CHAOS = id(ModCompat.BORN_IN_CHAOS, "spiritof_chaos");
    public static final ResourceLocation MOTHER_SPIDER = id(ModCompat.BORN_IN_CHAOS, "mother_spider");
    public static final ResourceLocation LIFESTEALER = id(ModCompat.BORN_IN_CHAOS, "lifestealer");
    public static final ResourceLocation SIR_PUMPKINHEAD = id(ModCompat.BORN_IN_CHAOS, "sir_pumpkinhead");
    public static final ResourceLocation LORD_PUMPKINHEAD = id(ModCompat.BORN_IN_CHAOS, "lord_pumpkinhead");
    public static final ResourceLocation KRAMPUS = id(ModCompat.BORN_IN_CHAOS, "krampus");
    public static final ResourceLocation KRAMPUS_HENCHMAN = id(ModCompat.BORN_IN_CHAOS, "krampus_henchman");
    public static final ResourceLocation PUMPKIN_PISTOL_PROJECTILE = id(ModCompat.BORN_IN_CHAOS, "pumpkin_pistol_projectile");

    // Terramity
    public static final ResourceLocation DUSKROK = id(ModCompat.TERRAMITY, "duskrok");
    public static final ResourceLocation HELLROK = id(ModCompat.TERRAMITY, "hellrok");
    public static final ResourceLocation GOB = id(ModCompat.TERRAMITY, "gob");
    public static final ResourceLocation TRIAL_GUARDIAN = id(ModCompat.TERRAMITY, "trial_guardian");
    public static final ResourceLocation SUPER_SNIFFER = id(ModCompat.TERRAMITY, "super_sniffer");
    public static final ResourceLocation GUNDALF = id(ModCompat.TERRAMITY, "gundalf");
    public static final ResourceLocation ULTRA_SNIFFER = id(ModCompat.TERRAMITY, "ultra_sniffer");

    // Mutant Monsters
    public static final ResourceLocation MUTANT_SKELETON = id(ModCompat.MUTANT_MONSTERS, "mutant_skeleton");
    public static final ResourceLocation MUTANT_ENDERMAN = id(ModCompat.MUTANT_MONSTERS, "mutant_enderman");
    public static final ResourceLocation MUTANT_ZOMBIE = id(ModCompat.MUTANT_MONSTERS, "mutant_zombie");

    // EEEAB's Mobs
    public static final ResourceLocation CORPSE_WARLOCK = id(ModCompat.EEEABS_MOBS, "corpse_warlock");
    public static final ResourceLocation NAMELESS_GUARDIAN = id(ModCompat.EEEABS_MOBS, "nameless_guardian");

    // Awakened Bosses
    public static final ResourceLocation HEROBRINE = id(ModCompat.AWAKENED_BOSSES, "herobrine");

    // Marium's Soulslike Weaponry
    public static final ResourceLocation ACCURSED_LORD_BOSS = id(ModCompat.SOULS_WEAPONRY, "accursed_lord_boss"); //Decaying King
    public static final ResourceLocation RETURNING_KNIGHT = id(ModCompat.SOULS_WEAPONRY, "returning_knight");
    public static final ResourceLocation MOONKNIGHT = id(ModCompat.SOULS_WEAPONRY, "moonknight"); //Fallen Icon
    public static final ResourceLocation CHAOS_MONARCH = id(ModCompat.SOULS_WEAPONRY, "chaos_monarch"); //Monarch of Chaos
    public static final ResourceLocation DRAUGR_BOSS = id(ModCompat.SOULS_WEAPONRY, "draugr_boss"); //Old Champion's Remains
    public static final ResourceLocation NIGHT_SHADE = id(ModCompat.SOULS_WEAPONRY, "night_shade"); //Frenzied Shade
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

    /** The Pumpkin Horseman (Sleepy Hollows), matched by type rather than custom name. */
    public static boolean isHorseman(Entity entity) {
        EntityType<?> type = entity.getType();
        return EntityType.getKey(type).getPath().equals("horseman")
                || type.getDescription().getString().equalsIgnoreCase("horseman");
    }

    private static ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
