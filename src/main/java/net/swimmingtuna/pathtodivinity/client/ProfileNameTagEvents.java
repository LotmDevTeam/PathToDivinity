package net.swimmingtuna.pathtodivinity.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.swimmingtuna.pathtodivinity.PTD;

/**
 * Client-side lifecycle for the profile visibility roster.
 *
 * <p>Safemode players render with their ordinary name tag — no marker is appended.
 */
@EventBusSubscriber(modid = PTD.MOD_ID, value = Dist.CLIENT)
public class ProfileNameTagEvents {

    @SubscribeEvent
    public static void forgetRosterOnDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ProfileVisibilityCache.clear();
    }
}
