package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.aquaflora.AquafloraPetalPiercesAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (0.1) from [balance.celestisynth].
 */
@Mixin(value = AquafloraPetalPiercesAttack.class, remap = false)
public class AquafloraPetalPiercesAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "tickAttack", constant = @Constant(floatValue = 0.1F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.AQUAFLORA_PETAL_PIERCES_DAMAGE.apply(original);
    }
}
