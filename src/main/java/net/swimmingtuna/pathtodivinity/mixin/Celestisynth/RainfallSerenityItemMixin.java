package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.thecelestialworkshop.celestisynth.common.item.weapons.RainfallSerenityItem;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scales Rainfall Serenity arrows' base damage ([balance.celestisynth]) after Celestisynth builds them. This used to
 * overwrite the whole method, which also replaced Celestisynth's Apothic Attributes handling; that is its own again.
 */
@Mixin(value = RainfallSerenityItem.class, remap = false)
public class RainfallSerenityItemMixin {
    @Inject(method = "customArrow", at = @At("RETURN"))
    private void pathtodivinity$scaleArrow(AbstractArrow arrow, CallbackInfoReturnable<AbstractArrow> cir) {
        AbstractArrow built = cir.getReturnValue();
        if (PTDBalance.RAINFALL_SERENITY_ARROW_DAMAGE_MULTIPLIER.enabled()) {
            built.setBaseDamage(PTDBalance.RAINFALL_SERENITY_ARROW_DAMAGE_MULTIPLIER.scale(built.getBaseDamage()));
        }
    }
}
