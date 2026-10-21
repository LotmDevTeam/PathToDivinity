package net.swimmingtuna.pathtodivinity.inactivity;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;

import java.util.Map;
import java.util.UUID;

/** {@code /beyonder inactivity}: every tracked Sequence 0 player and how long until they are demoted. */
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
        long timeout = InactivityTracker.timeoutMillis();
        int shown = 0;
        for (Map.Entry<UUID, InactivityData.Entry> mapEntry : InactivityData.get(server).entries().entrySet()) {
            InactivityData.Entry entry = mapEntry.getValue();
            if (entry.sequence0Pathway == null && !entry.pendingDemotion) {
                continue;
            }
            String status;
            if (entry.pendingDemotion) {
                status = "slot released, demoted to Sequence 1 at next login";
            } else if (server.getPlayerList().getPlayer(mapEntry.getKey()) != null) {
                status = "online";
            } else if (entry.lastActiveMillis == Long.MIN_VALUE) {
                status = "not timed yet";
            } else {
                status = "demoted in " + InactivityTracker.formatMillis(timeout - (now - entry.lastActiveMillis));
            }
            String line = name(server, mapEntry.getKey()) + " (" + entry.sequence0Pathway + "): " + status;
            source.sendSuccess(() -> Component.literal(line), false);
            shown++;
        }
        if (shown == 0) {
            source.sendSuccess(() -> Component.literal("No Sequence 0 players are being tracked.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return shown;
    }

    private static String name(MinecraftServer server, UUID uuid) {
        GameProfileCache cache = server.getProfileCache();
        return cache == null ? uuid.toString() : cache.get(uuid).map(GameProfile::getName).orElse(uuid.toString());
    }
}
