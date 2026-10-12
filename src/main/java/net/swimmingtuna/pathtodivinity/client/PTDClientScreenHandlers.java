package net.swimmingtuna.pathtodivinity.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.swimmingtuna.pathtodivinity.network.ProfileScreenS2CPacket;

/**
 * The client half of packet handling: every {@code Minecraft.setScreen} this mod performs lives here, so the
 * packet classes themselves stay loadable on a dedicated server. This mirrors LOTM's {@code ClientScreenHandlers}.
 */
@OnlyIn(Dist.CLIENT)
public final class PTDClientScreenHandlers {

    private PTDClientScreenHandlers() {
    }

    public static void openProfileScreen(ProfileScreenS2CPacket state) {
        Minecraft minecraft = Minecraft.getInstance();
        // A refresh after an attempted switch must not tear down the open screen: rebuilding it would drop the
        // player back to the game for a frame and lose their place.
        if (minecraft.screen instanceof ProfileScreen open) {
            open.refresh(state);
            return;
        }
        minecraft.setScreen(new ProfileScreen(state));
    }
}
