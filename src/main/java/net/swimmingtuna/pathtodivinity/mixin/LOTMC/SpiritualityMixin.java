package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.swimmingtuna.lotm.beyonder.Spirituality;
import net.swimmingtuna.lotm.world.worlddata.BeyonderEntityData;
import net.swimmingtuna.pathtodivinity.BeyonderSummonCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Spirituality.class, remap = false)
public class SpiritualityMixin {

    /** LOTM keeps no spirituality for mobs of unregistered types; a {@code /beyonder summon} mob gets it anyway. */
    @WrapOperation(method = {
            "getSpirituality(Lnet/minecraft/world/entity/LivingEntity;)I",
            "addSpirituality(Lnet/minecraft/world/entity/LivingEntity;I)V",
            "setSpirituality(Lnet/minecraft/world/entity/LivingEntity;I)V"},
            at = @At(value = "INVOKE",
                    target = "Lnet/swimmingtuna/lotm/world/worlddata/BeyonderEntityData;getStringForEntity(Lnet/minecraft/world/entity/EntityType;)Ljava/lang/String;"),
            remap = false)
    private static String pathtodivinity$summonedSpirituality(BeyonderEntityData data, EntityType<?> type, Operation<String> original,
                                                             @Local(argsOnly = true) LivingEntity living) {
        return BeyonderSummonCommand.registrationFor(original.call(data, type), living);
    }
}
