package net.swimmingtuna.pathtodivinity.mixin.BornInChaos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;

@Mixin(targets = "net.mcreator.borninchaosv.item.SoulbaneItem$1")
public class SoulbaneMixin {

    @Inject(method = "getAttackDamageBonus", at = @At("HEAD"), cancellable = true)
    private void modifyDamage(CallbackInfoReturnable<Float> cir) {
        if (PTDBalance.SOULBANE_BONUS_DAMAGE.enabled()) {
            cir.setReturnValue((float) PTDBalance.SOULBANE_BONUS_DAMAGE.get());
        }
    }
}
