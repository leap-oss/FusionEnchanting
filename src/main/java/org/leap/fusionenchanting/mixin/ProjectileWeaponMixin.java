package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.FusionEnchanting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponMixin {
    @Inject(method="shoot",at=@At("HEAD"))
    private void fusion$begin(ServerLevel level,LivingEntity owner,InteractionHand hand,ItemStack weapon,List<ItemStack> ammunition,float velocity,float spread,boolean critical,LivingEntity target,CallbackInfo ci){FusionEnchanting.runtime().beginLaunch(owner,weapon,Math.min(1,velocity/3));}
    @Inject(method="shoot",at=@At("RETURN"))
    private void fusion$end(CallbackInfo ci){FusionEnchanting.runtime().launch.remove();}
}
