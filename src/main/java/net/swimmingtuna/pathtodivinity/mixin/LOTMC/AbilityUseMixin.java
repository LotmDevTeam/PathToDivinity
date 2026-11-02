package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.swimmingtuna.lotm.ability.AbilityUse;
import net.swimmingtuna.pathtodivinity.PTDUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbilityUse.class, remap = false)
public class AbilityUseMixin {

    /** [items] banned_abilities: keybind, wheel and hotbar casts. */
    @Inject(method = "useAbilityItem", at = @At("HEAD"), cancellable = true)
    private static void blockBannedCast(Player player, Item item, InteractionHand hand, CallbackInfo ci) {
        if (PTDUtil.isAbilityBanned(item)) {
            ci.cancel();
        }
    }

    /** [items] banned_abilities: passives, toggles and ability entities. */
    @Inject(method = "canUseAbility(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;)Z", at = @At("HEAD"), cancellable = true)
    private static void blockBannedAbility(LivingEntity living, Item item, CallbackInfoReturnable<Boolean> cir) {
        if (PTDUtil.isAbilityBanned(item)) {
            cir.setReturnValue(false);
        }
    }
}
