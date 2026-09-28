package net.swimmingtuna.pathtodivinity.mixin.Celestisynth;


import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.swimmingtuna.pathtodivinity.config.StartupConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.thecelestialworkshop.celestisynth.common.registry.CSItemTiers;
import org.thecelestialworkshop.celestisynth.common.registry.CSItems;
import org.thecelestialworkshop.celestisynth.common.registry.CSTags;

import java.util.List;

@Mixin(value = CSItemTiers.class, remap = false)
public class CelestisynthItemTiersMixin {

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/TierSortingRegistry;registerTier(Lnet/minecraft/world/item/Tier;Lnet/minecraft/resources/ResourceLocation;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/world/item/Tier;"))
    private static Tier redirectRegisterTier(Tier tier, net.minecraft.resources.ResourceLocation name, List<Object> after, List<Object> before) {
        // Runs while items register, before Forge configs load: values come from config/ptd/pathtodivinity-startup.toml.
        if (!StartupConfig.CELESTISYNTH_TIER_ENABLED) {
            return TierSortingRegistry.registerTier(tier, name, after, before);
        }
        ForgeTier modifiedTier = new ForgeTier(StartupConfig.CELESTISYNTH_TIER_LEVEL, StartupConfig.CELESTISYNTH_TIER_USES,
                StartupConfig.CELESTISYNTH_TIER_SPEED, StartupConfig.CELESTISYNTH_TIER_ATTACK_DAMAGE_BONUS,
                StartupConfig.CELESTISYNTH_TIER_ENCHANTMENT_VALUE, CSTags.Blocks.NEEDS_CELESTIAL_TOOL, () -> {
            return Ingredient.of(new ItemLike[]{(ItemLike) CSItems.CELESTIAL_CORE_HEATED.get()});
        });
        return TierSortingRegistry.registerTier(modifiedTier, name, after, before);
    }
}