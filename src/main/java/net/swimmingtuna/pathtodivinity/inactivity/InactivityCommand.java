package net.swimmingtuna.pathtodivinity.inactivity;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.swimmingtuna.lotm.world.worlddata.Sequence0Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** {@code /beyonder inactivity}: every tracked Sequence 2-0 player, when they were last seen and when they are demoted. */
public class InactivityCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("beyonder")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("inactivity")
                        .executes(context -> list(context.getSource()))));
    }

    private static int list(CommandSourceStack source) {
        if (!InactivityTracker.isEnabled()) {
            source.sendFailure(Component.literal("[inactivity] is disabled in the server config."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        long now = System.currentTimeMillis();
        List<Map.Entry<UUID, InactivityData.Entry>> tracked = new ArrayList<>();
        for (Map.Entry<UUID, InactivityData.Entry> mapEntry : InactivityData.get(server).entries().entrySet()) {
            if (mapEntry.getValue().limitedPathway != null) {
                tracked.add(mapEntry);
            }
        }
        tracked.sort(Comparator.comparingInt((Map.Entry<UUID, InactivityData.Entry> e) -> e.getValue().limitedSequence)
                .thenComparing(e -> name(server, e.getKey())));
        for (Map.Entry<UUID, InactivityData.Entry> mapEntry : tracked) {
            InactivityData.Entry entry = mapEntry.getValue();
            String lastActivity;
            if (server.getPlayerList().getPlayer(mapEntry.getKey()) != null) {
                lastActivity = "online now";
            } else if (entry.lastLogoutMillis == Long.MIN_VALUE) {
                lastActivity = "last seen unknown";
            } else {
                lastActivity = "last seen " + InactivityTracker.formatMillis(now - entry.lastLogoutMillis) + " ago";
            }
            int effective = InactivityTracker.effectiveSequence(entry);
            String demotion = entry.pendingSteps > 0 ? "falls to Seq " + effective + " at next login" : "";
            if (Sequence0Data.isLimitedSequence(effective)) {
                String next;
                if (entry.lastActiveMillis == Long.MIN_VALUE) {
                    next = "not timed yet";
                } else {
                    long left = InactivityTracker.timeoutMillis(entry.strikes) - (now - entry.lastActiveMillis);
                    next = left > 0 ? "Seq " + (effective + 1) + " in " + InactivityTracker.formatMillis(left)
                            : "Seq " + (effective + 1) + " due once offline";
                }
                demotion = demotion.isEmpty() ? next : demotion + ", then " + next;
            }
            if (entry.strikes > 0) {
                demotion += " (strike " + entry.strikes + ")";
            }
            String line = name(server, mapEntry.getKey()) + " Seq " + entry.limitedSequence + " (" + entry.limitedPathway
                    + "): " + lastActivity + ", " + demotion;
            source.sendSuccess(() -> Component.literal(line), false);
        }
        if (tracked.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No Sequence 2-0 players are being tracked.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return tracked.size();
    }

    private static String name(MinecraftServer server, UUID uuid) {
        GameProfileCache cache = server.getProfileCache();
        return cache == null ? uuid.toString() : cache.get(uuid).map(GameProfile::getName).orElse(uuid.toString());
    }
}
