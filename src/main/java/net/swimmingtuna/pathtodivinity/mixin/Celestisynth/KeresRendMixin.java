package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.entity.projectile.KeresRend;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Keres Rend max-health damage factor (Celestisynth: 0.015) from [balance.celestisynth]. It respects mobGriefing again: an
 * earlier copy of the whole method had dropped that check.
 */
@Mixin(value = KeresRend.class, remap = false)
public class KeresRendMixin {
    /** Scales the part of each hit that is a share of the target's max health. */
    @ModifyConstant(method = "checkWalls", constant = @Constant(floatValue = 0.015F))
    private float pathtodivinity$damage(float original) {
        return PTDBalance.KERES_REND_MAX_HEALTH_DAMAGE.apply(original);
    }
}
