package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.FusionEnchanting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {
    @Inject(method="attack",at=@At("HEAD"))
    private void fusion$begin(CallbackInfo ci){if((Object)this instanceof ServerPlayer player)FusionEnchanting.runtime().beginAttack(player);}
    @Inject(method="attack",at=@At("RETURN"))
    private void fusion$end(CallbackInfo ci){if((Object)this instanceof ServerPlayer)FusionEnchanting.runtime().attack.remove();}
}
