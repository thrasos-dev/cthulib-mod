package net.pixeldreamstudios.cthulib.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mixin(Options.class)
public class OptionsMixin {

    @Inject(method = "load", at = @At("HEAD"))
    private void beforeLoadOptions(CallbackInfo ci) {
        CthuLibConfig config = CthuLibConfig.get();
        if (config.resetResourcePacks) {
            modifyOptionsFile();
        }
    }

    private void modifyOptionsFile() {
        try {
            Path optionsPath = Minecraft.getInstance()
                    .gameDirectory.toPath().resolve("options.txt");

            if (!Files.exists(optionsPath)) {
                return;
            }

            List<String> lines = Files.readAllLines(optionsPath);
            List<String> modifiedLines = new ArrayList<>();

            for (String line : lines) {
                if (line.startsWith("resourcePacks:")) {
                    modifiedLines.add("resourcePacks:[]");
                } else if (line.startsWith("incompatibleResourcePacks:")) {
                    modifiedLines.add("incompatibleResourcePacks:[]");
                } else {
                    modifiedLines.add(line);
                }
            }

            Files.write(optionsPath, modifiedLines);
        } catch (Exception e) {
        }
    }
}