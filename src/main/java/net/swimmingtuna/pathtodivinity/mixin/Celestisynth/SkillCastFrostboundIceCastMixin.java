package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.entity.skillcast.SkillCastFrostboundIceCast;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Multiplies Frostbound ice cast damage ([balance.celestisynth]).
 */
@Mixin(value = SkillCastFrostboundIceCast.class)
public class SkillCastFrostboundIceCastMixin {
    @ModifyArg(method = "tick", remap = true,
            at = @At(value = "INVOKE", target = "Lorg/thecelestialworkshop/celestisynth/common/entity/skillcast/SkillCastFrostboundIceCast;initiateAbilityAttack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;FLorg/thecelestialworkshop/celestisynth/api/item/AttackHurtTypes;)V", remap = false),
            index = 2)
    private float pathtodivinity$scaleDamage(float damage) {
        return PTDBalance.FROSTBOUND_ICE_CAST_DAMAGE_MULTIPLIER.scale(damage);
    }
}
