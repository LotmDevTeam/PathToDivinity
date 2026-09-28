package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.procedures.ConductiteLaserRifleRightclickedProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;


@Mixin(value = ConductiteLaserRifleRightclickedProcedure.class, remap = false)
public class ConductiteLaserRifleDamageMixin {

    @ModifyConstant(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(doubleValue = 1.2D)
    )
    private static double modifyDamage(double damage) {
        return PTDBalance.CONDUCTITE_LASER_RIFLE_DAMAGE.apply(damage);
    }
}
