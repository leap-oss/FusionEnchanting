package org.leap.enchantmentfusion.discovery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.*;
import org.leap.enchantmentfusion.model.*;
import org.leap.enchantmentfusion.service.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.*;

/** Optional, read-only written-book guide. No guide item is granted to the player. */
public final class RecipeGuide {
    private record Line(String text,boolean title,ClickEvent click) {}
    private final FusionService service;
    public RecipeGuide(FusionService service){this.service=service;}

    public boolean open(Player player,String filter) {
        if(!player.hasPermission("enchantmentfusion.recipes"))return false;
        var book=build(filter);if(book.isEmpty())return false;
        // Outside inventory events: vanilla returns any anvil inputs before opening its book screen.
        player.closeInventory();player.openBook(book.get());return true;
    }
    public Optional<ItemStack> build(String query) {
        String filter=query==null?"":query.toLowerCase(Locale.ROOT);
        List<FusionDefinition> recipes=service.definitions().stream().filter(d->service.tuning(d.id()).enabled())
                .filter(d->filter.isEmpty()||d.id().equals(filter)||d.ingredients().containsKey(filter)).toList();
        if(recipes.isEmpty())return Optional.empty();
        List<Component> pages=new ArrayList<>();
        boolean detail=recipes.size()==1&&recipes.getFirst().id().equals(filter);
        if(detail){
            FusionDefinition d=recipes.getFirst();appendMetadata(pages,d,false);
            var tuning=service.tuning(d.id());
            List<Line> effect=new ArrayList<>();add(effect,d.name()+" I",true);add(effect,"");add(effect,"Efek dasar:");add(effect,d.description());
            add(effect,"");add(effect,"Batasan:");add(effect,d.limitation());add(effect,"");
            add(effect,"Cooldown: "+number(tuning.cooldown())+" dtk");add(effect,"Peluang: "+Math.round(tuning.chance()*100)+"%");
            add(effect,"Skala efek: x"+number(tuning.power()));
            append(pages,effect,command("Katalog","/fusion guide"));
        }else{
            pages.add(introduction());pages.add(Component.empty());
            Map<String,Integer> firstPages=new LinkedHashMap<>();
            for(FusionDefinition d:recipes){firstPages.putIfAbsent(group(d.equipment()),pages.size()+1);appendMetadata(pages,d,true);}
            List<Line> index=new ArrayList<>();add(index,"Daftar resep",true);add(index,"");
            firstPages.forEach((name,page)->index.add(new Line(name,false,ClickEvent.changePage(page))));
            add(index,"");add(index,recipes.size()+" resep aktif");
            pages.set(1,component(index));
        }
        if(pages.size()>100)throw new IllegalStateException("Guide exceeds vanilla page limit");
        ItemStack book=new ItemStack(Material.WRITTEN_BOOK);BookMeta meta=(BookMeta)book.getItemMeta();
        meta.title(Component.text("Panduan Fusion"));meta.author(Component.text("LEAP"));meta.addPages(pages.toArray(Component[]::new));
        book.setItemMeta(meta);return Optional.of(book);
    }
    private void appendMetadata(List<Component> pages,FusionDefinition d,boolean detailLink) {
        List<Line> lines=new ArrayList<>();add(lines,d.name()+" I",true);add(lines,"");add(lines,"Bahan:");
        var sources=d.ingredients().entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.reverseOrder())).toList();
        for(int i=0;i<sources.size();i++){if(i>0)add(lines,"+");var e=sources.get(i);add(lines,RecipeText.ingredient(e.getKey(),e.getValue()));}
        add(lines,"Untuk:");add(lines,d.equipment().label());add(lines,"XP: vanilla +"+service.tuning(d.id()).extraLevels());
        if(detailLink)lines.add(new Line("[Efek & detail]",false,ClickEvent.runCommand("/fusion guide "+d.id())));
        append(pages,lines,command("Katalog","/fusion guide"));
    }
    private static Component introduction() {
        List<Line> lines=new ArrayList<>();add(lines,"ENCHANT FUSION",true);add(lines,"");
        for(String text:List.of("Buku + buku","= buku fusion.","Pasang bukunya","ke equipment.","","Buat: vanilla +","biaya fusion.","Pasang: vanilla.","","Resep & bahan:"))add(lines,text);
        lines.add(new Line("[Lihat resep]",false,ClickEvent.changePage(2)));return component(lines);
    }
    private static void add(List<Line> lines,String text){add(lines,text,false);}
    private static void add(List<Line> lines,String text,boolean title){for(String part:RecipeText.wrap(text,title?16:17))lines.add(new Line(part,title,null));}
    private static void append(List<Component> pages,List<Line> lines,Line footer){
        for(int i=0;i<lines.size();i+=13){List<Line> page=new ArrayList<>(lines.subList(i,Math.min(i+13,lines.size())));page.add(footer);pages.add(component(page));}
    }
    private static Component component(List<Line> lines){
        if(lines.size()>14)throw new IllegalArgumentException("Too many book lines");
        Component page=Component.empty();
        for(int i=0;i<lines.size();i++){
            Line line=lines.get(i);if(i>0)page=page.append(Component.newline());
            Component text=Component.text(line.text(),line.click()!=null?NamedTextColor.BLUE:line.title()?NamedTextColor.DARK_PURPLE:NamedTextColor.BLACK)
                    .decoration(TextDecoration.ITALIC,false);
            if(line.title())text=text.decorate(TextDecoration.BOLD);
            if(line.click()!=null)text=text.clickEvent(line.click()).decorate(TextDecoration.UNDERLINED);
            page=page.append(text);
        }
        return page;
    }
    private static Line command(String label,String command){return new Line("["+label+"]",false,ClickEvent.runCommand(command));}
    private static String number(double value){return value==Math.rint(value)?Long.toString((long)value):Double.toString(value);}
    private static String group(Equipment category){return switch(category){
        case SWORD->"Pedang";case BOW,CROSSBOW->"Bow / Crossbow";case TOOL,PICKAXE->"Mining";
        case ARMOR->"Armor";case BOOTS->"Sepatu";case TRIDENT->"Trisula";case MACE->"Mace";case HELMET,FISHING_ROD->"Utility";
    };}
}
