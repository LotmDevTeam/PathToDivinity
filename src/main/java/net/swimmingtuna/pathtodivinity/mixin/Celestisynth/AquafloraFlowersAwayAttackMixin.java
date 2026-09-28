package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.aquaflora.AquafloraFlowersAwayAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (0.5) from [balance.celestisynth].
 */
@Mixin(value = AquafloraFlowersAwayAttack.class, remap = false)
public class AquafloraFlowersAwayAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "startUsing", constant = @Constant(floatValue = 0.5F, ordinal = 1))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.AQUAFLORA_FLOWERS_AWAY_DAMAGE.apply(original);
    }
}
