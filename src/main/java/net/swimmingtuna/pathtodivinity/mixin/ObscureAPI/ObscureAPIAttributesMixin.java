package net.swimmingtuna.pathtodivinity.mixin.ObscureAPI;


import com.obscuria.obscureapi.registry.ObscureAPIAttributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;

@Mixin(value = ObscureAPIAttributes.class, remap = false)
public class ObscureAPIAttributesMixin {

    @Inject(method = "criticalHitAndMagicResistanceEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void disableEvent(LivingHurtEvent event, CallbackInfo ci) {
        if (PTDBalance.DISABLE_CRIT_AND_MAGIC_RESISTANCE.isOn()) {
            ci.cancel();
        }
    }
}
