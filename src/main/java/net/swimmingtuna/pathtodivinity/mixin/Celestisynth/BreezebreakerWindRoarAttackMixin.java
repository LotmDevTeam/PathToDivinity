package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.breezebreaker.BreezebreakerWindRoarAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (0.075) from [balance.celestisynth].
 */
@Mixin(value = BreezebreakerWindRoarAttack.class, remap = false)
public class BreezebreakerWindRoarAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "tickAttack", constant = @Constant(floatValue = 0.075F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.BREEZEBREAKER_WIND_ROAR_DAMAGE.apply(original);
    }
}
