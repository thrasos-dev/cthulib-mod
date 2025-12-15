package net.pixeldreamstudios.cthulib.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.pixeldreamstudios.cthulib.Cthulib;
import net.pixeldreamstudios.cthulib.client.ModCollector;
import net.pixeldreamstudios.cthulib.config.PromoMessageConfig;

@Mod(Cthulib.MOD_ID)
public final class CthulibNeoForge {
    public CthulibNeoForge() {
        Cthulib.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModCollectorNeoForge.init();
            ModIconLoaderNeoForge.init();
            ModCollector.preloadModData();
            PromoMessageConfig.load();
        }
    }
}