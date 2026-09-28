package net.swimmingtuna.pathtodivinity;

import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.swimmingtuna.lotm.caps.BeyonderHolder;
import net.swimmingtuna.lotm.caps.BeyonderHolderAttacher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@code /sequencelock} — caps how far players may advance down the Beyonder ladder.
 *
 * <p>Only available while {@code Sequence Lock Enabled} is on in the config, so regular players on a normal
 * pack never see it. The lock itself lives in {@link SequenceLockData} and is enforced by
 * {@code mixin/LOTMC/BeyonderPotionMixin}.
 */
public class SequenceLockCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sequencelock")
                // Evaluated on every use and on command-tree sync, so toggling the config takes effect
                // immediately and the command stays hidden from tab-complete while the feature is off.
                .requires(source -> source.hasPermission(2) && PTDServerConfig.SEQUENCE_LOCK_ENABLED.get())
                .then(Commands.literal("unlock")
                        .executes(context -> unlock(context.getSource())))
                .then(Commands.argument("sequence", IntegerArgumentType.integer(0, 9))
                        .executes(context -> lock(context.getSource(),
                                IntegerArgumentType.getInteger(context, "sequence"))))
                .executes(context -> status(context.getSource()))
        );
    }

    private static int lock(CommandSourceStack source, int sequence) {
        MinecraftServer server = source.getServer();
        SequenceLockData.get(server).setLockedSequence(sequence);

        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "Sequence advancement is now locked at Sequence " + sequence
                        + ". You cannot advance past it.").withStyle(ChatFormatting.GOLD), false);

        reportPlayersPastLock(source, sequence);
        return 1;
    }

    private static int unlock(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        SequenceLockData data = SequenceLockData.get(server);
        if (!data.isLocked()) {
            source.sendFailure(Component.literal("Sequence advancement is not locked."));
            return 0;
        }
        data.setLockedSequence(SequenceLockData.NO_LOCK);

        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "Sequence advancement is no longer locked.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        SequenceLockData data = SequenceLockData.get(source.getServer());
        if (data.isLocked()) {
            int sequence = data.getLockedSequence();
            source.sendSuccess(() -> Component.literal(
                    "Sequence advancement is locked at Sequence " + sequence
                            + ". Use /sequencelock unlock to remove it."), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Sequence advancement is not locked. Use /sequencelock <0-9> to set a cap."), false);
        }
        return 1;
    }

    /**
     * Tells every online op which players are already stronger than the new lock. Setting a lock never demotes
     * anyone, so this is how an admin finds out who slipped past before the cap went up.
     *
     * <p>Only online players can be checked — an offline player's {@link BeyonderHolder} capability is not loaded.
     */
    private static void reportPlayersPastLock(CommandSourceStack source, int lockedSequence) {
        MinecraftServer server = source.getServer();
        List<ServerPlayer> pastLock = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BeyonderHolder holder = BeyonderHolderAttacher.getHolderUnwrap(player);
            // getSequence() >= 0 skips half-initialised holders, same guard as BeyonderHolderCloneFix.
            if (holder.getCurrentClass() != null && holder.getSequence() >= 0
                    && holder.getSequence() < lockedSequence) {
                pastLock.add(player);
            }
        }
        pastLock.sort(Comparator.comparingInt(player -> BeyonderHolderAttacher.getHolderUnwrap(player).getSequence()));

        MutableComponent report;
        if (pastLock.isEmpty()) {
            report = Component.literal("[Sequence Lock] No online players are past Sequence " + lockedSequence + ".")
                    .withStyle(ChatFormatting.GRAY);
        } else {
            report = Component.literal("[Sequence Lock] " + pastLock.size() + " online player(s) are already past Sequence "
                    + lockedSequence + ":").withStyle(ChatFormatting.YELLOW);
            for (ServerPlayer player : pastLock) {
                report.append(Component.literal("\n  " + player.getName().getString() + " - Sequence "
                                + BeyonderHolderAttacher.getHolderUnwrap(player).getSequence())
                        .withStyle(ChatFormatting.YELLOW));
            }
        }

        MutableComponent finalReport = report;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(finalReport);
            }
        }
        // Console and command blocks are not in the player list, so feed them the report directly.
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendSuccess(() -> finalReport, false);
        }
    }
}
