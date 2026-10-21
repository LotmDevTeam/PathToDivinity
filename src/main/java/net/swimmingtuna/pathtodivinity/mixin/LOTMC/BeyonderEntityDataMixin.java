package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.swimmingtuna.lotm.events.LivingTickEvent;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.lotm.world.worlddata.BeyonderEntityData;
import net.swimmingtuna.pathtodivinity.BeyonderSummonCommand;
import net.swimmingtuna.pathtodivinity.MobAbilitySequenceContext;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if (profile != null && !profile.usesAbilities() && BeyonderSummonCommand.summonedAs(mob) == null) {
            ci.cancel();
        }
    }

    /** A {@code /beyonder summon} mob is a Beyonder by itself, whatever its type is registered as. */
    @Inject(method = "resolveGenericEntityPathway", at = @At("HEAD"), cancellable = true, remap = false)
    private static void pathtodivinity$summonedPathway(LivingEntity entity, ServerLevel level,
                                                      CallbackInfoReturnable<BeyonderUtil.PathwayAndSequence> cir) {
        BeyonderUtil.PathwayAndSequence summoned = BeyonderSummonCommand.summonedAs(entity);
        CompoundTag data = entity.getPersistentData();
        // Purification still applies, the same way LOTM applies it to registered types.
        if (summoned == null || data.getBoolean("purificationPathwayRemoved")) {
            return;
        }
        cir.setReturnValue(data.contains("purificationSequenceOverride")
                ? new BeyonderUtil.PathwayAndSequence(summoned.pathway(), data.getInt("purificationSequenceOverride"))
                : summoned);
    }

    /** LOTM only ticks spirituality and abilities for registered types; let summoned mobs through too. */
    @WrapOperation(method = "regenerateSpirituality", at = @At(value = "INVOKE",
            target = "Lnet/swimmingtuna/lotm/world/worlddata/BeyonderEntityData;getStringForEntity(Lnet/minecraft/world/entity/EntityType;)Ljava/lang/String;"),
            remap = false)
    private static String pathtodivinity$tickSummoned(BeyonderEntityData data, EntityType<?> type, Operation<String> original,
                                                     @Local(argsOnly = true) LivingTickEvent event) {
        return BeyonderSummonCommand.registrationFor(original.call(data, type), event.getEntity());
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
