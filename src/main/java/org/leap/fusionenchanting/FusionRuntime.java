package org.leap.fusionenchanting;

import org.leap.fusionenchanting.ability.FusionAbilities;
import org.leap.fusionenchanting.anvil.AnvilLogic;
import org.leap.fusionenchanting.api.*;
import org.leap.fusionenchanting.config.FusionConfig;
import org.leap.fusionenchanting.core.*;
import org.leap.fusionenchanting.discovery.*;
import org.leap.fusionenchanting.item.FusionItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.state.BlockState;
import java.nio.file.Path;
import java.util.*;

public final class FusionRuntime {
    public record Attack(ServerPlayer player,ItemStack weapon,boolean charged,float fall){}
    public record Launch(UUID player,ItemStack weapon,float force){}
    public record Mining(ServerPlayer player,BlockPos pos,BlockState block,ItemStack tool,int tick){}
    private record Task(int tick,int revision,Runnable action){}
    public final FusionItems items=new FusionItems();
    public final FusionEvents events=new FusionEvents();
    public final Cooldowns cooldowns=new Cooldowns();
    public final FusionProgression progression=new FusionProgression();
    public final AnvilLogic anvil=new AnvilLogic(this);
    public final FusionAbilities abilities=new FusionAbilities(this);
    public final RecipeGuide guide=new RecipeGuide(this);
    public final RecipeGui gui=new RecipeGui(this,guide);
    public final AnvilDiscovery discovery=new AnvilDiscovery(this);
    public final ThreadLocal<Attack> attack=new ThreadLocal<>();
    public final ThreadLocal<Launch> launch=new ThreadLocal<>();
    public final ThreadLocal<Mining> mining=new ThreadLocal<>();
    private final Path path;
    private final Queue<Task> tasks=new ArrayDeque<>();
    private FusionConfig config;
    private MinecraftServer server;
    private int revision,tick;
    public FusionRuntime(Path path)throws Exception{this.path=path;config=FusionConfig.load(path);items.tunings(this::tuning);guide.attachGui(gui);abilities.verifyCatalog();}
    public FusionConfig config(){return config;}
    public int revision(){return revision;}
    public int tick(){return tick;}
    public FusionTuning tuning(String id){return config.tunings().get(id);}
    public FusionLevels.Profile profile(FusionItems.State state){return FusionLevels.profile(state.definition(),tuning(state.definition().id()),state.level());}
    public boolean allowed(ServerPlayer player){
        var world=player.level().dimension().location();
        return !config.disabledWorlds().contains(world.toString())&&!config.disabledWorlds().contains(world.getPath())&&FusionEvents.allow(events.allowUse,player);
    }
    public boolean recipes(ServerPlayer player){return FusionEvents.allow(events.allowRecipes,player);}
    public boolean eligible(ServerPlayer player){return !player.isRemoved()&&player.isAlive()&&!player.isSpectator()&&!player.isCreative()
            &&player.getServer()!=null&&player.getServer().getPlayerList().getPlayer(player.getUUID())==player&&allowed(player);}
    public Optional<FusionItems.State> active(ServerPlayer player,ItemStack item){
        return eligible(player)&&!FusionItems.book(item)?items.read(item).filter(s->tuning(s.definition().id()).enabled()):Optional.empty();
    }
    public void defer(Runnable action){tasks.add(new Task(tick+1,revision,action));}
    public void tick(MinecraftServer server){
        if(this.server!=server){this.server=server;tick=0;tasks.clear();cooldowns.clear();abilities.clear();discovery.clear();}
        tick++;
        while(!tasks.isEmpty()&&tasks.peek().tick()<=tick){Task task=tasks.remove();if(task.revision()==revision)task.action().run();}
        for(var player:server.getPlayerList().getPlayers()){
            abilities.tick(player);
            checkEnchantTable(player);
            if(tick%5==0)discovery.tick(player);
            if(tick%20==0){refresh(player);checkNearbyLibrarians(player);}
        }
        if(tick%200==0){cooldowns.prune();abilities.prune(server);discovery.prune(server);}
        attack.remove();launch.remove();mining.remove();
    }
    public void reload()throws Exception{
        FusionConfig replacement=FusionConfig.load(path);config=replacement;revision++;tasks.clear();discovery.clear();
        if(server!=null)for(var player:server.getPlayerList().getPlayers())if(player.containerMenu instanceof FusionAnvil menu)menu.fusion$invalidate();
    }
    public void stop(MinecraftServer instance){
        if(server!=instance)return;tasks.clear();cooldowns.clear();abilities.clear();discovery.clear();attack.remove();launch.remove();mining.remove();server=null;
    }
    private void refresh(ServerPlayer player){
        var inventory=player.getInventory();
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            ItemStack item=inventory.getItem(slot);var state=items.read(item);if(state.isEmpty())continue;
            var updated=items.fuse(item,state.get().definition(),state.get().instance());if(!ItemStack.matches(item,updated))inventory.setItem(slot,updated);
        }
    }
    private final Set<UUID> checkedMenuItems=new HashSet<>();
    private void checkEnchantTable(ServerPlayer player){
        if(!allowed(player)||!(player.containerMenu instanceof EnchantmentMenu menu))return;
        ItemStack slotItem=menu.getSlot(0).getItem();
        if(slotItem.isEmpty()||items.marked(slotItem))return;
        var enchants=FusionItems.enchantments(slotItem);
        if(enchants.isEmpty())return;
        boolean isBook=FusionItems.book(slotItem);
        Optional<FusionDefinition> match=FusionCatalog.definitions().stream()
                .filter(d->tuning(d.id()).enabled())
                .filter(d->isBook||FusionItems.accepts(d.equipment(),slotItem))
                .filter(d->d.ingredients().keySet().stream().allMatch(k->enchants.getOrDefault(k,0)>=1))
                .max(Comparator.comparingInt(d->FusionLevels.level(d,enchants)));
        if(match.isEmpty())return;
        FusionDefinition d=match.get();
        ItemStack fused=items.fuse(slotItem,d,UUID.randomUUID());
        var state=items.read(fused);
        if(state.isEmpty())return;
        menu.getSlot(0).set(fused);
        menu.broadcastChanges();
        int tier=state.get().level();
        if(isBook)progression.recordBookCreated(player,tier);else progression.recordBookApplied(player,tier);
        if(progression.notifyEnabled(player)){
            player.sendSystemMessage(Component.literal("✦ Enchanting Table Resonance: ").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(d.name()+" "+FusionLevels.roman(tier)).withStyle(ChatFormatting.GOLD)));
            player.level().playSound(null,player.blockPosition(),SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.PLAYERS,0.8f,1.3f);
        }
    }
    private void checkNearbyLibrarians(ServerPlayer player){
        if(!allowed(player))return;
        for(Villager v:player.level().getEntitiesOfClass(Villager.class,player.getBoundingBox().inflate(8),
                e->e.getVillagerData().getProfession()==VillagerProfession.LIBRARIAN)){
            int level=v.getVillagerData().getLevel();
            String tag="fusion_librarian_lvl_"+level;
            if(v.getTags().contains(tag))continue;
            v.addTag(tag);
            var offers=v.getOffers();
            boolean hasFusion=offers.stream().anyMatch(o->items.marked(o.getResult()));
            double chance=(level>=4&&!hasFusion)?1.0:(level>=3?0.50:0.35);
            for(int i=offers.size()-1;i>=0;i--){
                MerchantOffer offer=offers.get(i);
                ItemStack res=offer.getResult();
                if(!res.is(Items.ENCHANTED_BOOK)||items.marked(res))continue;
                if(player.getRandom().nextDouble()>=chance)continue;
                var existing=FusionItems.enchantments(res);
                var candidates=FusionCatalog.definitions().stream()
                        .filter(d->tuning(d.id()).enabled())
                        .filter(d->existing.keySet().stream().anyMatch(d.ingredients()::containsKey))
                        .toList();
                if(candidates.isEmpty())candidates=FusionCatalog.definitions().stream().filter(d->tuning(d.id()).enabled()).toList();
                if(candidates.isEmpty())continue;
                FusionDefinition chosen=candidates.get(player.getRandom().nextInt(candidates.size()));
                int tier=player.getRandom().nextDouble()<0.30&&FusionLevels.example(chosen,2).isPresent()?2:1;
                if(FusionLevels.example(chosen,tier).isEmpty())tier=3;
                ItemStack fusionBook=items.fuse(FusionItems.sourceBook(player.server.registryAccess(),chosen,tier),chosen,UUID.randomUUID());
                int emeralds=tier==1?18+player.getRandom().nextInt(9):28+player.getRandom().nextInt(9);
                offers.set(i,new MerchantOffer(
                        new ItemCost(Items.EMERALD,emeralds),
                        Optional.of(new ItemCost(Items.BOOK,1)),
                        fusionBook,
                        offer.getUses(),
                        Math.max(3,offer.getMaxUses()),
                        Math.max(10,offer.getXp()),
                        offer.getPriceMultiplier(),
                        offer.getDemand()
                ));
                if(level>=4&&!hasFusion)break;
            }
        }
    }
    public void beginAttack(ServerPlayer player){attack.set(new Attack(player,player.getMainHandItem().copy(),player.getAttackStrengthScale(0)>=.9f,player.fallDistance));}
    public void beginLaunch(LivingEntity owner,ItemStack weapon,float force){if(owner instanceof ServerPlayer player)launch.set(new Launch(player.getUUID(),weapon.copy(),force));}
    public void beginMine(ServerPlayer player,BlockPos pos){mining.set(new Mining(player,pos.immutable(),player.level().getBlockState(pos),player.getMainHandItem().copy(),tick));}
}
