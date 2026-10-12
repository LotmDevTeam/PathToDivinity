package net.swimmingtuna.pathtodivinity.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.profile.ProfileManager;
import net.swimmingtuna.pathtodivinity.profile.ProfileVisibility;

/**
 * Connection-time halves of the profile roster clients draw name tags from.
 *
 * <p>Profile changes broadcast themselves from {@link ProfileManager}; these two only cover a player
 * arriving with a roster to catch up on, and leaving with an entry to clear.
 */
@EventBusSubscriber(modid = PTD.MOD_ID)
public class ProfileVisibilityEvents {

    @SubscribeEvent
    public static void syncOnLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ProfileVisibility.syncAllTo(player);
        // The roster above only reaches the joiner; everyone already online needs telling about them.
        ProfileVisibility.broadcast(player, ProfileManager.getActiveProfile(player));
    }

    @SubscribeEvent
    public static void clearOnLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProfileVisibility.broadcast(player, null);
        }
    }
}
