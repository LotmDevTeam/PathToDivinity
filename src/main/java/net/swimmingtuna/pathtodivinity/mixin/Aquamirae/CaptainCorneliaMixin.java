package net.swimmingtuna.pathtodivinity.mixin.Aquamirae;

import dev.obscuria.aquamirae.common.entity.CaptainCorneliaBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;

/**
 * Removes Captain Cornelia's "very low HP" wall.
 *
 * In {@link CaptainCorneliaBoss#baseTick()} she self-heals every tick once her health drops to 20 or
 * below:
 * <pre>
 *     if (this.getHealth() &lt;= 20.0F &amp;&amp; !this.hasEffect(MobEffects.WEAKNESS)) {
 *         this.heal(0.1F);
 *     }
 * </pre>
 * That's 0.1 HP/tick (2 HP/s) unless she has Weakness. (The 1.20.1 Forge Aquamirae healed 0.5 HP/tick
 * below 16.) We rewrite the 20.0F threshold to a value below any possible health, so the guard is never
 * true and the heal never fires. Health can never be negative, so -1.0F disables it completely. It is
 * the only 20.0F constant in {@code baseTick}.
 *
 * The rage/Levitation regen at low health is left untouched.
 */
@Mixin(value = CaptainCorneliaBoss.class)
public class CaptainCorneliaMixin {

    @ModifyConstant(method = "baseTick", constant = @Constant(floatValue = 20.0F))
    private float ptd$removeLowHealthRegen(float original) {
        return PTDBalance.CAPTAIN_CORNELIA_NO_LOW_HEALTH_REGEN.isOn() ? -1.0F : original;
    }
}
