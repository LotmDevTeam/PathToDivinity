package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.solaris.SolarisFullRoundAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (0.23) from [balance.celestisynth].
 */
@Mixin(value = SolarisFullRoundAttack.class, remap = false)
public class SolarisFullRoundAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "tickAttack", constant = @Constant(floatValue = 0.23F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.SOLARIS_FULL_ROUND_DAMAGE.apply(original);
    }
}
