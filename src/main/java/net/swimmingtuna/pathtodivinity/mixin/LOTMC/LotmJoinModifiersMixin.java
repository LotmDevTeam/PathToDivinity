package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.event.mob.MobEvents;
import net.swimmingtuna.lotm.world.worlddata.BeyonderEntityData;
import net.swimmingtuna.pathtodivinity.BeyonderSummonCommand;
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
 * <p>{@code Pathways.applyPathwayAttributes} also applies the pathway's modifiers, and
 * {@code startPassives} replaced the old pathway events.
 */
@Mixin(value = MobEvents.class, remap = false)
public class LotmJoinModifiersMixin {

    @Redirect(method = "onLivingJoinWorld",
            at = @At(value = "INVOKE", target = "Lnet/swimmingtuna/lotm/beyonder/Pathways;applyPathwayAttributes(Lnet/minecraft/world/entity/LivingEntity;Lnet/swimmingtuna/lotm/beyonder/api/BeyonderClass;I)V"),
            remap = false)
    private static void pathtodivinity$applyPathwayAttributes(LivingEntity entity, BeyonderClass pathway, int sequence) {
        if (usesPathwayEffects(entity)) {
            Pathways.applyPathwayAttributes(entity, pathway, sequence);
        }
    }

    @Redirect(method = "onLivingJoinWorld",
            at = @At(value = "INVOKE", target = "Lnet/swimmingtuna/lotm/beyonder/api/BeyonderClass;startPassives(Lnet/minecraft/world/entity/LivingEntity;I)V"),
            remap = false)
    private static void pathtodivinity$startPassives(BeyonderClass pathway, LivingEntity entity, int sequence) {
        if (usesPathwayEffects(entity)) {
            pathway.startPassives(entity, sequence);
        }
    }

    /** A {@code /beyonder summon} mob gets the same join setup (full spirituality, attributes, events) as a registered type. */
    @WrapOperation(method = "onLivingJoinWorld", at = @At(value = "INVOKE",
            target = "Lnet/swimmingtuna/lotm/world/worlddata/BeyonderEntityData;getStringForEntity(Lnet/minecraft/world/entity/EntityType;)Ljava/lang/String;"),
            remap = false)
    private static String pathtodivinity$joinSummoned(BeyonderEntityData data, EntityType<?> type, Operation<String> original,
                                                     @Local(argsOnly = true) EntityJoinLevelEvent event) {
        return BeyonderSummonCommand.registrationFor(original.call(data, type), event.getEntity());
    }

    private static boolean usesPathwayEffects(LivingEntity entity) {
        BeyonderProfile profile = BeyonderProfiles.find(entity);
        return profile == null || profile.usesAbilities() || BeyonderSummonCommand.summonedAs(entity) != null;
    }
}
