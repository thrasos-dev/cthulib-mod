package net.pixeldreamstudios.cthulib.mixin.client;

import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.pixeldreamstudios.cthulib.client.RawImageTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadableResourceManager.class)
public class ReloadableResourceManagerMixin {

    @Inject(method = "createReload", at = @At("RETURN"))
    private void cthulib$resetRawTextures(CallbackInfoReturnable<ReloadInstance> cir) {
        RawImageTextures.onResourceReload();
    }
}
