package net.pixeldreamstudios.cthulib.fabric;

import net.fabricmc.api.ModInitializer;
import net.pixeldreamstudios.cthulib.Cthulib;

public final class CthuLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Cthulib.init();
    }
}
