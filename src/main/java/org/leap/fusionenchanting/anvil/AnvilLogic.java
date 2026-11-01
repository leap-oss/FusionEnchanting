package org.leap.fusionenchanting.anvil;

import org.leap.fusionenchanting.*;
import org.leap.fusionenchanting.api.FusionEvents;
import org.leap.fusionenchanting.api.FusionEvents.Operation;
import org.leap.fusionenchanting.core.*;
import org.leap.fusionenchanting.item.FusionItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Adds metadata/surcharge to an already-valid native merge; native menus still own consumption. */
public final class AnvilLogic {
    public record Plan(ItemStack left,ItemStack right,ItemStack nativeResult,ItemStack output,FusionDefinition recipe,
                       FusionTuning tuning,UUID identity,int cost,int rightCost,int revision,Operation operation){}
    public record Preview(ItemStack output,int cost,int rightCost,Plan plan){}
    private final FusionRuntime runtime;
    public AnvilLogic(FusionRuntime runtime){this.runtime=runtime;}
    public List<FusionDefinition> candidates(ItemStack left,ItemStack right,ItemStack result){
        FusionItems items=runtime.items;
        if(!FusionItems.book(left)||!FusionItems.book(right)||!FusionItems.book(result)||items.marked(left)||items.marked(right))return List.of();
        var a=FusionItems.enchantments(left);var b=FusionItems.enchantments(right);var merged=FusionItems.enchantments(result);
        return FusionCatalog.definitions().stream().filter(d->FusionItems.legalSources(left,d)&&FusionItems.legalSources(right,d)&&FusionItems.legalSources(result,d))
                .filter(d->FusionRules.matchesBooks(d,a,b,merged,false,runtime.tuning(d.id()))).toList();
    }
    public Preview prepare(ServerPlayer player,ItemStack left,ItemStack right,ItemStack nativeResult,int vanillaCost,int rightCost){
        FusionItems items=runtime.items;boolean a=items.marked(left),b=items.marked(right);
        if(nativeResult.isEmpty())return null;
        FusionDefinition recipe;UUID identity;Operation operation;int cost=vanillaCost;
        if(a||b){
            if(a&&b)return blocked(cost,rightCost);
            var state=items.read(a?left:right);
            if(state.isEmpty()||b&&(!FusionItems.book(right)||!runtime.allowed(player)||!runtime.tuning(state.get().definition().id()).enabled()))return blocked(cost,rightCost);
            recipe=state.get().definition();identity=state.get().instance();operation=b?Operation.APPLY_BOOK:Operation.MAINTAIN;
        }else{
            if(!runtime.allowed(player))return null;var matches=candidates(left,right,nativeResult);
            if(matches.size()!=1||cost<1)return null;
            recipe=matches.getFirst();identity=UUID.randomUUID();operation=Operation.CREATE_BOOK;
            cost=FusionRules.creationCost(cost,runtime.tuning(recipe.id()).extraLevels());rightCost=1;
        }
        if(cost<1)return null;
        ItemStack output=items.fuse(nativeResult,recipe,identity);
        if(items.read(output).isEmpty()||!player.getAbilities().instabuild&&cost>=40)return blocked(cost,rightCost);
        Plan plan=new Plan(left.copy(),right.copy(),nativeResult.copy(),output.copy(),recipe,runtime.tuning(recipe.id()),identity,cost,rightCost,runtime.revision(),operation);
        return new Preview(output,cost,rightCost,plan);
    }
    private static Preview blocked(int cost,int rightCost){return new Preview(ItemStack.EMPTY,cost,rightCost,null);}
    public boolean valid(Plan p,ServerPlayer player,ItemStack left,ItemStack right,ItemStack output,int cost,int rightCost){
        if(p==null||player.isRemoved()||!player.isAlive()||p.revision()!=runtime.revision()||!ItemStack.matches(p.left(),left)||!ItemStack.matches(p.right(),right)
                ||!ItemStack.matches(p.output(),output)||cost!=p.cost()||rightCost!=p.rightCost()||!p.tuning().equals(runtime.tuning(p.recipe().id()))
                ||!ItemStack.matches(runtime.items.fuse(p.nativeResult(),p.recipe(),p.identity()),output))return false;
        if(!player.getAbilities().instabuild&&(player.experienceLevel<cost||cost>=40))return false;
        return p.operation()==Operation.MAINTAIN||runtime.allowed(player)&&p.tuning().enabled();
    }
    public boolean permit(Plan plan,ServerPlayer player,ItemStack left,ItemStack right,ItemStack output,int cost,int rightCost){
        if(!valid(plan,player,left,right,output,cost,rightCost))return false;
        return plan.operation()==Operation.MAINTAIN||FusionEvents.allow(runtime.events.beforeCreate,new FusionEvents.Creation(player,plan.recipe(),plan.operation(),left,right,output,cost));
    }
}
