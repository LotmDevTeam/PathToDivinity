package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.attack.aquaflora.AquafloraSlashFrenzyAttack;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sets the damage factor Celestisynth passes to attributeDependentAttack (0.8) from [balance.celestisynth].
 */
@Mixin(value = AquafloraSlashFrenzyAttack.class, remap = false)
public class AquafloraSlashFrenzyAttackMixin {
    /** Damage factor. */
    @ModifyConstant(method = "tickAttack", constant = @Constant(floatValue = 0.8F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.AQUAFLORA_SLASH_FRENZY_SINGLE_WIELD_DAMAGE.apply(original);
    }
}
