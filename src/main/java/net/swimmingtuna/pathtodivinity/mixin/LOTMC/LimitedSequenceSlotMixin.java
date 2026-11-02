package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.server.MinecraftServer;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.pathtodivinity.profile.ProfileManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import net.swimmingtuna.lotm.beyonder.Pathways;

/**
 * Keeps Safemode players out of LOTM's capped sequence slots.
 *
 * <p>With {@code Only One God} enabled, {@code Sequence0Data} allows just one Sequence 0 holder per pathway per
 * world (plus three at Sequence 1 and nine at Sequence 2). {@code Pathways.setSequence} and
 * {@code setPathwayAndSequence} both funnel their slot bookkeeping through
 * {@code updateLimitedSequenceSlots}, which is the one place to intercept.
 *
 * <p>A Safemode profile carries no PvP risk, so letting it occupy a capped slot would let a risk-free player
 * lock a Normal-profile player out of a high sequence. Safemode therefore claims nothing.
 *
 * <p>Returning {@code true} matters: the method's contract is "the slot bookkeeping succeeded". Returning
 * {@code false} makes the caller abandon the sequence change entirely, which would stop Safemode players
 * advancing at all.
 */
@Mixin(value = Pathways.class, remap = false)
public class LimitedSequenceSlotMixin {

    @Inject(
            method = "updateLimitedSequenceSlots(Lnet/minecraft/server/MinecraftServer;Ljava/util/UUID;"
                    + "Lnet/swimmingtuna/lotm/beyonder/api/BeyonderClass;I"
                    + "Lnet/swimmingtuna/lotm/beyonder/api/BeyonderClass;I)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void pathtodivinity$safemodeClaimsNoSlots(MinecraftServer server, UUID uuid,
                                                             BeyonderClass oldPathway, int oldSequence,
                                                             BeyonderClass newPathway, int newSequence,
                                                             CallbackInfoReturnable<Boolean> cir) {
        if (server != null && ProfileManager.isSafemode(server, uuid)) {
            cir.setReturnValue(true);
        }
    }
}
