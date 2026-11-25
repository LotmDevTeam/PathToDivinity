package net.swimmingtuna.pathtodivinity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.swimmingtuna.lotm.beyonder.MobBeyonders;
import net.swimmingtuna.lotm.init.BeyonderClassInit;

/**
 * Mobs summoned by PtD's old {@code /beyonder summon} before LOTM had per-mob grants kept their pathway in
 * persistent data. The command itself is gone: LOTM's {@code /beyondersummon} is the one command for a single
 * Beyonder mob (LOTM #203). This still moves such old mobs onto a grant when they load.
 */
public final class LegacySummonMigration {

    private static final String PATHWAY_KEY = "ptdBeyonderPathway";
    private static final String SEQUENCE_KEY = "ptdBeyonderSequence";

    private LegacySummonMigration() {
    }

    /** Call before LOTM's join setup so the mob still starts as a Beyonder. */
    public static void migrateLegacy(LivingEntity living) {
        CompoundTag data = living.getPersistentData();
        if (!data.contains(PATHWAY_KEY)) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(data.getString(PATHWAY_KEY));
        // give() ignores a null (unknown) pathway
        MobBeyonders.give(living, id == null ? null : BeyonderClassInit.getRegistry().get(id), data.getInt(SEQUENCE_KEY));
        data.remove(PATHWAY_KEY);
        data.remove(SEQUENCE_KEY);
    }
}
