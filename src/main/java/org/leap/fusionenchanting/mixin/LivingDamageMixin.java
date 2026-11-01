package org.leap.fusionenchanting.mixin;

import org.leap.fusionenchanting.FusionEnchanting;
import org.leap.fusionenchanting.ability.FusionAbilities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class LivingDamageMixin {
    @Unique private FusionAbilities.Hit fusion$hit;
    @Unique private float fusion$health;
    @Inject(method="hurt",at=@At("HEAD"))
    private void fusion$before(DamageSource source,float damage,CallbackInfoReturnable<Boolean> cir){fusion$hit=null;fusion$health=((LivingEntity)(Object)this).getHealth();}
    // Native shield/invulnerability checks run first; armor/resistance still process the bonus.
    @ModifyArgs(method="hurt",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void fusion$bonus(Args args){
        LivingEntity target=(LivingEntity)(Object)this;if(target.level().isClientSide)return;
        float damage=args.get(1);if(damage<=0)return;
        fusion$hit=FusionEnchanting.runtime().abilities.plan(target,args.get(0));
        if(fusion$hit!=null)args.set(1,damage+(float)fusion$hit.bonus());
    }
    @Inject(method="hurt",at=@At("RETURN"))
    private void fusion$after(DamageSource source,float damage,CallbackInfoReturnable<Boolean> cir){
        LivingEntity target=(LivingEntity)(Object)this;var hit=fusion$hit;fusion$hit=null;
        if(!target.level().isClientSide&&cir.getReturnValueZ()&&target.getHealth()<fusion$health){
            FusionEnchanting.runtime().abilities.afterHit(hit);FusionEnchanting.runtime().abilities.damaged(target,source);
        }
    }
    @Inject(method="die",at=@At("RETURN"))
    private void fusion$kill(DamageSource source,CallbackInfo ci){LivingEntity target=(LivingEntity)(Object)this;if(!target.level().isClientSide&&target.isDeadOrDying())FusionEnchanting.runtime().abilities.killed(target,source);}
}
