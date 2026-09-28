package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.solaris.SolarisSoulDashAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (0.18) from [balance.celestisynth].
 */
@Mixin(value = SolarisSoulDashAttack.class, remap = false)
public class SolarisSoulDashAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "tickAttack", constant = @Constant(floatValue = 0.18F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.SOLARIS_SOUL_DASH_DAMAGE.apply(original);
    }
}
