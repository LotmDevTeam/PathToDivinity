package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.procedures.CrescentMoonbladeProjectileProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;


@Mixin(value = CrescentMoonbladeProjectileProcedure.class, remap = false)
public class CrescentMoonbladeDamageMixin {

    @ModifyConstant(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            constant = @Constant(floatValue = 10.0F)
    )
    private static float modifyDamage(float damage) {
        return PTDBalance.CRESCENT_MOONBLADE_PROJECTILE_DAMAGE.apply(damage);
    }
}