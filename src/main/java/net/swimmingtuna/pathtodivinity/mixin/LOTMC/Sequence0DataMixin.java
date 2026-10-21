package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.swimmingtuna.lotm.world.worlddata.Sequence0Data;
import net.swimmingtuna.pathtodivinity.inactivity.InactivityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * LOTM drops any player who was offline for 12,000,000 game ticks (~7 days, not counting server downtime) to
 * Sequence 1 on login. With [inactivity] enabled {@link InactivityTracker} does this instead, configurably and
 * without leaving the Sequence 0 slot held while the player is away, so LOTM's check is turned off.
 */
@Mixin(value = Sequence0Data.class, remap = false)
public class Sequence0DataMixin {

    @Inject(method = "shouldSetTag(Ljava/util/UUID;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void pathtodivinity$inactivityReplacesLoginDemotion(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        if (InactivityTracker.isEnabled()) {
            cir.setReturnValue(false);
        }
    }
}
