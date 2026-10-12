package net.swimmingtuna.pathtodivinity.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.swimmingtuna.pathtodivinity.network.ProfileVisibilityS2CPacket;
import net.swimmingtuna.pathtodivinity.profile.ProfileType;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What this client has been told about other players' profiles.
 *
 * <p>The server's profile store never leaves the server, so this is the only thing name tag rendering has to
 * go on. Deliberately not synchronised: payload handlers run on the client thread, so every read and
 * write happens there.
 */
@OnlyIn(Dist.CLIENT)
public final class ProfileVisibilityCache {

    private static final Map<UUID, ProfileType> PROFILES = new HashMap<>();

    private ProfileVisibilityCache() {
    }

    public static void apply(ProfileVisibilityS2CPacket packet) {
        if (packet.isReplaceAll()) {
            PROFILES.clear();
        }
        for (Map.Entry<UUID, ProfileType> entry : packet.getEntries().entrySet()) {
            // A null profile is how a logout, or a player who never chose, is carried across the wire.
            if (entry.getValue() == null) {
                PROFILES.remove(entry.getKey());
            } else {
                PROFILES.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Nullable
    public static ProfileType get(UUID uuid) {
        return PROFILES.get(uuid);
    }

    public static boolean isSafemode(UUID uuid) {
        return PROFILES.get(uuid) == ProfileType.SAFEMODE;
    }

    /** Called when leaving a server, so its roster is not carried into the next one. */
    public static void clear() {
        PROFILES.clear();
    }
}
