package net.swimmingtuna.pathtodivinity.gating;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.swimmingtuna.pathtodivinity.PTD;

/** "Requires Sequence N or higher" on gated items: red while unmet, grey once met. */
@Mod.EventBusSubscriber(modid = PTD.MOD_ID, value = Dist.CLIENT)
public final class GateTooltip {

    private GateTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
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
