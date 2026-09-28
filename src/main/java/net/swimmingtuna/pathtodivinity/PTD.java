package net.swimmingtuna.pathtodivinity;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.swimmingtuna.lotm.client.Configs;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(PTD.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class PTD {

    public static final int NEW_STRUCTURE_SIZE = 512;
    public static Supplier<Boolean> fadeOut;
    public static Supplier<Integer> fadeTicks;

    public static Supplier<Double> maxBrightness;
    public static Supplier<Double> fadeRate = () -> maxBrightness.get() / fadeTicks.get();

    public static final String MOD_ID = "pathtodivinity";
    public static final Logger LOGGER = LogUtils.getLogger();


    public PTD() {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PTDConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PTDCommands::onCommandRegistration);
    }
}
