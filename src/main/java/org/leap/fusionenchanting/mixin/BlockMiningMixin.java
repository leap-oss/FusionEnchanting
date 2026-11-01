package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.FusionEnchanting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class BlockMiningMixin {
    @Shadow @Final protected ServerPlayer player;
    @Inject(method="destroyBlock",at=@At("HEAD"))
    private void fusion$before(BlockPos pos,CallbackInfoReturnable<Boolean> cir){FusionEnchanting.runtime().beginMine(player,pos);}
    @Inject(method="destroyBlock",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"))
    private void fusion$tool(CallbackInfoReturnable<Boolean> cir){FusionEnchanting.runtime().abilities.beforeMine(player);}
    @Inject(method="destroyBlock",at=@At("RETURN"))
    private void fusion$after(BlockPos pos,CallbackInfoReturnable<Boolean> cir){
        var runtime=FusionEnchanting.runtime();if(cir.getReturnValueZ())runtime.abilities.mined(player);runtime.mining.remove();
    }
}
