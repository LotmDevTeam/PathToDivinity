package net.swimmingtuna.pathtodivinity.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.swimmingtuna.lotm.caps.BeyonderHolder;
import net.swimmingtuna.lotm.caps.BeyonderHolderAttacher;
import net.swimmingtuna.pathtodivinity.PTD;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Preserves the LOTM {@link BeyonderHolder} (pathway + sequence + spirituality) across death.
 *
 * <p>Written for Forge, where Obscure API (no NeoForge release yet) broke this: its
 * {@code PlayerDataCapability$EventBusHandlers.clonePlayer}
 * calls {@code event.getOriginal().invalidateCaps()} in the middle of {@link PlayerEvent.Clone},
 * which corrupts LOTM's own holder copy and leaves the respawned player with a half-initialised
 * holder (class set, sequence == -1). That both crashes {@code SpectatorAttributes#applyAll}
 * (now guarded by BeyonderHolderRespawnGuardMixin) and, more importantly, loses the player's
 * sequence so their attributes / max spirituality / regen never reset to the right values.
 *
 * <p>Strategy:
 * <ul>
 *   <li>{@code HIGHEST} priority — snapshot the original (dead) player's holder NBT <em>before</em>
 *       any other Clone handler (Obscure, LOTM) can touch it.</li>
 *   <li>{@code LOWEST} priority — write that snapshot back onto the new player's holder <em>after</em>
 *       everyone else has run, so our correct copy always wins.</li>
 * </ul>
 * This mirrors LOTM's own atomic NBT copy (serializeNBT -> deserializeNBT), so the new holder
 * ends up identical to the pre-death one. The respawn handler then applies modifiers with a valid
 * sequence and the guard mixin never has to fire.
 */
@EventBusSubscriber(modid = PTD.MOD_ID)
public class BeyonderHolderCloneFix {

    /** original player UUID -> pristine holder NBT captured before other mods mangle it. */
    private static final Map<UUID, CompoundTag> SNAPSHOTS = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureHolderBeforeClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        Player original = event.getOriginal();
        // LOTM's holder is a data attachment, which stays readable on the dead player (Forge capabilities
        // had to be revived first).
        BeyonderHolder oldHolder = BeyonderHolderAttacher.getHolderUnwrap(original);
        // Only snapshot a genuinely valid holder, so we never persist the corrupted state.
        if (oldHolder.getCurrentClass() != null && oldHolder.getSequence() >= 0) {
            SNAPSHOTS.put(original.getUUID(), oldHolder.serializeNBT(false));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restoreHolderAfterClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        CompoundTag snapshot = SNAPSHOTS.remove(event.getOriginal().getUUID());
        if (snapshot == null) {
            return;
        }
        BeyonderHolder newHolder = BeyonderHolderAttacher.getHolderUnwrap(event.getEntity());
        newHolder.deserializeNBT(snapshot, false);
    }
}
