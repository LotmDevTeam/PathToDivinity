package net.swimmingtuna.pathtodivinity;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(PTD.MOD_ID)
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class PTD {

    public static final int NEW_STRUCTURE_SIZE = 512;
    public static Supplier<Boolean> fadeOut;
    public static Supplier<Integer> fadeTicks;

    public static Supplier<Double> maxBrightness;
    public static Supplier<Double> fadeRate = () -> maxBrightness.get() / fadeTicks.get();

    public static final String MOD_ID = "pathtodivinity";
    public static final Logger LOGGER = LogUtils.getLogger();


    public PTD(IEventBus modEventBus, ModContainer modContainer) {
        PTDServerConfig.relocateLegacyCommonFile();
        modContainer.registerConfig(ModConfig.Type.SERVER, PTDServerConfig.SPEC, PTDServerConfig.FILE_NAME);
        modContainer.registerConfig(ModConfig.Type.SERVER, PTDBalance.SPEC, PTDBalance.FILE_NAME);
        modEventBus.addListener(PTDServerConfig::onConfigLoading);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PTDCommands::onCommandRegistration);
    }

    /**
     * Re-sends the command tree to every online player when the server config is edited in-game.
     *
     * <p>{@code /sequencelock} and {@code /beyonderprofile} are gated behind config flags via Brigadier
     * {@code .requires} predicates, but the client only receives a filtered copy of the tree on login and on
     * op changes. Without this, flipping the config while the world is open would leave the command missing
     * from tab-completion until the player relogged.
     */
    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != PTDServerConfig.SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        // Config files are reloaded off-thread; touching the player list has to happen on the server thread.
        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                server.getCommands().sendCommands(player);
            }
        });
    }
}
