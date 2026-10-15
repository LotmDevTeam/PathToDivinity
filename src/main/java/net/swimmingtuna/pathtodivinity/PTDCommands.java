package net.swimmingtuna.pathtodivinity;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.swimmingtuna.lotm.LOTM;
import net.swimmingtuna.lotm.commands.BeyonderClassArgument;

public class PTDCommands {

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, LOTM.MOD_ID);
    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<BeyonderClassArgument>> BEYONDER_CLASS = ARGUMENT_TYPES.register("beyonder_class",
            () -> ArgumentTypeInfos.registerByClass(BeyonderClassArgument.class, SingletonArgumentInfo.contextFree(BeyonderClassArgument::beyonderClass)));



    /** Registered at HIGHEST priority (see PTD) so /beyonder check is added before LOTM's; see BeyonderCheckCommand. */
    public static void onCommandRegistration(RegisterCommandsEvent event) {
        HealthValidateCommand.register(event.getDispatcher());
        SequenceLockCommand.register(event.getDispatcher());
        BeyonderProfileCommand.register(event.getDispatcher());
        BeyonderCheckCommand.register(event.getDispatcher());
        AbilityBanCommand.register(event.getDispatcher());
    }
}
