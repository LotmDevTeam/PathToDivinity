package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.procedures.BlasphemicRaptureRightclickedProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;


@Mixin(value = BlasphemicRaptureRightclickedProcedure.class, remap = false)
public class BlasphemicRaptureDamageMixin {

    @ModifyConstant(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(doubleValue = 2.25D)
    )
    private static double modifyDamage(double damage) {
        return PTDBalance.BLASPHEMIC_RAPTURE_DAMAGE.apply(damage);
    }
}
