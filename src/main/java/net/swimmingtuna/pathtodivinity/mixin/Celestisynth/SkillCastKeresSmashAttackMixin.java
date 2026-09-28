package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.entity.skillcast.SkillCastKeresSmash;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Keres Smash pulse strengths, hit damage and caster healing from [balance.celestisynth]. The rest is Celestisynth's own
 * again, so the smash is removed when its caster dies (an earlier copy of the method skipped that).
 */
@Mixin(value = SkillCastKeresSmash.class)
public class SkillCastKeresSmashAttackMixin {
    @ModifyConstant(method = "tick", remap = true, constant = @Constant(floatValue = 1.0F, ordinal = 0))
    private float pathtodivinity$firstPulse(float original) {
        return PTDBalance.KERES_SMASH_FIRST_PULSE.apply(original);
    }

    @ModifyConstant(method = "tick", remap = true, constant = @Constant(floatValue = 0.8F, ordinal = 0))
    private float pathtodivinity$secondPulse(float original) {
        return PTDBalance.KERES_SMASH_SECOND_PULSE.apply(original);
    }

    @ModifyConstant(method = "tick", remap = true, constant = @Constant(floatValue = 0.6F, ordinal = 0))
    private float pathtodivinity$thirdPulse(float original) {
        return PTDBalance.KERES_SMASH_THIRD_PULSE.apply(original);
    }

    @ModifyArg(method = "doSmashAttack", remap = false,
            at = @At(value = "INVOKE", target = "Lorg/thecelestialworkshop/celestisynth/common/entity/skillcast/SkillCastKeresSmash;initiateAbilityAttack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;FLorg/thecelestialworkshop/celestisynth/api/item/AttackHurtTypes;)V", remap = false),
            index = 2)
    private float pathtodivinity$scaleHit(float damage) {
        return PTDBalance.KERES_SMASH_HIT_MULTIPLIER.scale(damage);
    }

    @ModifyConstant(method = "doSmashAttack", remap = false, constant = @Constant(floatValue = 4.0F, ordinal = 0))
    private float pathtodivinity$healDivisor(float original) {
        return PTDBalance.KERES_SMASH_HEAL_DIVISOR.apply(original);
    }
}
