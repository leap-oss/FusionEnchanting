package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.*;
import org.leap.fusionenchanting.api.FusionProjectile;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class ArrowMixin implements FusionProjectile {
    @Unique private ItemStack fusion$weapon=ItemStack.EMPTY;
    @Unique private float fusion$force;
    @Unique private boolean fusion$used;
    @Inject(method="<init>",at=@At("RETURN"))
    private void fusion$capture(CallbackInfo ci){var runtime=FusionEnchanting.runtime();var context=runtime.launch.get();if(context!=null&&runtime.items.read(context.weapon()).isPresent())fusion$weapon(context.weapon(),context.force());}
    @Inject(method="addAdditionalSaveData",at=@At("RETURN"))
    private void fusion$save(CompoundTag tag,CallbackInfo ci){
        AbstractArrow arrow=(AbstractArrow)(Object)this;if(fusion$weapon.isEmpty())return;
        CompoundTag own=new CompoundTag();own.put("weapon",fusion$weapon.save(arrow.registryAccess()));own.putFloat("force",fusion$force);own.putBoolean("used",fusion$used);tag.put("fusionenchanting_shot",own);
    }
    @Inject(method="readAdditionalSaveData",at=@At("RETURN"))
    private void fusion$load(CompoundTag tag,CallbackInfo ci){
        fusion$weapon=ItemStack.EMPTY;fusion$used=false;fusion$force=0;
        if(!tag.contains("fusionenchanting_shot",Tag.TAG_COMPOUND))return;CompoundTag own=tag.getCompound("fusionenchanting_shot");
        float force=own.getFloat("force");if(!Float.isFinite(force)||force<0||force>1)return;
        AbstractArrow arrow=(AbstractArrow)(Object)this;var weapon=ItemStack.parse(arrow.registryAccess(),own.getCompound("weapon"));
        if(weapon.isPresent()&&FusionEnchanting.runtime().items.read(weapon.get()).isPresent()){fusion$weapon=weapon.get();fusion$force=force;fusion$used=own.getBoolean("used");}
    }
    @Override public ItemStack fusion$weapon(){return fusion$weapon.copy();}
    @Override public void fusion$weapon(ItemStack weapon,float force){fusion$weapon=weapon.copy();fusion$force=Math.max(0,Math.min(1,force));}
    @Override public float fusion$force(){return fusion$force;}
    @Override public boolean fusion$used(){return fusion$used;}
    @Override public void fusion$used(boolean value){fusion$used=value;}
}
