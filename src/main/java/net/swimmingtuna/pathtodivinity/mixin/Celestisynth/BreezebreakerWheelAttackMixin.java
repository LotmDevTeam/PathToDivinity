package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.breezebreaker.BreezebreakerWheelAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (1.4) from [balance.celestisynth].
 */
@Mixin(value = BreezebreakerWheelAttack.class, remap = false)
public class BreezebreakerWheelAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "tickAttack", constant = @Constant(floatValue = 1.4F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.BREEZEBREAKER_WHEEL_DAMAGE.apply(original);
    }
}
