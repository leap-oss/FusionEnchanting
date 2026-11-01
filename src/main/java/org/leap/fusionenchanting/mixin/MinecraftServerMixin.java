package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.FusionEnchanting;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method="tickServer",at=@At("TAIL"))
    private void fusion$tick(CallbackInfo ci){FusionEnchanting.runtime().tick((MinecraftServer)(Object)this);}
    @Inject(method="stopServer",at=@At("HEAD"))
    private void fusion$stop(CallbackInfo ci){FusionEnchanting.runtime().stop((MinecraftServer)(Object)this);}
}
