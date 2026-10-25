package net.swimmingtuna.pathtodivinity.inactivity;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

import java.util.Collection;

/**
 * {@code /beyonder demote <player>}: drops a player one Sequence through {@link InactivityTracker}, so their slot
 * is released and, if they are offline, the change lands at their next login like an inactivity demotion.
 */
public class DemoteCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("beyonder")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("demote")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(context -> demote(context.getSource(),
                                        GameProfileArgument.getGameProfiles(context, "player"))))));
    }

    private static int demote(CommandSourceStack source, Collection<GameProfile> profiles) throws CommandSyntaxException {
        int demoted = 0;
        for (GameProfile profile : profiles) {
            String error = InactivityTracker.adminDemote(source.getServer(), profile.getId());
            if (error != null) {
                source.sendFailure(Component.literal(profile.getName() + ": " + error));
                continue;
            }
            boolean online = source.getServer().getPlayerList().getPlayer(profile.getId()) != null;
            source.sendSuccess(() -> Component.literal("Demoted " + profile.getName() + " one Sequence"
                    + (online ? "." : "; it applies when they next log in.")), true);
            demoted++;
        }
        return demoted;
    }
}
