package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.thecelestialworkshop.celestisynth.common.entity.skillcast.SkillCastPoltergeistWard;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;

@Mixin(value = SkillCastPoltergeistWard.class, remap = true)
public class PoltergeistWardDamageMixin {

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 1)
    private float modifyDamage(float damage) {
        return PTDBalance.POLTERGEIST_WARD_DAMAGE.apply(damage);
    }
}