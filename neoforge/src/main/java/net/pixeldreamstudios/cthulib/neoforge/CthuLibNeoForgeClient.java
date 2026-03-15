package net.pixeldreamstudios.cthulib.neoforge;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.pixeldreamstudios.cthulib.CthuLib;
import net.pixeldreamstudios.cthulib.util.client.PromoMessageHandler;

@Mod(CthuLib.MOD_ID)
public final class CthuLibNeoForgeClient {
    @SubscribeEvent
    public static void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        PromoMessageHandler.onJoinServer();
    }
}