package net.swimmingtuna.pathtodivinity.mixin.LOTMC;

import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.world.worlddata.Sequence0Data;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** LOTM only exposes the Sequence 0 holder; the inactivity tracker also needs to list Sequence 1 and 2 holders. */
@Mixin(value = Sequence0Data.class, remap = false)
public interface Sequence0DataAccessor {

    /** Sequence -> pathway name -> holders. */
    @Accessor(value = "sequenceHolders", remap = false)
    Map<Integer, Map<String, Set<UUID>>> pathtodivinity$sequenceHolders();

    @Invoker(value = "pathwayName", remap = false)
    static String pathtodivinity$pathwayName(BeyonderClass pathway) {
        throw new AssertionError();
    }
}
