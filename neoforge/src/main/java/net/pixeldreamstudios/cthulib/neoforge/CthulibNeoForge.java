package net.pixeldreamstudios.cthulib.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.pixeldreamstudios.cthulib.CthuLib;
import net.pixeldreamstudios.cthulib.client.CthuLibConfigScreen;
import net.pixeldreamstudios.cthulib.client.DataPreloader;
import net.pixeldreamstudios.cthulib.client.ModCollector;
import net.pixeldreamstudios.cthulib.config.PromoMessageConfig;

@Mod(CthuLib.MOD_ID)
public final class CthulibNeoForge {
    public CthulibNeoForge() {
        CthuLib.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModCollectorNeoForge.init();
            ModIconLoaderNeoForge.init();
            ModCollector.preloadModData();
            DataPreloader.preloadData();
            PromoMessageConfig.load();
            
            ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (mc, parent) -> new CthuLibConfigScreen(parent)
            );
        }
    }
}