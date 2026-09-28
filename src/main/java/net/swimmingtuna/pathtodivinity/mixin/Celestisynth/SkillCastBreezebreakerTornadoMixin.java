package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.thecelestialworkshop.celestisynth.common.entity.skillcast.SkillCastBreezebreakerTornado;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Multiplies Breezebreaker tornado damage ([balance.celestisynth]). The rest of the tick is Celestisynth's own again, so
 * the tornado is removed when its caster dies (an earlier copy of the method skipped that).
 */
@Mixin(value = SkillCastBreezebreakerTornado.class)
public class SkillCastBreezebreakerTornadoMixin {
    @ModifyArg(method = "tick", remap = true,
            at = @At(value = "INVOKE", target = "Lorg/thecelestialworkshop/celestisynth/common/entity/skillcast/SkillCastBreezebreakerTornado;initiateAbilityAttack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;FLorg/thecelestialworkshop/celestisynth/api/item/AttackHurtTypes;)V", remap = false),
            index = 2)
    private float pathtodivinity$scaleDamage(float damage) {
        return PTDBalance.BREEZEBREAKER_TORNADO_DAMAGE_MULTIPLIER.scale(damage);
    }
}
