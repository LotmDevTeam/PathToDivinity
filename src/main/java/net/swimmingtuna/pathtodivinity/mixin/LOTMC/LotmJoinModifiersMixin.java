package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.world.entity.LivingEntity;
import net.swimmingtuna.lotm.beyonder.MobBeyonders;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.swimmingtuna.lotm.beyonder.Pathways;

/**
 * When a mob registered with LOTM joins the world, LOTM applies its pathway's stat modifiers and
 * pathway events on top of Path to Divinity's scaling. For bosses whose profile says
 * {@code "ability_sequence": -1} ("a Beyonder of this pathway, but inert") both are skipped, so
 * giving such a boss a pathway changes nothing about how it fights.
 *
 * <p>LOTM does that setup in {@code MobBeyonders.startUp}, on join and when a mob is given a pathway.
 */
@Mixin(value = MobBeyonders.class, remap = false)
public class LotmJoinModifiersMixin {

    @Redirect(method = "startUp",
            at = @At(value = "INVOKE", target = "Lnet/swimmingtuna/lotm/beyonder/Pathways;applyPathwayAttributes(Lnet/minecraft/world/entity/LivingEntity;Lnet/swimmingtuna/lotm/beyonder/api/BeyonderClass;I)V"),
            remap = false)
    private static void pathtodivinity$applyPathwayAttributes(LivingEntity entity, BeyonderClass pathway, int sequence) {
        if (usesPathwayEffects(entity)) {
            Pathways.applyPathwayAttributes(entity, pathway, sequence);
        }
    }

    @Redirect(method = "startUp",
            at = @At(value = "INVOKE", target = "Lnet/swimmingtuna/lotm/beyonder/api/BeyonderClass;startPassives(Lnet/minecraft/world/entity/LivingEntity;I)V"),
            remap = false)
    private static void pathtodivinity$startPassives(BeyonderClass pathway, LivingEntity entity, int sequence) {
        if (usesPathwayEffects(entity)) {
            pathway.startPassives(entity, sequence);
        }
    }

    private static boolean usesPathwayEffects(LivingEntity entity) {
        BeyonderProfile profile = BeyonderProfiles.find(entity);
        return profile == null || profile.usesAbilities() || MobBeyonders.hasGrant(entity);
    }
}
