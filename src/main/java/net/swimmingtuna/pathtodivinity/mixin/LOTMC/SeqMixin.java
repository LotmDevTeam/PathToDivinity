package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.world.entity.LivingEntity;
import net.swimmingtuna.lotm.ability.Seq;
import net.swimmingtuna.lotm.beyonder.MobBeyonders;
import net.swimmingtuna.pathtodivinity.MobAbilitySequenceContext;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfiles;
import net.swimmingtuna.pathtodivinity.beyonders.ReleasedTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every LOTM sequence read goes through {@code Seq.of}, which caches {@code compute}'s result per entity. */
@Mixin(value = Seq.class, remap = false)
public abstract class SeqMixin {

    @Shadow
    private static Seq compute(LivingEntity living) {
        throw new AssertionError();
    }

    /**
     * While BeyonderEntityData is picking which abilities a mob may use, skip the cache (it holds the profile
     * sequence) so the sequence comes from the /beyonderentity registration. See BeyonderEntityDataMixin.
     */
    @Inject(method = "of(Lnet/minecraft/world/entity/LivingEntity;)Lnet/swimmingtuna/lotm/ability/Seq;", at = @At("HEAD"), cancellable = true)
    private static void pathtodivinity$registeredSequenceForAbilities(LivingEntity entity, CallbackInfoReturnable<Seq> cir) {
        if (MobAbilitySequenceContext.isSelectingAbilitiesFor(entity)) {
            cir.setReturnValue(compute(entity));
        }
    }

    /**
     * Profiles (data/<ns>/ptd_beyonders) set the sequence LOTM reports for a mob. On the client this reads the
     * copy ProfileSyncPacket sent, so both sides agree.
     */
    @Inject(method = "compute", at = @At("RETURN"), cancellable = true)
    private static void pathtodivinity$profileSequence(LivingEntity living, CallbackInfoReturnable<Seq> cir) {
        if (MobAbilitySequenceContext.isSelectingAbilitiesFor(living)) {
            return;
        }
        // A mob given a pathway of its own (LOTM's /beyonderentity summon) keeps that sequence over its type's profile,
        // and so does every mob of a type an admin mapped by hand with /beyonderentity (LOTM #203).
        if (MobBeyonders.hasGrant(living) || ReleasedTypes.isReleased(living)) {
            return;
        }
        BeyonderProfile profile = BeyonderProfiles.find(living);
        if (profile != null && profile.sequence() != null) {
            cir.setReturnValue(SeqAccessor.pathtodivinity$create(cir.getReturnValue().pathway(), profile.sequence(), false, living));
        }
    }
}
