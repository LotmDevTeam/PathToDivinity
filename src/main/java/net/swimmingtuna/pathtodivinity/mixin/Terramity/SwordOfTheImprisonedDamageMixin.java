package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.procedures.ImprisonedProjectileProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;


@Mixin(value = ImprisonedProjectileProcedure.class, remap = false)
public class SwordOfTheImprisonedDamageMixin {

    @ModifyConstant(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            constant = @Constant(floatValue = 22.0F)
    )
    private static float modifyDamage(float damage) {
        return PTDBalance.SWORD_OF_THE_IMPRISONED_PROJECTILE_DAMAGE.apply(damage);
    }
}