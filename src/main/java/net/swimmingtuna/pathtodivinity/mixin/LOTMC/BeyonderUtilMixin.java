package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.MobAbilitySequenceContext;
import net.swimmingtuna.pathtodivinity.compat.PTDEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

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

    // Keyed by registry id so that none of the optional mobs' mods are loaded along with BeyonderUtil
    private static final Map<ResourceLocation, Integer> ENTITY_SEQUENCE_MAP = new HashMap<>();

    static {
        initializeEntitySequenceMap();
    }

    private static void initializeEntitySequenceMap() {
        // Sequence 9 entities
        ENTITY_SEQUENCE_MAP.put(PTDEntities.OVERGROWN_COLOSSUS, 9);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.WARPED_FUNGUSSUS, 9);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.KOBOLEDIATOR, 9);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.UMVUTHI, 9);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.MAW, 9);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.SKELETOSAURUS, 9);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.NIGHTMARE_STALKER, 9);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.GLUTTON_FISH, 9);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.WROUGHTNAUT, 9);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.DIRE_HOUND_LEADER, 9);

        // Sequence 8 entities
        ENTITY_SEQUENCE_MAP.put(PTDEntities.BLAST_CANNON, 8);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.FROSTBITTEN_GOLEM, 8);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.ENDERSENT, 8);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.DUSKROK, 8);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.MUTANT_SKELETON, 8);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.MUTANT_ENDERMAN, 8);
        //ENTITY_SEQUENCE_MAP.put(AMEntityRegistry.WARPED_MOSCO.get(), 8);
        ENTITY_SEQUENCE_MAP.put(EntityType.getKey(EntityType.ELDER_GUARDIAN), 8);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.SPIRITOF_CHAOS, 8);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.MOTHER_SPIDER, 8);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.HELLROK, 8);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.MUTANT_ZOMBIE, 8);

        // Sequence 7 entities
        ENTITY_SEQUENCE_MAP.put(PTDEntities.ANCIENT_GUARDIAN, 7);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.CORPSE_WARLOCK, 7);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.FROSTMAW, 7);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.MAZE_MOTHER, 7);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.WITHERED_ABOMINATION, 7);

        // Sequence 6 entities
        ENTITY_SEQUENCE_MAP.put(PTDEntities.NETHERITE_MONSTROSITY, 6);
        ENTITY_SEQUENCE_MAP.put(EntityType.getKey(EntityType.WITHER), 6);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.HEROBRINE, 6);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.LIFESTEALER, 6);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.LAVA_EATER, 6);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.SIR_PUMPKINHEAD, 6);

        // Sequence 5 entities
        ENTITY_SEQUENCE_MAP.put(PTDEntities.THE_HARBINGER, 5);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.CAPTAIN_CORNELIA, 5);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.POSESSED_PALADIN, 5);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.ACCURSED_LORD_BOSS, 5);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.RETURNING_KNIGHT, 5);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.ENDER_GUARDIAN, 5);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.MOONKNIGHT, 5);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.CHAOS_MONARCH, 5);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.DRAUGR_BOSS, 5);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.NIGHT_SHADE, 5);

        // Sequence 4 entities
        ENTITY_SEQUENCE_MAP.put(PTDEntities.CLOUD_GOLEM, 4);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.IGNIS, 4);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.SCYLLA, 4);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.MALEDICTUS, 4);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.GOB, 4);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.THE_LEVIATHAN, 4);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.NAMELESS_GUARDIAN, 4);

        // Sequence 3 entities
        ENTITY_SEQUENCE_MAP.put(PTDEntities.LORD_PUMPKINHEAD, 3);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.TRIAL_GUARDIAN, 3);

        // Sequence 2 entities
        ENTITY_SEQUENCE_MAP.put(PTDEntities.SUPER_SNIFFER, 2);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.GUNDALF, 2);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.DAY_STALKER, 2);
        ENTITY_SEQUENCE_MAP.put(PTDEntities.NIGHT_PROWLER, 2);

        // Sequence 1 entities
        ENTITY_SEQUENCE_MAP.put(PTDEntities.ULTRA_SNIFFER, 1);
    }

    @Inject(method = "getSequence", at = @At("HEAD"), cancellable = true)
    private static void injectCustomSequences(LivingEntity living, CallbackInfoReturnable<Integer> cir) {
        if (living == null) {
            cir.setReturnValue(10);
            return;
        }
        // While BeyonderEntityData is picking which abilities a mob may use, let LOTM resolve the
        // sequence itself so it comes from the /beyonderentity registration rather than the map
        // below. See BeyonderEntityDataMixin.
        if (MobAbilitySequenceContext.isSelectingAbilitiesFor(living)) {
            return;
        }
        Integer customSequence = ENTITY_SEQUENCE_MAP.get(PTDEntities.idOf(living));
        if (customSequence != null) {
            cir.setReturnValue(customSequence);
            return;
        }
        // Type-based, not living.getName(): a name tag would otherwise hand any mob a boss sequence.
        String entityName = PTDEntities.typeSearchText(living);
        String className = living.getClass().getSimpleName();

        if (living instanceof Mob) {
            if (entityName.contains("vessel")) {
                cir.setReturnValue(3);
                return;
            }
            if (PTDEntities.isHorseman(living)) {
                cir.setReturnValue(4);
                return;
            }
            if (entityName.contains("doomharbor")) {
                cir.setReturnValue(7);
                return;
            }
            if (entityName.contains("terrible") || entityName.contains("puny")) {
                cir.setReturnValue(8);
                return;
            }
            if (entityName.contains("plague_bringer")) {
                cir.setReturnValue(7);
                return;
            }
            if (entityName.contains("aero_guardian")) {
                cir.setReturnValue(8);
                return;
            }
            if (entityName.contains("dyrolian")) {
                cir.setReturnValue(6);
                return;
            }
        }
        if (className.equals("VoidBlossomEntity")) {
            cir.setReturnValue(6);
            return;
        }
        if (className.equals("LichEntity")) {
            cir.setReturnValue(7);
            return;
        }
        if (className.equals("GauntletEntity")) {
            cir.setReturnValue(7);
            return;
        }
    }
}