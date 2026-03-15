package net.pixeldreamstudios.cthulib.fabric;

import net.fabricmc.api.ModInitializer;
import net.pixeldreamstudios.cthulib.CthuLib;

public final class CthuLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CthuLib.init();
    }
}
