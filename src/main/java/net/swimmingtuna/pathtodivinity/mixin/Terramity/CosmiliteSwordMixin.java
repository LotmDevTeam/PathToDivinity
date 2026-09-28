package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;

@Mixin(targets = "net.mcreator.terramity.item.CosmiliteSwordItem$1")
public class CosmiliteSwordMixin {

    @Inject(method = "getAttackDamageBonus", at = @At("HEAD"), cancellable = true)
    private void modifyDamage(CallbackInfoReturnable<Float> cir) {
        if (PTDBalance.COSMILITE_SWORD_BONUS_DAMAGE.enabled()) {
            cir.setReturnValue((float) PTDBalance.COSMILITE_SWORD_BONUS_DAMAGE.get());
        }
    }
}
