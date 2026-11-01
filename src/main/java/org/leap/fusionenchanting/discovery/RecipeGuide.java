package org.leap.fusionenchanting.discovery;

import org.leap.fusionenchanting.FusionRuntime;
import org.leap.fusionenchanting.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;
import java.util.*;

/** Optional native book: tier recipes and live effects, never an item granted to the player. */
public final class RecipeGuide {
    private record Line(String text,boolean title,ClickEvent click){}
    private final FusionRuntime runtime;
    private RecipeGui gui;
    public RecipeGuide(FusionRuntime runtime){this.runtime=runtime;}
    public void attachGui(RecipeGui gui){this.gui=gui;}
    public RecipeGui gui(){return gui;}
    public boolean open(ServerPlayer player,String filter){
        if(gui!=null)return gui.open(player,filter);
        return openBook(player,filter);
    }
    public boolean openBook(ServerPlayer player,String filter){
        if(!runtime.recipes(player))return false;var result=build(filter);if(result.isEmpty())return false;
        player.closeContainer();int slot=36+player.getInventory().selected;ItemStack original=player.getMainHandItem().copy();
        player.connection.send(new ClientboundContainerSetSlotPacket(0,player.inventoryMenu.incrementStateId(),slot,result.get()));
        player.connection.send(new ClientboundOpenBookPacket(InteractionHand.MAIN_HAND));
        player.connection.send(new ClientboundContainerSetSlotPacket(0,player.inventoryMenu.incrementStateId(),slot,original));return true;
    }
    public Optional<ItemStack> build(String query){
        String filter=query==null?"":query.toLowerCase(Locale.ROOT);
        var recipes=FusionCatalog.definitions().stream().filter(d->runtime.tuning(d.id()).enabled())
                .filter(d->filter.isEmpty()||d.id().equals(filter)||d.ingredients().containsKey(filter)).toList();
        if(recipes.isEmpty())return Optional.empty();List<Component> pages=new ArrayList<>();
        if(recipes.size()==1&&recipes.getFirst().id().equals(filter)){
            FusionDefinition d=recipes.getFirst();
            for(int tier=1;tier<=3;tier++){
                var sources=FusionLevels.example(d,tier);if(sources.isEmpty())continue;
                metadata(pages,d,tier,sources.get(),false);
                var profile=FusionLevels.profile(d,runtime.tuning(d.id()),tier);
                List<Line> effect=new ArrayList<>();add(effect,d.name()+" "+FusionLevels.roman(tier),true);add(effect,"");
                add(effect,"Effects:");add(effect,FusionDescriptions.describe(d,profile));add(effect,"");
                add(effect,"Limits:");add(effect,d.limitation());
                if(profile.chance()<1)add(effect,"Chance: "+FusionDescriptions.number(profile.chance()*100)+"%");
                append(pages,effect);
            }
        }else{
            pages.add(introduction());pages.add(Component.empty());Map<String,Integer> firstPages=new LinkedHashMap<>();
            for(var d:recipes){firstPages.putIfAbsent(group(d.equipment()),pages.size()+1);metadata(pages,d,3,d.ingredients(),true);}
            List<Line> index=new ArrayList<>();add(index,"Recipe catalog",true);add(index,"");
            firstPages.forEach((name,page)->index.add(new Line(name,false,new ClickEvent(ClickEvent.Action.CHANGE_PAGE,Integer.toString(page)))));
            add(index,"");add(index,recipes.size()+" active recipes");pages.set(1,component(index));
        }
        if(pages.size()>100)throw new IllegalStateException("Guide exceeds the native book page limit");
        ItemStack book=new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough("Fusion Guide"),"LEAP",0,pages.stream().map(Filterable::passThrough).toList(),true));return Optional.of(book);
    }
    private void metadata(List<Component> pages,FusionDefinition d,int tier,Map<String,Integer> sources,boolean detailLink){
        List<Line> lines=new ArrayList<>();add(lines,d.name()+" "+FusionLevels.roman(tier),true);add(lines,"");add(lines,"Source books:");
        var sorted=sources.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.reverseOrder())).toList();
        for(int i=0;i<sorted.size();i++){if(i>0)add(lines,"+");var e=sorted.get(i);add(lines,RecipeText.ingredient(e.getKey(),e.getValue()));}
        add(lines,"Applies to:");add(lines,d.equipment().label());add(lines,"XP: vanilla +"+runtime.tuning(d.id()).extraLevels());
        if(detailLink)lines.add(new Line("[Levels & stats]",false,new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fusion guide "+d.id())));
        append(pages,lines);
    }
    private static Component introduction(){
        List<Line> lines=new ArrayList<>();add(lines,"FUSION ENCHANT",true);add(lines,"");
        for(String text:List.of("Books + books","= fusion book.","Source levels","set fusion tier.","","Create: vanilla +","recipe fee.","Apply: vanilla.","","Browse recipes:"))add(lines,text);
        lines.add(new Line("[View recipes]",false,new ClickEvent(ClickEvent.Action.CHANGE_PAGE,"2")));return component(lines);
    }
    private static void add(List<Line> lines,String text){add(lines,text,false);}
    private static void add(List<Line> lines,String text,boolean title){for(String part:RecipeText.wrap(text,title?16:17))lines.add(new Line(part,title,null));}
    private static void append(List<Component> pages,List<Line> lines){
        for(int i=0;i<lines.size();i+=13){List<Line> page=new ArrayList<>(lines.subList(i,Math.min(i+13,lines.size())));page.add(new Line("[Catalog]",false,new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fusion guide")));pages.add(component(page));}
    }
    private static Component component(List<Line> lines){
        if(lines.size()>14)throw new IllegalArgumentException("Too many book lines");MutableComponent page=Component.empty();
        for(int i=0;i<lines.size();i++){
            if(i>0)page.append("\n");Line line=lines.get(i);Style style=Style.EMPTY.withItalic(false).withColor(line.click()!=null?ChatFormatting.BLUE:line.title()?ChatFormatting.DARK_PURPLE:ChatFormatting.BLACK);
            if(line.title())style=style.withBold(true);if(line.click()!=null)style=style.withClickEvent(line.click()).withUnderlined(true);
            page.append(Component.literal(line.text()).setStyle(style));
        }return page;
    }
    private static String group(Equipment e){return switch(e){case SWORD->"Swords";case BOW,CROSSBOW->"Bows / Crossbows";case TOOL,PICKAXE->"Mining tools";case ARMOR->"Armor";case BOOTS->"Boots";case TRIDENT->"Tridents";case MACE->"Maces";case HELMET,FISHING_ROD->"Utility";};}
}
