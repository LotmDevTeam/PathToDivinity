package net.swimmingtuna.pathtodivinity.mixin.Aquamirae;

import com.obscuria.aquamirae.common.entities.CaptainCornelia;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;

/**
 * Removes Captain Cornelia's "very low HP" wall.
 *
 * In {@link CaptainCornelia#baseTick()} she self-heals every tick once her health drops to 16 or
 * below:
 * <pre>
 *     if (this.getHealth() &lt;= 16.0F &amp;&amp; !this.hasEffect(MobEffects.WEAKNESS)) {
 *         this.heal(0.5F);
 *     }
 * </pre>
 * That's 0.5 HP/tick (10 HP/s) unless she has Weakness, which makes her nearly impossible to finish
 * off. We rewrite the 16.0F threshold to a value below any possible health, so the guard is never
 * true and the heal never fires. Health can never be negative, so -1.0F disables it completely.
 *
 * The rage/Levitation regen (the separate {@code heal(1.0F)} at &lt;25% HP) is left untouched.
 */
@Mixin(value = CaptainCornelia.class)
public class CaptainCorneliaMixin {

    @ModifyConstant(method = "baseTick", constant = @Constant(floatValue = 16.0F))
    private float ptd$removeLowHealthRegen(float original) {
        return PTDBalance.CAPTAIN_CORNELIA_NO_LOW_HEALTH_REGEN.isOn() ? -1.0F : original;
    }
}
