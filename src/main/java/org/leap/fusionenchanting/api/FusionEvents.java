package org.leap.fusionenchanting.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.leap.fusionenchanting.core.FusionDefinition;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/** Server-thread veto hooks for protection/permission integrations, independent of Bukkit. */
public final class FusionEvents {
    public enum Operation { CREATE_BOOK, APPLY_BOOK, MAINTAIN }
    public record Creation(ServerPlayer player,FusionDefinition recipe,Operation operation,ItemStack left,ItemStack right,ItemStack output,int levels){
        public Creation{left=left.copy();right=right.copy();output=output.copy();}
        @Override public ItemStack left(){return left.copy();}
        @Override public ItemStack right(){return right.copy();}
        @Override public ItemStack output(){return output.copy();}
    }
    public record Ability(ServerPlayer player,String id,LivingEntity target){}
    public final CopyOnWriteArrayList<Predicate<Creation>> beforeCreate=new CopyOnWriteArrayList<>();
    public final CopyOnWriteArrayList<Predicate<Ability>> beforeAbility=new CopyOnWriteArrayList<>();
    public final CopyOnWriteArrayList<Predicate<ServerPlayer>> allowUse=new CopyOnWriteArrayList<>();
    public final CopyOnWriteArrayList<Predicate<ServerPlayer>> allowRecipes=new CopyOnWriteArrayList<>();
    public static <T> boolean allow(Iterable<Predicate<T>> callbacks,T context){
        // A failed protection callback is a veto, never permission to modify gameplay.
        try{for(var callback:callbacks)if(!callback.test(context))return false;return true;}
        catch(RuntimeException error){org.leap.fusionenchanting.FusionEnchanting.LOGGER.error("Fusion integration veto failed",error);return false;}
    }
}
