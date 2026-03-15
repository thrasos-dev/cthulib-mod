package net.pixeldreamstudios.cthulib.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.pixeldreamstudios.cthulib.client.CthuLibConfigScreen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CthuLibConfigScreen::new;
    }
}
