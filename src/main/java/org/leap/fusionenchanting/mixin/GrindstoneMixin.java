package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.FusionEnchanting;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMixin {
    @Shadow @Final private Container resultSlots;
    @Shadow @Final private Container repairSlots;
    @Inject(method="createResult",at=@At("RETURN"))
    private void fusion$clean(CallbackInfo ci){
        var items=FusionEnchanting.runtime().items;boolean a=items.marked(repairSlots.getItem(0)),b=items.marked(repairSlots.getItem(1));
        if(a&&b)resultSlots.setItem(0,ItemStack.EMPTY);
        else if((a||b)&&!resultSlots.getItem(0).isEmpty())resultSlots.setItem(0,items.strip(resultSlots.getItem(0)));
    }
}
