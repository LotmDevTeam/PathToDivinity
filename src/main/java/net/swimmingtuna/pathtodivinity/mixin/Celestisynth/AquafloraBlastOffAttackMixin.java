package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.aquaflora.AquafloraBlastOffAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (1.3) from [balance.celestisynth].
 */
@Mixin(value = AquafloraBlastOffAttack.class, remap = false)
public class AquafloraBlastOffAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "startUsing", constant = @Constant(floatValue = 1.3F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.AQUAFLORA_BLAST_OFF_DAMAGE.apply(original);
    }
}
