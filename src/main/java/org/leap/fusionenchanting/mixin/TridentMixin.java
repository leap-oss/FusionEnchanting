package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.FusionEnchanting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentItem.class)
public abstract class TridentMixin {
    @Inject(method="releaseUsing",at=@At("HEAD"))
    private void fusion$begin(ItemStack stack,Level level,LivingEntity owner,int remaining,CallbackInfo ci){if(!level.isClientSide)FusionEnchanting.runtime().beginLaunch(owner,stack,1);}
    @Inject(method="releaseUsing",at=@At("RETURN"))
    private void fusion$end(ItemStack stack,Level level,LivingEntity owner,int remaining,CallbackInfo ci){
        if(level.isClientSide)return;var runtime=FusionEnchanting.runtime();var context=runtime.launch.get();
        if(context!=null&&owner instanceof ServerPlayer player&&player.isAutoSpinAttack())runtime.abilities.riptide(player,context.weapon());runtime.launch.remove();
    }
}
