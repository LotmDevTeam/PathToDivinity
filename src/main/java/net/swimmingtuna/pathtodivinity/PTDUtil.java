package net.swimmingtuna.pathtodivinity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfiles;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;


public class PTDUtil {

    /** A Beyonder boss, per its boss profile ({@code data/<ns>/ptd_beyonders}). */
    public static boolean isBeyonderEntity(Entity entity) {
        return BeyonderProfiles.isBoss(entity);
    }


    /** In {@code #pathtodivinity:banned}. */
    public static boolean isBannableItem(ItemStack itemStack) {
        return itemStack.is(PTDTags.BANNED);
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
            // getContainerSize() covers the main inventory, armor and offhand. Sequence-gated items are
            // no longer taken away here: they stay, inert, until the player can use them (gating/).
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack itemStack = inventory.getItem(i);
                if (isBannableItem(itemStack)) {
                    inventory.setItem(i, ItemStack.EMPTY);
                    disposeOfBannedItem(player, itemStack);
                    living.sendSystemMessage(Component.literal("Banned item removed from inventory: " + itemStack.getHoverName().getString()).withStyle(ChatFormatting.RED));
                } else if (itemStack.is(PTDTags.DESTROYED)) {
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
}
