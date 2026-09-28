package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.cresentia.CrescentiaBarrageAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (0.07) from [balance.celestisynth].
 */
@Mixin(value = CrescentiaBarrageAttack.class, remap = false)
public class CrescentiaBarrageAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "tickAttack", constant = @Constant(floatValue = 0.07F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.CRESCENTIA_BARRAGE_DAMAGE.apply(original);
    }
}
