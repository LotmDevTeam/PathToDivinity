package net.swimmingtuna.pathtodivinity.gating;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.swimmingtuna.pathtodivinity.PTD;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Stops players below an item's required sequence from using it, however they try. Checks run on
 * both sides so the client doesn't predict a swing or skill that the server then refuses; the
 * message is sent only by the server. Passive effects are stopped separately, by
 * {@code ItemStackInventoryTickMixin} and the Celestisynth hurt-dispatch mixin.
 */
@Mod.EventBusSubscriber(modid = PTD.MOD_ID)
public final class UseBlocker {

    private static final int MESSAGE_COOLDOWN_TICKS = 20;
    /** Last game tick each player was told, so spamming right-click doesn't spam the action bar. */
    private static final Map<Player, Long> LAST_MESSAGE = new WeakHashMap<>();

    private UseBlocker() {
    }

    /** Right-click use: guns, staffs, Celestisynth skills (they start through Item.use). */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isBlocked(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /** Using the item on a block. The block itself (a chest, a door) still works. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isBlocked(event.getEntity(), event.getItemStack())) {
            event.setUseItem(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (isBlocked(player, player.getMainHandItem())) {
            event.setCanceled(true);
        }
    }

    /** Drawing a bow, charging a crossbow or any other held use. */
    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && isBlocked(player, event.getItem())) {
            event.setCanceled(true);
        }
    }

    /**
     * Fallback for direct hits that skip the events above, such as sweep attacks. Only hits dealt by
     * the player in person are checked, so LOTM abilities used while carrying a gated weapon still land.
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player && event.getSource().getDirectEntity() == player
                && isBlocked(player, player.getMainHandItem())) {
            event.setCanceled(true);
        }
    }

    /** Equipping can't be cancelled, so gated armour goes straight back into the inventory. */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot().getType() != EquipmentSlot.Type.ARMOR || !(event.getEntity() instanceof Player player)
                || player.level().isClientSide()) {
            return;
        }
        ItemStack worn = event.getTo();
        if (isBlocked(player, worn)) {
            ItemStack removed = worn.copy();
            player.setItemSlot(event.getSlot(), ItemStack.EMPTY);
            if (!player.getInventory().add(removed)) {
                player.drop(removed, false);
            }
        }
    }

    /** True, and tells the player why, when they may not use {@code stack}. */
    private static boolean isBlocked(Player player, ItemStack stack) {
        int required = SequenceGates.requiredSequence(stack);
        if (required == SequenceGates.NONE || SequenceGates.meetsRequirement(player, required)) {
            return false;
        }
        if (!player.level().isClientSide()) {
            long now = player.level().getGameTime();
            Long last = LAST_MESSAGE.get(player);
            if (last == null || now - last >= MESSAGE_COOLDOWN_TICKS) {
                LAST_MESSAGE.put(player, now);
                player.displayClientMessage(Component.translatable("message.pathtodivinity.requires_sequence",
                        required, stack.getHoverName()).withStyle(ChatFormatting.RED), true);
            }
        }
        return true;
    }
}
