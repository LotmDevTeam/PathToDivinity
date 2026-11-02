package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.minecraft.world.entity.LivingEntity;
import net.swimmingtuna.lotm.ability.Seq;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Seq.class, remap = false)
public interface SeqAccessor {

    @Invoker(value = "<init>", remap = false)
    static Seq pathtodivinity$create(BeyonderClass pathway, int sequence, boolean healthGuess, LivingEntity owner) {
        throw new AssertionError();
    }
}
