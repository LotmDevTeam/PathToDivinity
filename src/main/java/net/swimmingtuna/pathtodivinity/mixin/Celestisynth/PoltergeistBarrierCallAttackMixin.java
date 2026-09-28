package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.poltergeist.PoltergeistBarrierCallAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (0.8) from [balance.celestisynth].
 */
@Mixin(value = PoltergeistBarrierCallAttack.class, remap = false)
public class PoltergeistBarrierCallAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "startUsing", constant = @Constant(floatValue = 0.8F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.POLTERGEIST_BARRIER_CALL_DAMAGE.apply(original);
    }
}
