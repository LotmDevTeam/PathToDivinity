package net.swimmingtuna.pathtodivinity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.compat.PTDEntities;
import net.swimmingtuna.pathtodivinity.compat.PTDItems;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;

import java.util.HashSet;
import java.util.Set;

public class PTDUtil {

    // Registry ids of all beyonder entity types, so optional mods never have to be loaded to check one
    private static final Set<ResourceLocation> BEYONDER_ENTITY_TYPES = new HashSet<>();

    static {
        // Initialize the set with all beyonder entity types
        initializeBeyonderEntityTypes();
    }

    private static void initializeBeyonderEntityTypes() {
        // Sequence 9 entities
        BEYONDER_ENTITY_TYPES.add(PTDEntities.OVERGROWN_COLOSSUS);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.WARPED_FUNGUSSUS);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.KOBOLEDIATOR);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.UMVUTHI);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.MAW);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.SKELETOSAURUS);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.NIGHTMARE_STALKER);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.GLUTTON_FISH);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.WROUGHTNAUT);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.DIRE_HOUND_LEADER);
        //Witness too but it shouldn't destroy blocks

        // Sequence 8 entities
        BEYONDER_ENTITY_TYPES.add(PTDEntities.BLAST_CANNON);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.FROSTBITTEN_GOLEM);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.ENDERSENT);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.DUSKROK);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.MUTANT_SKELETON);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.MUTANT_ENDERMAN);
        //BEYONDER_ENTITY_TYPES.add(AMEntityRegistry.WARPED_MOSCO.get());
        BEYONDER_ENTITY_TYPES.add(EntityType.getKey(EntityType.ELDER_GUARDIAN));
        BEYONDER_ENTITY_TYPES.add(PTDEntities.SPIRITOF_CHAOS);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.MOTHER_SPIDER);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.HELLROK);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.MUTANT_ZOMBIE);


        // Sequence 7 entities
        BEYONDER_ENTITY_TYPES.add(PTDEntities.ANCIENT_GUARDIAN);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.CORPSE_WARLOCK);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.FROSTMAW);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.MAZE_MOTHER);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.WITHERED_ABOMINATION);
        BEYONDER_ENTITY_TYPES.add(net.swimmingtuna.lotm.init.EntityInit.ASMANN.getId());


        // Sequence 6 entities
        BEYONDER_ENTITY_TYPES.add(PTDEntities.NETHERITE_MONSTROSITY);
        BEYONDER_ENTITY_TYPES.add(EntityType.getKey(EntityType.WITHER));
        BEYONDER_ENTITY_TYPES.add(PTDEntities.HEROBRINE);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.LIFESTEALER);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.LAVA_EATER);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.SIR_PUMPKINHEAD);
        BEYONDER_ENTITY_TYPES.add(net.swimmingtuna.lotm.init.EntityInit.SHADOWLESS_DEMONIC_WOLF.getId());

        // Sequence 5 entities
        BEYONDER_ENTITY_TYPES.add(PTDEntities.THE_HARBINGER);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.CAPTAIN_CORNELIA);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.POSESSED_PALADIN);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.ACCURSED_LORD_BOSS); //Decaying King
        BEYONDER_ENTITY_TYPES.add(PTDEntities.RETURNING_KNIGHT);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.ENDER_GUARDIAN);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.MOONKNIGHT); //Fallen Icon
        BEYONDER_ENTITY_TYPES.add(PTDEntities.CHAOS_MONARCH); //Monarch of Chaos
        BEYONDER_ENTITY_TYPES.add(PTDEntities.DRAUGR_BOSS); //Old Champion's Remains
        BEYONDER_ENTITY_TYPES.add(PTDEntities.NIGHT_SHADE); //Frenzied Shade
        BEYONDER_ENTITY_TYPES.add(net.swimmingtuna.lotm.init.EntityInit.DRAGON.getId());

        // Sequence 4 entities
        BEYONDER_ENTITY_TYPES.add(PTDEntities.CLOUD_GOLEM);
        //BEYONDER_ENTITY_TYPES.add(AMEntityRegistry.VOID_WORM.get());
        BEYONDER_ENTITY_TYPES.add(PTDEntities.IGNIS);

        BEYONDER_ENTITY_TYPES.add(PTDEntities.SCYLLA); //ADD TO SEQUENCE 4
        BEYONDER_ENTITY_TYPES.add(PTDEntities.MALEDICTUS); //ADD TO SEQUENCE 4

        BEYONDER_ENTITY_TYPES.add(PTDEntities.GOB);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.THE_LEVIATHAN);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.NAMELESS_GUARDIAN);

        // Sequence 3 entities
        BEYONDER_ENTITY_TYPES.add(PTDEntities.LORD_PUMPKINHEAD);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.TRIAL_GUARDIAN);

        // Sequence 2 entities
        BEYONDER_ENTITY_TYPES.add(PTDEntities.SUPER_SNIFFER);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.GUNDALF);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.DAY_STALKER);
        BEYONDER_ENTITY_TYPES.add(PTDEntities.NIGHT_PROWLER);

        // Sequence 1 entities
        BEYONDER_ENTITY_TYPES.add(PTDEntities.ULTRA_SNIFFER);
        BEYONDER_ENTITY_TYPES.add(net.swimmingtuna.lotm.init.EntityInit.INTERDIMENSIONAL_HUNTER.getId());
    }


    private static boolean matchesNameBasedConditions(Entity entity) {
        // Type-based, not entity.getName(): a custom name would let any name-tagged mob pass.
        String entityName = PTDEntities.typeSearchText(entity);
        String className = entity.getClass().getSimpleName(); //Comments are equal to sequence
        if (entityName.contains("vessel")) return true; //3
        if (PTDEntities.isHorseman(entity)) return true;  //4
        if (entityName.contains("doomharbor")) return true; //7
        if (entityName.contains("terrible") || entityName.contains("puny")) return true; //8
        if (entityName.contains("plague_bringer")) return true; //7
        if (entityName.contains("aero_guardian")) return true; //8
        if (entityName.contains("dyrolian")) return true; //6
        if (className.equals("VoidBlossomEntity")) return true; //6
        if (className.equals("LichEntity")) return true; //7
        return className.equals("GauntletEntity"); //7
    }

    // Main method that checks both Set and name-based conditions
    public static boolean isBeyonderEntity(Entity entity) {
        if (BEYONDER_ENTITY_TYPES.contains(PTDEntities.idOf(entity))) {
            return true;
        }
        return matchesNameBasedConditions(entity);
    }

    public static boolean isBeyonderEntity(EntityType<?> entityType) {
        return BEYONDER_ENTITY_TYPES.contains(EntityType.getKey(entityType));
    }


    public static boolean isBannableItem(ItemStack itemStack) {
        return

                PTDItems.is(itemStack, PTDItems.ULTRA_SNIFFER_FUR) ||
                        PTDItems.is(itemStack, PTDItems.POKER_CHIP_BRACELETS) ||
                        PTDItems.is(itemStack, PTDItems.FATEFUL_COIN) ||
                        PTDItems.is(itemStack, PTDItems.LUCKY_DICE) ||

                        PTDItems.is(itemStack, PTDItems.CURSIUM_CHESTPLATE);
        //DyrolianSword

    }


    public static void removeBannedItem(LivingEntity living) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                ItemStack itemStack = living.getItemBySlot(slot);
                if (isBannableItem(itemStack)) {
                    living.setItemSlot(slot, ItemStack.EMPTY);
                    disposeOfBannedItem(living, itemStack);
                    living.sendSystemMessage(Component.literal("Banned item removed: " + itemStack.getHoverName().getString()).withStyle(ChatFormatting.RED));
                }
            }
        }
        if (living instanceof Player player) {
            // getContainerSize() covers the main inventory, armor and offhand, so held items are
            // included. (Held Celestisynth weapons used to be moved into the inventory first, only
            // to be dropped by this same loop straight after.)
            boolean canUseSequence4Items = canUseSequence4Items(player);
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack itemStack = inventory.getItem(i);
                if (isBannableItem(itemStack)) {
                    inventory.setItem(i, ItemStack.EMPTY);
                    disposeOfBannedItem(player, itemStack);
                    living.sendSystemMessage(Component.literal("Banned item removed from inventory: " + itemStack.getHoverName().getString()).withStyle(ChatFormatting.RED));
                } else if (!canUseSequence4Items && isBannableSequence5Item(itemStack)) {
                    inventory.setItem(i, ItemStack.EMPTY);
                    player.drop(itemStack, false);
                    living.sendSystemMessage(Component.literal("Item dropped from inventory: " + itemStack.getHoverName().getString() + " (Requires Sequence 4 or higher)").withStyle(ChatFormatting.GOLD));
                } else if (PTDItems.is(itemStack, PTDItems.MUSIC_SHEET_OF_UNTIMELY_DEATH)) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    /**
     * Applies [items] banned_item_action to an item already taken off {@code living}. Only players
     * can have it dropped; a mob's banned gear is always deleted so it can't be farmed.
     */
    private static void disposeOfBannedItem(LivingEntity living, ItemStack removed) {
        if (living instanceof Player player && PTDServerConfig.BANNED_ITEM_ACTION.get() == PTDServerConfig.BannedItemAction.DROP) {
            player.drop(removed, false);
        }
    }

    /**
     * Whether the player is a Beyonder of Sequence 4 or stronger (lower number = stronger).
     * Players without a pathway have sequence -1, which the old {@code getSequence > 4} check
     * treated as allowed, so non-Beyonders could use every restricted weapon.
     */
    public static boolean canUseSequence4Items(Player player) {
        int sequence = BeyonderUtil.getSequence(player);
        return sequence >= 0 && sequence <= 4;
    }

    public static boolean isBannableSequence5Item(ItemStack stack) {
        return
                PTDItems.is(stack, PTDItems.AQUAFLORA) ||
                        PTDItems.is(stack, PTDItems.KERES) ||
                        PTDItems.is(stack, PTDItems.BREEZEBREAKER) ||
                        PTDItems.is(stack, PTDItems.SOLARIS) ||
                        PTDItems.is(stack, PTDItems.CRESCENTIA) ||
                        PTDItems.is(stack, PTDItems.POLTERGEIST) ||
                        PTDItems.is(stack, PTDItems.RAINFALL_SERENITY) ||
                        PTDItems.is(stack, PTDItems.FROSTBOUND);
    }
}
