package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.poltergeist.PoltergeistCosmicSteelAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Multiplies Poltergeist Cosmic Steel smash damage ([balance.celestisynth]).
 */
@Mixin(value = PoltergeistCosmicSteelAttack.class, remap = false)
public class PoltergeistCosmicSteelAttackMixin {
    @ModifyArg(method = "doImpact",
            at = @At(value = "INVOKE", target = "Lorg/thecelestialworkshop/celestisynth/common/attack/poltergeist/PoltergeistCosmicSteelAttack;initiateAbilityAttack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;FLorg/thecelestialworkshop/celestisynth/api/item/AttackHurtTypes;)V", remap = false),
            index = 2)
    private float pathtodivinity$scaleDamage(float damage) {
        return PTDBalance.POLTERGEIST_COSMIC_STEEL_DAMAGE_MULTIPLIER.scale(damage);
    }
}
