package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.breezebreaker.BreezebreakerDualGalestormAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (1.3) from [balance.celestisynth].
 */
@Mixin(value = BreezebreakerDualGalestormAttack.class, remap = false)
public class BreezebreakerDualGalestormAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "tickAttack", constant = @Constant(floatValue = 1.3F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.BREEZEBREAKER_DUAL_GALESTORM_DAMAGE.apply(original);
    }
}
