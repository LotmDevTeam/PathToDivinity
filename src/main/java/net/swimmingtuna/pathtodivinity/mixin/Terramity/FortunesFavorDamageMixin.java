package net.swimmingtuna.pathtodivinity.mixin.Terramity;

import net.mcreator.terramity.procedures.FortunesFavorRightclickedProcedure;
import net.mcreator.terramity.procedures.SharperRoundsDamageProcedure;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;

@Mixin(value = FortunesFavorRightclickedProcedure.class, remap = false)
public class FortunesFavorDamageMixin {

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/mcreator/terramity/procedures/SharperRoundsDamageProcedure;execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/item/ItemStack;)D"))
    private static double amplifyBulletDamage(LevelAccessor world, ItemStack itemstack) {
        double originalDamage = SharperRoundsDamageProcedure.execute(world, itemstack);
        return PTDBalance.FORTUNES_FAVOR_DAMAGE_MULTIPLIER.scale(originalDamage);
    }
}