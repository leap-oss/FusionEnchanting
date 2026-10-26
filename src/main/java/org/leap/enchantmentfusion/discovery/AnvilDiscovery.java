package org.leap.enchantmentfusion.discovery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.leap.enchantmentfusion.model.*;
import org.leap.enchantmentfusion.service.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/** Read-only guidance. Never changes the anvil's inputs, result, cost or click handling. */
public final class AnvilDiscovery implements Listener {
    private record Hint(String text,String filter) {}
    private static final class Session {
        final AnvilInventory inventory;
        BukkitTask task;
        String lastHint="",lastLink="";
        int nextHint,nextLink;
        Session(AnvilInventory inventory){this.inventory=inventory;}
    }
    private final Plugin plugin;
    private final FusionService service;
    private final FusionItems items;
    private final NamespacedKey introduced;
    private final Map<UUID,Session> sessions=new HashMap<>();

    public AnvilDiscovery(Plugin plugin,FusionService service,FusionItems items){
        this.plugin=plugin;this.service=service;this.items=items;
        introduced=new NamespacedKey("enchantmentfusion","recipe_guide_seen");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void open(InventoryOpenEvent event){
        if(event.getInventory() instanceof AnvilInventory inventory&&event.getPlayer() instanceof Player player)request(player,inventory);
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void prepare(PrepareAnvilEvent event){
        if(event.getView().getPlayer() instanceof Player player)request(player,event.getInventory());
    }
    private boolean visible(Player player){return service.allowed(player)&&player.hasPermission("enchantmentfusion.recipes");}
    private void request(Player player,AnvilInventory inventory){
        if(!visible(player))return;
        Session session=sessions.get(player.getUniqueId());
        if(session==null||session.inventory!=inventory){clear(player.getUniqueId());session=new Session(inventory);sessions.put(player.getUniqueId(),session);}
        if(session.task==null){Session pending=session;session.task=Bukkit.getScheduler().runTaskLater(plugin,()->render(player,pending),1);}
    }
    private void render(Player player,Session session){
        session.task=null;
        if(!player.isOnline()||!visible(player)||player.getOpenInventory().getTopInventory()!=session.inventory)return;
        if(!player.getPersistentDataContainer().has(introduced,PersistentDataType.BYTE)){
            player.sendMessage(Component.text("Fusion: buku enchant + buku enchant → buku fusion; pasang ke equipment lewat anvil. ",NamedTextColor.GOLD)
                    .append(link("[Panduan resep]","")));
            player.getPersistentDataContainer().set(introduced,PersistentDataType.BYTE,(byte)1);
        }
        Hint hint=hint(player,session.inventory);
        if(hint==null){session.lastHint="";return;}
        int tick=Bukkit.getCurrentTick();
        if(hint.text().equals(session.lastHint))return;
        if(tick<session.nextHint){session.task=Bukkit.getScheduler().runTaskLater(plugin,()->render(player,session),session.nextHint-tick);return;}
        player.sendActionBar(Component.text(hint.text(),NamedTextColor.YELLOW));
        session.lastHint=hint.text();session.nextHint=tick+20;
        if(hint.filter()!=null&&!hint.filter().equals(session.lastLink)&&tick>=session.nextLink){
            player.sendMessage(Component.text("Fusion: "+hint.text()+" ",NamedTextColor.GRAY).append(link("[Lihat resep]",hint.filter())));
            session.lastLink=hint.filter();session.nextLink=tick+200;
        }
    }
    private Hint hint(Player player,AnvilInventory inventory){
        ItemStack left=inventory.getItem(0),right=inventory.getItem(1),result=inventory.getItem(2);
        AnvilView view=(AnvilView)player.getOpenInventory();
        var output=items.read(result);
        if(output.isPresent()){
            var state=output.get();String action=FusionService.book(result)?"Buku ":"Pasang ";
            return new Hint(action+state.definition().name()+" I • "+view.getRepairCost()+" level"+(player.getGameMode()!=GameMode.CREATIVE&&player.getLevel()<view.getRepairCost()?" (XP belum cukup)":""),null);
        }
        if(items.marked(left)||items.marked(right)){
            var source=items.read(items.marked(right)?right:left);
            if(source.isPresent()&&items.marked(right)&&!empty(left)&&!source.get().definition().equipment().accepts(left.getType().name()))
                return new Hint("Buku "+source.get().definition().name()+" untuk: "+source.get().definition().equipment().label(),source.get().definition().id());
            return null;
        }
        if(FusionService.book(left)&&empty(right)||FusionService.book(right)&&empty(left)){
            var sources=FusionItems.enchantments(FusionService.book(left)?left:right);
            for(FusionDefinition recipe:service.definitions())if(service.tuning(recipe.id()).enabled()){
                var source=recipe.ingredients().keySet().stream().filter(sources::containsKey).sorted().findFirst();
                if(source.isPresent())return new Hint("Contoh resep (level minimal): "+RecipeText.equation(recipe),source.get());
            }
            return null;
        }
        if(!FusionService.book(left)||!FusionService.book(right))return null;
        var a=FusionItems.enchantments(left);var b=FusionItems.enchantments(right);
        List<FusionDefinition> pairs=service.definitions().stream().filter(d->service.tuning(d.id()).enabled()).filter(d->{
            var keys=new ArrayList<>(d.ingredients().keySet());
            return a.containsKey(keys.get(0))&&b.containsKey(keys.get(1))||a.containsKey(keys.get(1))&&b.containsKey(keys.get(0));
        }).toList();
        if(pairs.isEmpty())return new Hint("Kedua buku ini tidak memiliki pasangan fusion aktif.","");
        if(player.getGameMode()!=GameMode.CREATIVE&&view.getRepairCost()>=view.getMaximumRepairCost())
            return new Hint("Fusion tetap mengikuti batas Too Expensive anvil vanilla.",pairs.getFirst().id());
        if(service.bookCandidates(left,right,result).size()>1)
            return new Hint("Ada beberapa pasangan fusion: hasil tetap vanilla. Gunakan dua buku dengan satu pasangan yang jelas.","");
        if(pairs.size()==1)return new Hint("Syarat resep: "+RecipeText.equation(pairs.getFirst()),pairs.getFirst().id());
        return new Hint("Belum ada fusion tunggal yang memenuhi syarat; periksa bahan dan level minimal.","");
    }
    private static boolean empty(ItemStack item){return item==null||item.getType().isAir();}
    private static Component link(String label,String filter){return Component.text(label,NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/fusion guide"+(filter.isEmpty()?"":" "+filter)));}
    @EventHandler public void close(InventoryCloseEvent event){if(event.getInventory() instanceof AnvilInventory)clear(event.getPlayer().getUniqueId());}
    @EventHandler public void quit(PlayerQuitEvent event){clear(event.getPlayer().getUniqueId());}
    private void clear(UUID id){Session session=sessions.remove(id);if(session!=null&&session.task!=null)session.task.cancel();}
    public void clear(){new ArrayList<>(sessions.keySet()).forEach(this::clear);}
}
