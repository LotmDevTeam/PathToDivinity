package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.world.entity.LivingEntity;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.events.ModEvents;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * When a mob registered with LOTM joins the world, LOTM applies its pathway's stat modifiers and
 * pathway events on top of Path to Divinity's scaling. For bosses whose profile says
 * {@code "ability_sequence": -1} ("a Beyonder of this pathway, but inert") both are skipped, so
 * giving such a boss a pathway changes nothing about how it fights.
 *
 * <p>LOTM 2.5.2 and older call {@code pathway.applyAllModifiers}; 2.5.3 calls
 * {@code BeyonderUtil.applyPathwayAttributes} instead. Every redirect here is {@code require = 0} so
 * whichever call is missing is skipped rather than stopping the game from loading.
 */
@Mixin(value = ModEvents.class, remap = false)
public class LotmJoinModifiersMixin {

    @Redirect(method = "onLivingJoinWorld",
            at = @At(value = "INVOKE", target = "Lnet/swimmingtuna/lotm/beyonder/api/BeyonderClass;applyAllModifiers(Lnet/minecraft/world/entity/LivingEntity;I)V"),
            remap = false, require = 0)
    private static void pathtodivinity$applyModifiers(BeyonderClass pathway, LivingEntity entity, int sequence) {
        if (usesPathwayEffects(entity)) {
            pathway.applyAllModifiers(entity, sequence);
        }
    }

    @Redirect(method = "onLivingJoinWorld",
            at = @At(value = "INVOKE", target = "Lnet/swimmingtuna/lotm/util/BeyonderUtil;applyPathwayAttributes(Lnet/minecraft/world/entity/LivingEntity;Lnet/swimmingtuna/lotm/beyonder/api/BeyonderClass;I)V"),
            remap = false, require = 0)
    private static void pathtodivinity$applyPathwayAttributes(LivingEntity entity, BeyonderClass pathway, int sequence) {
        if (usesPathwayEffects(entity)) {
            BeyonderUtil.applyPathwayAttributes(entity, pathway, sequence);
        }
    }

    @Redirect(method = "onLivingJoinWorld",
            at = @At(value = "INVOKE", target = "Lnet/swimmingtuna/lotm/beyonder/api/BeyonderClass;addAllEvents(Lnet/minecraft/world/entity/LivingEntity;I)V"),
            remap = false, require = 0)
    private static void pathtodivinity$addEvents(BeyonderClass pathway, LivingEntity entity, int sequence) {
        if (usesPathwayEffects(entity)) {
            pathway.addAllEvents(entity, sequence);
        }
    }

    private static boolean usesPathwayEffects(LivingEntity entity) {
        BeyonderProfile profile = BeyonderProfiles.find(entity);
        return profile == null || profile.usesAbilities();
    }
}
