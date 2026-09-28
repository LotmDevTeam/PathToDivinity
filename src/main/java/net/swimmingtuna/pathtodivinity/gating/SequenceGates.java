package net.swimmingtuna.pathtodivinity.gating;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.PTD;

/**
 * Sequence-gated items: an item in {@code #pathtodivinity:requires_sequence_N} only works for a
 * Beyonder at Sequence N or stronger (a lower number is stronger). Until then it is inert: it can be
 * carried, but not used, and its passive effects don't run. See {@link UseBlocker}.
 *
 * <p>Tags sync to clients, and LOTM syncs a player's own sequence, so this check gives the same
 * answer on both sides.
 */
public final class SequenceGates {

    /** No gate on this item. */
    public static final int NONE = -1;

    private static final TagKey<Item>[] REQUIRES_SEQUENCE = createTags();

    private SequenceGates() {
    }

    @SuppressWarnings("unchecked")
    private static TagKey<Item>[] createTags() {
        TagKey<Item>[] tags = new TagKey[10];
        for (int sequence = 0; sequence < tags.length; sequence++) {
            tags[sequence] = TagKey.create(Registries.ITEM, new ResourceLocation(PTD.MOD_ID, "requires_sequence_" + sequence));
        }
        return tags;
    }

    /** The sequence an item requires, or {@link #NONE}. In several tiers, the strictest (lowest) wins. */
    public static int requiredSequence(ItemStack stack) {
        if (stack.isEmpty()) {
            return NONE;
        }
        for (int sequence = 0; sequence < REQUIRES_SEQUENCE.length; sequence++) {
            if (stack.is(REQUIRES_SEQUENCE[sequence])) {
                return sequence;
            }
        }
        return NONE;
    }

    public static boolean canUse(Player player, ItemStack stack) {
        int required = requiredSequence(stack);
        return required == NONE || meetsRequirement(player, required);
    }

    /**
     * Creative and spectator players always pass. Anyone else must be a Beyonder (players without a
     * pathway have sequence -1) at the required sequence or stronger.
     */
    public static boolean meetsRequirement(Player player, int requiredSequence) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        int sequence = BeyonderUtil.getSequence(player);
        return sequence >= 0 && sequence <= requiredSequence;
    }
}
