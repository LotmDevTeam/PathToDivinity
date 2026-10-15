package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.MobAbilitySequenceContext;
import net.swimmingtuna.pathtodivinity.PTDUtil;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * The Forge version also lengthened Terramity's Ultra Sniffer's lifespan in {@code ageHandlerTick};
 * Terramity has no NeoForge release, so that injection was dropped.
 */
@Mixin(value = BeyonderUtil.class, remap = false)
public class BeyonderUtilMixin {

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

    /** [items] banned_abilities: keybind, wheel and hotbar casts. */
    @Inject(method = "useAbilityItem", at = @At("HEAD"), cancellable = true)
    private static void blockBannedCast(Player player, Item item, InteractionHand hand, CallbackInfo ci) {
        if (PTDUtil.isAbilityBanned(item)) {
            ci.cancel();
        }
    }

    /** [items] banned_abilities: passives, toggles and ability entities. */
    @Inject(method = "canUseAbility(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;)Z", at = @At("HEAD"), cancellable = true)
    private static void blockBannedAbility(LivingEntity living, Item item, CallbackInfoReturnable<Boolean> cir) {
        if (PTDUtil.isAbilityBanned(item)) {
            cir.setReturnValue(false);
        }
    }
}