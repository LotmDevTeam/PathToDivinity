package net.swimmingtuna.pathtodivinity.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.swimmingtuna.pathtodivinity.gating.SequenceGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps sequence-gated items inert while carried by a player below their required sequence: most
 * "while in inventory / while held" passives (buffs, auras, particles) run from the item's
 * inventoryTick, which this skips. Works for any mod's item added to the gate tags.
 */
@Mixin(ItemStack.class)
public class ItemStackInventoryTickMixin {

    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    private void pathtodivinity$skipGatedItem(Level level, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        if (entity instanceof Player player && !SequenceGates.canUse(player, (ItemStack) (Object) this)) {
            ci.cancel();
        }
    }
}
