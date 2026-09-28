package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.item.UnholyLanceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;

@Mixin(value = UnholyLanceItem.class, remap = true)
public class UnholyLanceMixin {

    @ModifyConstant(
            method = "getDefaultAttributeModifiers",
            constant = @Constant(doubleValue = 15.0)
    )
    private double modifyAttackDamage(double damage) {
        return PTDBalance.UNHOLY_LANCE_ATTACK_DAMAGE.apply(damage);
    }
}
