package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.MobAbilitySequenceContext;
import net.swimmingtuna.pathtodivinity.compat.PTDEntities;
import net.swimmingtuna.pathtodivinity.profile.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.profile.BeyonderProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = BeyonderUtil.class, remap = false)
public class BeyonderUtilMixin {

    @ModifyVariable(
            method = "ageHandlerTick",
            at = @At(value = "LOAD", ordinal = 0),
            name = "maxAge"
    )
    private static int modifyMaxAgeForUltraSniffer(int maxAge, LivingEvent.LivingTickEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (PTDEntities.ULTRA_SNIFFER.equals(PTDEntities.idOf(livingEntity))) {
            return 6000;
        }
        return maxAge;
    }

    @Inject(method = "getSequence", at = @At("HEAD"), cancellable = true)
    private static void injectCustomSequences(LivingEntity living, CallbackInfoReturnable<Integer> cir) {
        if (living == null) {
            cir.setReturnValue(10);
            return;
        }
        // While BeyonderEntityData is picking which abilities a mob may use, let LOTM resolve the
        // sequence itself so it comes from the /beyonderentity registration rather than the profile
        // sequence below. See BeyonderEntityDataMixin.
        if (MobAbilitySequenceContext.isSelectingAbilitiesFor(living)) {
            return;
        }
        // Profiles (data/<ns>/ptd_beyonders) set the sequence LOTM reports for a mob. On the client
        // this reads the copy ProfileSyncPacket sent, so both sides agree.
        BeyonderProfile profile = BeyonderProfiles.find(living);
        if (profile != null && profile.sequence() != null) {
            cir.setReturnValue(profile.sequence());
        }
    }
}