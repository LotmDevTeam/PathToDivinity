package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.swimmingtuna.lotm.item.BeyonderAbilities.SimpleAbilityItem;
import net.swimmingtuna.pathtodivinity.PTDUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** [items] banned_abilities: held-item casts fail, but the item stays in the inventory. */
@Mixin(value = SimpleAbilityItem.class, remap = false)
public class SimpleAbilityItemMixin {

    @Inject(method = "checkIfCanUseAbility(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void blockBannedAbility(LivingEntity livingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (PTDUtil.isAbilityBanned((Item) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(method = "inventoryTick", at = @At(value = "INVOKE",
            target = "Lnet/swimmingtuna/lotm/util/BeyonderUtil;canUseAbility(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;)Z"))
    private boolean keepBannedAbility(LivingEntity living, Item item, Operation<Boolean> original) {
        // shortcut: banned items are never removed by inventoryTick, even if the holder wouldn't own them; fine since they can't be used while banned
        return PTDUtil.isAbilityBanned(item) || original.call(living, item);
    }
}
