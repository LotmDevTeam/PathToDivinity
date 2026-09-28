package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.procedures.VulcanRightclickedProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;


@Mixin(value = VulcanRightclickedProcedure.class, remap = false)
public class VulcanDamageMixin {

    @ModifyConstant(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(doubleValue = 0.35D)
    )
    private static double modifyDamage(double damage) {
        return PTDBalance.VULCAN_DAMAGE.apply(damage);
    }
}
