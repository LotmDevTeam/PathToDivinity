package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;

import org.objectweb.asm.Opcodes;
import org.thecelestialworkshop.celestisynth.common.entity.projectile.KeresSlash;
import org.thecelestialworkshop.celestisynth.common.entity.skillcast.SkillCastKeresSlashWave;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keres Slash Wave slash and shadow damage from [balance.celestisynth]. The rest is Celestisynth's own again, so the wave
 * is removed when its caster dies (an earlier copy of the method skipped that).
 */
@Mixin(value = SkillCastKeresSlashWave.class)
public class SkillCastKeresSlashWaveMixin {
    @Redirect(method = "tick", remap = true,
            at = @At(value = "FIELD", target = "Lorg/thecelestialworkshop/celestisynth/common/entity/projectile/KeresSlash;baseDamage:F", opcode = Opcodes.PUTFIELD, remap = false))
    private void pathtodivinity$slashDamage(KeresSlash slash, float damage) {
        slash.baseDamage = PTDBalance.KERES_SLASH_WAVE_SLASH_DAMAGE_MULTIPLIER.scale(damage);
    }

    @ModifyConstant(method = "tick", remap = true, constant = @Constant(floatValue = 2.0F, ordinal = 0))
    private float pathtodivinity$shadowDamage(float original) {
        return PTDBalance.KERES_SLASH_WAVE_SHADOW_DAMAGE.apply(original);
    }
}
