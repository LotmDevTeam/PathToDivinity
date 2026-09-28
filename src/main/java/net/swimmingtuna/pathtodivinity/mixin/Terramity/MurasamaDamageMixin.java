package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.procedures.MurasamaDamageProcedure;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Damage of Murasama's delayed slash (Terramity: 30) from [balance.terramity]. The slash runs in a lambda Terramity
 * queues from execute(); this used to overwrite the whole procedure.
 */
@Mixin(value = MurasamaDamageProcedure.class, remap = false)
public class MurasamaDamageMixin {
    @ModifyConstant(method = "lambda$execute$2", constant = @Constant(floatValue = 30.0F))
    private static float pathtodivinity$procDamage(float original) {
        return PTDBalance.MURASAMA_PROC_DAMAGE.apply(original);
    }
}
