package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.FusionEnchanting;
import org.leap.fusionenchanting.api.FusionPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements FusionPlayer {
    @Unique private boolean fusion$introduced;
    @Unique private CompoundTag fusion$stats=new CompoundTag();
    @Inject(method="addAdditionalSaveData",at=@At("RETURN"))
    private void fusion$save(CompoundTag tag,CallbackInfo ci){
        tag.putBoolean("fusionenchanting_guide_seen",fusion$introduced);
        if(!fusion$stats.isEmpty())tag.put("fusionenchanting_stats",fusion$stats.copy());
    }
    @Inject(method="readAdditionalSaveData",at=@At("RETURN"))
    private void fusion$load(CompoundTag tag,CallbackInfo ci){
        fusion$introduced=tag.getBoolean("fusionenchanting_guide_seen");
        fusion$stats=tag.contains("fusionenchanting_stats",Tag.TAG_COMPOUND)?tag.getCompound("fusionenchanting_stats").copy():new CompoundTag();
    }
    @Inject(method="restoreFrom",at=@At("RETURN"))
    private void fusion$respawn(ServerPlayer old,boolean alive,CallbackInfo ci){
        fusion$introduced=((FusionPlayer)old).fusion$introduced();
        fusion$stats=((FusionPlayer)old).fusion$stats().copy();
        FusionEnchanting.runtime().abilities.teleported((ServerPlayer)(Object)this);
    }
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",at=@At("HEAD"))
    private void fusion$teleport(CallbackInfo ci){FusionEnchanting.runtime().abilities.teleported((ServerPlayer)(Object)this);}
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",at=@At("HEAD"))
    private void fusion$relativeTeleport(CallbackInfoReturnable<Boolean> cir){FusionEnchanting.runtime().abilities.teleported((ServerPlayer)(Object)this);}
    @Inject(method="teleportTo(DDD)V",at=@At("HEAD"))
    private void fusion$localTeleport(CallbackInfo ci){FusionEnchanting.runtime().abilities.teleported((ServerPlayer)(Object)this);}
    @Override public boolean fusion$introduced(){return fusion$introduced;}
    @Override public void fusion$introduced(boolean value){fusion$introduced=value;}
    @Override public CompoundTag fusion$stats(){return fusion$stats;}
    @Override public void fusion$stats(CompoundTag value){fusion$stats=value==null?new CompoundTag():value.copy();}
}
