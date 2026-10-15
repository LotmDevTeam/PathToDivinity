package net.swimmingtuna.pathtodivinity;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /abilityban add|remove|list} — edits [items] banned_abilities. The config file stays the single
 * source of truth; see {@link PTDUtil#isAbilityBanned}.
 */
public class AbilityBanCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("abilityban")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        BeyonderUtil.getAbilities().stream().map(BuiltInRegistries.ITEM::getKey), builder))
                                .executes(context -> add(context.getSource(), ResourceLocationArgument.getId(context, "item")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        PTDServerConfig.BANNED_ABILITIES.get().stream().map(String::valueOf), builder))
                                .executes(context -> remove(context.getSource(), ResourceLocationArgument.getId(context, "item")))))
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource())))
        );
    }

    private static int add(CommandSourceStack source, ResourceLocation id) {
        if (!BeyonderUtil.getAbilities().contains(BuiltInRegistries.ITEM.get(id))) {
            source.sendFailure(Component.literal(id + " is not a LOTM ability."));
            return 0;
        }
        List<String> banned = new ArrayList<>(PTDServerConfig.BANNED_ABILITIES.get());
        if (banned.contains(id.toString())) {
            source.sendFailure(Component.literal(id + " is already banned."));
            return 0;
        }
        banned.add(id.toString());
        write(banned);
        source.sendSuccess(() -> Component.literal("Banned " + id + "."), true);
        return 1;
    }

    private static int remove(CommandSourceStack source, ResourceLocation id) {
        List<String> banned = new ArrayList<>(PTDServerConfig.BANNED_ABILITIES.get());
        if (!banned.remove(id.toString())) {
            source.sendFailure(Component.literal(id + " is not banned."));
            return 0;
        }
        write(banned);
        source.sendSuccess(() -> Component.literal("Unbanned " + id + "."), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        List<? extends String> banned = PTDServerConfig.BANNED_ABILITIES.get();
        source.sendSuccess(() -> Component.literal(banned.isEmpty()
                ? "No abilities are banned."
                : "Banned abilities: " + String.join(", ", banned)), false);
        return banned.size();
    }

    private static void write(List<String> banned) {
        PTDServerConfig.BANNED_ABILITIES.set(banned);
        PTDServerConfig.BANNED_ABILITIES.save();
        PTDServerConfig.rebuildBannedAbilities();
    }
}
