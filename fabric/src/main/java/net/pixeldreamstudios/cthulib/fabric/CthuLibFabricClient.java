package net.pixeldreamstudios.cthulib.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.pixeldreamstudios.cthulib.client.ModCollector;
import net.pixeldreamstudios.cthulib.config.PromoMessageConfig;
import net.pixeldreamstudios.cthulib.util.client.PromoMessageHandler;

public final class CthuLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModCollectorFabric.init();
        ModIconLoaderFabric.init();
        ModCollector.preloadModData();
        PromoMessageConfig.load();
        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> {
                    client.execute(PromoMessageHandler::onJoinServer);
                });
    }
}