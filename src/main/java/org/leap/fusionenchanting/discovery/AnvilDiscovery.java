package org.leap.fusionenchanting.discovery;

import org.leap.fusionenchanting.FusionRuntime;
import org.leap.fusionenchanting.api.*;
import org.leap.fusionenchanting.core.*;
import org.leap.fusionenchanting.item.FusionItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Observes open native menus; never edits any input, result or XP. */
public final class AnvilDiscovery {
    private record Hint(String text,String filter){}
    private static final class Session{AnvilMenu menu;String last="",link="";int nextHint,nextLink;Session(AnvilMenu menu){this.menu=menu;}}
    private final FusionRuntime runtime;
    private final Map<UUID,Session> sessions=new HashMap<>();
    public AnvilDiscovery(FusionRuntime runtime){this.runtime=runtime;}
    public void tick(ServerPlayer player){
        if(!(player.containerMenu instanceof AnvilMenu menu)||!runtime.allowed(player)||!runtime.recipes(player)){
            Session old=sessions.remove(player.getUUID());if(old!=null&&!old.last.isEmpty())player.displayClientMessage(Component.empty(),true);return;
        }
        var state=(FusionPlayer)player;
        if(!state.fusion$introduced()){
            player.sendSystemMessage(Component.literal("Fusion: enchanted book + enchanted book → fusion book; apply it to equipment in an anvil. ").withStyle(ChatFormatting.GOLD).append(link("[Recipe guide]","")));state.fusion$introduced(true);
        }
        Session session=sessions.computeIfAbsent(player.getUUID(),key->new Session(menu));
        if(session.menu!=menu){session=new Session(menu);sessions.put(player.getUUID(),session);}
        Hint hint=hint(player,menu);if(hint==null){if(!session.last.isEmpty())player.displayClientMessage(Component.empty(),true);session.last="";return;}
        if(hint.text().equals(session.last)||runtime.tick()<session.nextHint)return;
        player.displayClientMessage(Component.literal(hint.text()).withStyle(ChatFormatting.YELLOW),true);session.last=hint.text();session.nextHint=runtime.tick()+20;
        if(hint.filter()!=null&&!hint.filter().equals(session.link)&&runtime.tick()>=session.nextLink){
            player.sendSystemMessage(Component.literal("Fusion: "+hint.text()+" ").withStyle(ChatFormatting.GRAY).append(link("[View recipes]",hint.filter())));session.link=hint.filter();session.nextLink=runtime.tick()+200;
        }
    }
    private Hint hint(ServerPlayer player,AnvilMenu menu){
        ItemStack left=menu.getSlot(0).getItem(),right=menu.getSlot(1).getItem(),result=menu.getSlot(2).getItem();int cost=((FusionAnvil)menu).fusion$getCost();var output=runtime.items.read(result);
        if(output.isPresent())return new Hint((FusionItems.book(result)?"Book ":"Apply ")+output.get().definition().name()+" "+FusionLevels.roman(output.get().level())+" • "+cost+" levels"+(!player.getAbilities().instabuild&&player.experienceLevel<cost?" (not enough XP)":""),null);
        if(runtime.items.marked(left)||runtime.items.marked(right)){
            var state=runtime.items.read(runtime.items.marked(right)?right:left);
            if(state.isPresent()&&runtime.items.marked(right)&&!left.isEmpty()&&!FusionItems.accepts(state.get().definition().equipment(),left))return new Hint("Book "+state.get().definition().name()+" applies to: "+state.get().definition().equipment().label(),state.get().definition().id());return null;
        }
        if(FusionItems.book(left)&&right.isEmpty()||FusionItems.book(right)&&left.isEmpty()){
            var sources=FusionItems.enchantments(FusionItems.book(left)?left:right);
            for(var d:FusionCatalog.definitions())if(runtime.tuning(d.id()).enabled()){
                var source=d.ingredients().keySet().stream().filter(sources::containsKey).sorted().findFirst();
                if(source.isPresent())return new Hint("Full-tier example: "+RecipeText.equation(d),source.get());
            }return null;
        }
        if(!FusionItems.book(left)||!FusionItems.book(right))return null;
        var a=FusionItems.enchantments(left);var b=FusionItems.enchantments(right);
        var pairs=FusionCatalog.definitions().stream().filter(d->runtime.tuning(d.id()).enabled()).filter(d->{var keys=new ArrayList<>(d.ingredients().keySet());return a.containsKey(keys.get(0))&&b.containsKey(keys.get(1))||a.containsKey(keys.get(1))&&b.containsKey(keys.get(0));}).toList();
        if(pairs.isEmpty())return new Hint("These books have no enabled fusion recipe.","");
        if(cost>=40&&!player.getAbilities().instabuild)return new Hint("Fusion follows the vanilla anvil Too Expensive limit.",pairs.getFirst().id());
        if(runtime.anvil.candidates(left,right,result).size()>1)return new Hint("Multiple fusion recipes match: the result stays vanilla. Use books with one clear pair.","");
        if(pairs.size()==1)return new Hint("Full-tier recipe: "+RecipeText.equation(pairs.getFirst()),pairs.getFirst().id());
        return new Hint("No single fusion qualifies; check the source books.","");
    }
    private static MutableComponent link(String label,String filter){return Component.literal(label).withStyle(s->s.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fusion guide"+(filter.isEmpty()?"":" "+filter))));}
    public void clear(){sessions.clear();}
    public void prune(MinecraftServer server){sessions.keySet().removeIf(id->server.getPlayerList().getPlayer(id)==null);}
}
