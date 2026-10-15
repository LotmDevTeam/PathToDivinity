package net.swimmingtuna.pathtodivinity.gating;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.PTDUtil;

/** "Requires Sequence N or higher" on gated items: red while unmet, grey once met. */
@EventBusSubscriber(modid = PTD.MOD_ID, value = Dist.CLIENT)
public final class GateTooltip {

    private GateTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (PTDUtil.isAbilityBanned(event.getItemStack().getItem())) {
            event.getToolTip().add(Component.translatable("tooltip.pathtodivinity.ability_banned").withStyle(ChatFormatting.RED));
        }
        int required = SequenceGates.requiredSequence(event.getItemStack());
        if (required == SequenceGates.NONE) {
            return;
        }
        Player player = event.getEntity();
        boolean met = player != null && SequenceGates.meetsRequirement(player, required);
        event.getToolTip().add(Component.translatable("tooltip.pathtodivinity.requires_sequence", required)
                .withStyle(met ? ChatFormatting.GRAY : ChatFormatting.RED));
    }
}
