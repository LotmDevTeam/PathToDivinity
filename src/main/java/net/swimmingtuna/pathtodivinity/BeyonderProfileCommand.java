package net.swimmingtuna.pathtodivinity;

import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.swimmingtuna.pathtodivinity.profile.PlayerProfileData;
import net.swimmingtuna.pathtodivinity.profile.ProfileManager;
import net.swimmingtuna.pathtodivinity.profile.ProfileType;

public class BeyonderProfileCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("beyonderprofile")
                .requires(source -> PTDServerConfig.PROFILES_ENABLED.get())
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("cooldown")
                                .then(Commands.argument("minutes", IntegerArgumentType.integer(0))
                                        .executes(context -> setCooldown(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "minutes"))))
                                .executes(context -> showCooldown(context.getSource(),
                                        EntityArgument.getPlayer(context, "player"))))
                        .executes(context -> inspect(context.getSource(),
                                EntityArgument.getPlayer(context, "player"))))
                .executes(context -> openScreen(context.getSource()))
        );
    }

    /**
     * Opens the profile screen. Choosing and switching live there rather than in subcommands: a switch swaps
     * the player's pathway, inventory and experience at once, which is not a thing to fire off a tab-complete.
     */
    private static int openScreen(CommandSourceStack source) throws CommandSyntaxException {
        ProfileManager.openScreen(source.getPlayerOrException());
        return 1;
    }

    private static int showCooldown(CommandSourceStack source, ServerPlayer target) {
        long remaining = ProfileManager.getRemainingCooldownTicks(source.getServer(), target.getUUID());
        String name = target.getGameProfile().getName();

        if (remaining <= 0) {
            source.sendSuccess(() -> Component.literal(name + " has no profile switch cooldown.")
                    .withStyle(ChatFormatting.GRAY), false);
        } else {
            source.sendSuccess(() -> Component.literal(name + " must wait "
                    + ProfileManager.formatTicks(remaining) + " before switching profiles again."), false);
        }
        return 1;
    }

    private static int setCooldown(CommandSourceStack source, ServerPlayer target, int minutes) {
        String name = target.getGameProfile().getName();
        int configured = PTDServerConfig.PROFILE_SWITCH_COOLDOWN_MINUTES.get();

        if (minutes > 0 && configured <= 0) {
            source.sendFailure(Component.literal("The profile switch cooldown is disabled in the config, "
                    + "so there is nothing to set."));
            return 0;
        }

        int applied = ProfileManager.setRemainingCooldownMinutes(source.getServer(), target.getUUID(), minutes);
        if (applied <= 0) {
            source.sendSuccess(() -> Component.literal("Cleared " + name + "'s profile switch cooldown.")
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            // Capped at the configured cooldown: the stored stamp is a switch time, so a longer wait than the
            // config allows cannot be expressed.
            String capped = applied < minutes ? " (capped at the configured " + configured + "m)" : "";
            source.sendSuccess(() -> Component.literal("Set " + name + "'s profile switch cooldown to "
                    + applied + "m" + capped + "."), true);
        }
        return 1;
    }

    private static int inspect(CommandSourceStack source, ServerPlayer target) {
        ProfileType active = ProfileManager.getActiveProfile(target);
        String name = target.getGameProfile().getName();

        if (active == null) {
            source.sendSuccess(() -> Component.literal(name + " has not chosen a profile yet.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        String live = ProfileManager.describeLiveBeyonder(target);
        boolean hasOther = PlayerProfileData.get(source.getServer())
                .getSnapshot(target.getUUID(), active.other()) != null;

        source.sendSuccess(() -> Component.literal(name + " is on their ")
                .append(Component.literal(active.getDisplayName()).withStyle(active.getColor()))
                .append(Component.literal(" profile (" + live + "). "
                        + active.other().getDisplayName() + " profile: "
                        + (hasOther ? "saved" : "never used") + ".")), false);
        return 1;
    }
}
