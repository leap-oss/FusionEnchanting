package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.FusionEnchanting;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemWearMixin {
    @Unique private LivingEntity fusion$wearer;
    @Inject(method="hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",at=@At("HEAD"))
    private void fusion$owner(int amount,ServerLevel level,LivingEntity wearer,Consumer<Item> broken,CallbackInfo ci){fusion$wearer=wearer;}
    @Redirect(method="hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int fusion$wear(ServerLevel level,ItemStack item,int amount){
        int nativeWear=EnchantmentHelper.processDurabilityChange(level,item,amount);
        return nativeWear>0&&fusion$wearer instanceof ServerPlayer player&&FusionEnchanting.runtime().abilities.avoidWear(player,item)?0:nativeWear;
    }
    @Inject(method="hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",at=@At("RETURN"))
    private void fusion$clear(CallbackInfo ci){fusion$wearer=null;}
}
