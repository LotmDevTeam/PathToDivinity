package net.swimmingtuna.pathtodivinity;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.swimmingtuna.lotm.LOTM;
import net.swimmingtuna.lotm.commands.BeyonderClassArgument;

public class PTDCommands {

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES = DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, LOTM.MOD_ID);
    public static final RegistryObject<SingletonArgumentInfo<BeyonderClassArgument>> BEYONDER_CLASS = ARGUMENT_TYPES.register("beyonder_class",
            () -> ArgumentTypeInfos.registerByClass(BeyonderClassArgument.class, SingletonArgumentInfo.contextFree(BeyonderClassArgument::beyonderClass)));



    /** Registered at HIGHEST priority (see PTD) so /beyonder check is added before LOTM's; see BeyonderCheckCommand. */
    public static void onCommandRegistration(RegisterCommandsEvent event) {
        HealthValidateCommand.register(event.getDispatcher());
        BeyonderCheckCommand.register(event.getDispatcher());
    }
}
