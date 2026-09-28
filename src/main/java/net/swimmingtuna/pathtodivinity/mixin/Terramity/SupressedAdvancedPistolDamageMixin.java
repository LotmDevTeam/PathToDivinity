package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.procedures.SuppressedAdvancedPistolRightclickedProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;


@Mixin(value = SuppressedAdvancedPistolRightclickedProcedure.class, remap = false)
public class SupressedAdvancedPistolDamageMixin {

    @ModifyConstant(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(doubleValue = 1.15D)
    )
    private static double modifyDamage(double damage) {
        return PTDBalance.SUPPRESSED_ADVANCED_PISTOL_DAMAGE.apply(damage);
    }
}
