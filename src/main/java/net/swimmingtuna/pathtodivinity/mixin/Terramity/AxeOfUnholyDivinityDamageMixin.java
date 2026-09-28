package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.procedures.AxeOfUnholyDivinityRightclickedProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;


@Mixin(value = AxeOfUnholyDivinityRightclickedProcedure.class, remap = false)
public class AxeOfUnholyDivinityDamageMixin {

    @ModifyConstant(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(intValue = 80),
            remap = false
    )
    private static int modifyEffectDuration(int original) {
        return PTDBalance.AXE_OF_UNHOLY_DIVINITY_EFFECT_TICKS.apply(original);
    }
}