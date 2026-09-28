package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.lotm.world.worlddata.BeyonderEntityData;
import net.swimmingtuna.pathtodivinity.MobAbilitySequenceContext;
import net.swimmingtuna.pathtodivinity.profile.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.profile.BeyonderProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Makes mobs pick their abilities at the sequence they are registered with in
 * {@link BeyonderEntityData}, i.e. the one given to
 * {@code /beyonderentity add <entity> <pathway> <sequence>}, instead of the sequence from
 * {@code BeyonderUtilMixin}'s entity map.
 *
 * <p>{@code selectAndUseAbility} builds its candidate list from
 * {@code BeyonderUtil.getAbilities(mob)}, which gates every ability on
 * {@code BeyonderUtil.getSequence(mob)}. Because {@code BeyonderUtilMixin} overrides that method
 * for all callers, a mob registered as e.g. {@code lotm:spectator 3} was still handed the
 * sequence 1 ability set (the ultra sniffer being the obvious case). The pathway half of the
 * decision already came from {@link BeyonderEntityData}, so the two halves disagreed.
 *
 * <p>Only this one call is scoped: the mapped sequence still applies everywhere else (spirituality
 * regen, ability cooldown length, damage scaling, other mods reading the sequence).
 */
@Mixin(value = BeyonderEntityData.class, remap = false)
public class BeyonderEntityDataMixin {

    /** Bosses whose profile says {@code "ability_sequence": -1} are Beyonders that never use abilities. */
    @Inject(method = "selectAndUseAbility(Lnet/minecraft/world/entity/Mob;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void pathtodivinity$skipAbilitiesWhenDisabled(Mob mob, CallbackInfo ci) {
        BeyonderProfile profile = BeyonderProfiles.find(mob);
        if (profile != null && !profile.usesAbilities()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "selectAndUseAbility(Lnet/minecraft/world/entity/Mob;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/swimmingtuna/lotm/util/BeyonderUtil;getAbilities(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/List;"
            ),
            remap = false
    )
    private static List<Item> pathtodivinity$abilitiesAtRegisteredSequence(LivingEntity entity) {
        MobAbilitySequenceContext.begin(entity);
        try {
            return BeyonderUtil.getAbilities(entity);
        } finally {
            MobAbilitySequenceContext.end();
        }
    }
}
