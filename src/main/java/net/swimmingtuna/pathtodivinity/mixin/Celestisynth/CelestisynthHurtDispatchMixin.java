package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.swimmingtuna.pathtodivinity.gating.SequenceGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.thecelestialworkshop.celestisynth.api.item.CSWeapon;
import org.thecelestialworkshop.celestisynth.common.events.CSCommonMiscEvents;

/**
 * Celestisynth weapons' "when the holder is hurt" passives (Aquaflora; Breezebreaker, including the
 * fall immunity BreezebreakerMixin gives it) are called from this one event handler for held
 * weapons. Skip them while the holder is below the weapon's required sequence.
 */
@Mixin(value = CSCommonMiscEvents.class, remap = false)
public class CelestisynthHurtDispatchMixin {

    @Redirect(method = "onLivingHurtEvent",
            at = @At(value = "INVOKE",
                    target = "Lorg/thecelestialworkshop/celestisynth/api/item/CSWeapon;onPlayerHurt(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;Lnet/minecraft/world/item/ItemStack;)V"),
            remap = false)
    private static void pathtodivinity$gatePassive(CSWeapon weapon, LivingHurtEvent event, ItemStack stack) {
        if (event.getEntity() instanceof Player player && !SequenceGates.canUse(player, stack)) {
            return;
        }
        weapon.onPlayerHurt(event, stack);
    }
}
