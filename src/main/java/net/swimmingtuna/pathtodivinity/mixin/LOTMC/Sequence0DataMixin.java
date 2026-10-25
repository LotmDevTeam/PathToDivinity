package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.swimmingtuna.lotm.world.worlddata.Sequence0Data;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import net.swimmingtuna.pathtodivinity.inactivity.InactivityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * LOTM drops any player who was offline for 12,000,000 game ticks (~7 days, not counting server downtime) to
 * Sequence 1 on login. With [inactivity] enabled {@link InactivityTracker} does this instead, configurably and
 * for Sequences 2 to 0, without leaving the slot held while the player is away, so LOTM's check is turned off.
 *
 * <p>Also replaces LOTM's hardcoded slot caps (1 / 3 / 9) with [slots] in the server config. Every capacity
 * check in {@code Sequence0Data} goes through {@code getSequenceCapacity}.
 */
@Mixin(value = Sequence0Data.class, remap = false)
public class Sequence0DataMixin {

    @Inject(method = "shouldSetTag(Ljava/util/UUID;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void pathtodivinity$inactivityReplacesLoginDemotion(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        if (InactivityTracker.isEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getSequenceCapacity(I)I", at = @At("HEAD"), cancellable = true, remap = false)
    private static void pathtodivinity$configuredCapacity(int sequence, CallbackInfoReturnable<Integer> cir) {
        if (!PTDServerConfig.SPEC.isLoaded()) {
            return; // no server running: keep LOTM's defaults
        }
        switch (sequence) {
            case 0 -> cir.setReturnValue(PTDServerConfig.SLOTS_SEQUENCE_0.get());
            case 1 -> cir.setReturnValue(PTDServerConfig.SLOTS_SEQUENCE_1.get());
            case 2 -> cir.setReturnValue(PTDServerConfig.SLOTS_SEQUENCE_2.get());
            default -> {
            }
        }
    }
}
