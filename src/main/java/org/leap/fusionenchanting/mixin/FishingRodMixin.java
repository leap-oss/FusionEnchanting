package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.api.FusionFishing;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingRodItem.class)
public abstract class FishingRodMixin {
    @Inject(method="use",at=@At("RETURN"))
    private void fusion$cast(Level level,Player actor,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir){
        if(actor instanceof ServerPlayer player&&player.fishing instanceof FusionFishing hook&&hook.fusion$rod().isEmpty())hook.fusion$rod(player.getItemInHand(hand));
    }
}
