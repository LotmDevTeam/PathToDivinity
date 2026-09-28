package net.swimmingtuna.pathtodivinity.compat;

import net.mcreator.borninchaosv.init.BornInChaosV1ModGameRules;
import net.minecraft.server.MinecraftServer;

/** Only call when {@link ModCompat#BORN_IN_CHAOS} is loaded. */
public final class BornInChaosCompat {

    private BornInChaosCompat() {
    }

    public static void disableKrampusSpawns(MinecraftServer server) {
        server.getGameRules().getRule(BornInChaosV1ModGameRules.KRAMPUS_SPAWN).set(false, server);
    }
}
