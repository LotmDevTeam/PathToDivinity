package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.swimmingtuna.lotm.LOTM;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.caps.BeyonderHolder;
import net.swimmingtuna.lotm.caps.BeyonderHolderAttacher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guards LOTM's {@link BeyonderHolder#onPlayerRespawn} against an inconsistent holder
 * state where the pathway (currentClass) is set but the sequence is still the constructor
 * default of -1.
 *
 * <p>This state is produced on respawn when Obscure API is present: its
 * {@code PlayerDataCapability$EventBusHandlers.clonePlayer} calls
 * {@code event.getOriginal().invalidateCaps()} in the middle of {@code PlayerEvent.Clone},
 * which interferes with LOTM's capability copy and leaves the new player's holder
 * half-initialised. {@code SpectatorAttributes#applyAll} then does {@code healthList.get(-1)}
 * and throws {@code ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 10}.
 *
 * <p>This is the mixin equivalent of adding {@code && holder.getSequence() >= 0} to the
 * {@code pathway != null} guard inside {@code onPlayerRespawn}: when the holder is in the
 * broken state, we skip the (crashing) modifier re-application for that respawn instead of
 * editing LOTM's source.
 */
@Mixin(value = BeyonderHolder.class, remap = false)
public class BeyonderHolderRespawnGuardMixin {

    @Inject(
            method = "onPlayerRespawn(Lnet/neoforged/neoforge/event/entity/player/PlayerEvent$PlayerRespawnEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void pathtodivinity$guardInvalidSequence(PlayerEvent.PlayerRespawnEvent event, CallbackInfo ci) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        BeyonderHolder holder = BeyonderHolderAttacher.getHolderUnwrap(player);
        BeyonderClass pathway = holder.getCurrentClass();
        if (pathway != null && holder.getSequence() < 0) {
            ci.cancel();
        }
    }
}
