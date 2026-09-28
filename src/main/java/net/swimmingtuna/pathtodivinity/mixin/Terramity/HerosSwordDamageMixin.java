package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.procedures.HeroSwordProjectileProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;


@Mixin(value = HeroSwordProjectileProcedure.class, remap = false)
public class HerosSwordDamageMixin {

    @ModifyConstant(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            constant = @Constant(floatValue = 15.0F)
    )
    private static float modifyDamage(float damage) {
        return PTDBalance.HEROS_SWORD_PROJECTILE_DAMAGE.apply(damage);
    }
}