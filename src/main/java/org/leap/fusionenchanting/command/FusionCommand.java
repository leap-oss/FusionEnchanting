package org.leap.fusionenchanting.command;

import com.mojang.brigadier.*;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.leap.fusionenchanting.*;
import org.leap.fusionenchanting.core.*;
import org.leap.fusionenchanting.discovery.FusionProgression;
import org.leap.fusionenchanting.item.FusionItems;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.*;

public final class FusionCommand {
    private FusionCommand(){}
    private static FusionRuntime runtime(){return FusionEnchanting.runtime();}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        var root=Commands.literal("fusion").executes(ctx->help(ctx.getSource()))
            .then(Commands.literal("help").executes(ctx->help(ctx.getSource())))
            .then(Commands.literal("guide").requires(FusionCommand::recipes).executes(ctx->guide(ctx.getSource(),""))
                .then(Commands.argument("filter",StringArgumentType.word()).suggests((ctx,b)->SharedSuggestionProvider.suggest(filters(),b)).executes(ctx->guide(ctx.getSource(),StringArgumentType.getString(ctx,"filter")))))
            .then(Commands.literal("gui").requires(FusionCommand::recipes).executes(ctx->guide(ctx.getSource(),""))
                .then(Commands.argument("filter",StringArgumentType.word()).suggests((ctx,b)->SharedSuggestionProvider.suggest(filters(),b)).executes(ctx->guide(ctx.getSource(),StringArgumentType.getString(ctx,"filter")))))
            .then(Commands.literal("book").requires(FusionCommand::recipes).executes(ctx->book(ctx.getSource(),""))
                .then(Commands.argument("filter",StringArgumentType.word()).suggests((ctx,b)->SharedSuggestionProvider.suggest(filters(),b)).executes(ctx->book(ctx.getSource(),StringArgumentType.getString(ctx,"filter")))))
            .then(Commands.literal("recipes").requires(FusionCommand::recipes).executes(ctx->recipes(ctx.getSource(),1))
                .then(Commands.argument("page",IntegerArgumentType.integer(1)).executes(ctx->recipes(ctx.getSource(),IntegerArgumentType.getInteger(ctx,"page")))))
            .then(Commands.literal("info").requires(FusionCommand::recipes).then(Commands.argument("id",StringArgumentType.word()).suggests((ctx,b)->SharedSuggestionProvider.suggest(ids(),b))
                .executes(ctx->info(ctx.getSource(),StringArgumentType.getString(ctx,"id")))))
            .then(Commands.literal("inspect").requires(FusionCommand::recipes).executes(ctx->{
                var player=ctx.getSource().getPlayerOrException();var state=runtime().items.read(player.getMainHandItem());
                if(state.isEmpty())return fail(ctx.getSource(),"This item has no valid fusion.");
                player.setItemInHand(InteractionHand.MAIN_HAND,runtime().items.fuse(player.getMainHandItem(),state.get().definition(),state.get().instance()));
                return info(ctx.getSource(),state.get().definition().id());
            }))
            .then(Commands.literal("extract").executes(ctx->extract(ctx.getSource())))
            .then(Commands.literal("notify").executes(ctx->notifyToggle(ctx.getSource())))
            .then(Commands.literal("stats").requires(FusionCommand::recipes).executes(ctx->stats(ctx.getSource())))
            .then(Commands.literal("give").requires(source->source.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("id",StringArgumentType.word())
                .suggests((ctx,b)->SharedSuggestionProvider.suggest(ids(),b))
                .executes(ctx->give(ctx.getSource(),EntityArgument.getPlayer(ctx,"player"),StringArgumentType.getString(ctx,"id"),3))
                .then(Commands.argument("level",IntegerArgumentType.integer(1,3)).executes(ctx->give(ctx.getSource(),EntityArgument.getPlayer(ctx,"player"),StringArgumentType.getString(ctx,"id"),IntegerArgumentType.getInteger(ctx,"level")))))))
            .then(Commands.literal("reload").requires(s->s.hasPermission(2)).executes(ctx->{
                try{runtime().reload();send(ctx.getSource(),"Settings loaded. Old fusion previews were invalidated; reinsert your anvil inputs.");return 1;}
                catch(Exception e){return fail(ctx.getSource(),"Reload rejected: "+e.getMessage()+". The previous active configuration is still in use.");}
            }));
        var node=dispatcher.register(root);dispatcher.register(Commands.literal("efusion").executes(ctx->help(ctx.getSource())).redirect(node));
    }
    private static List<String> ids(){return FusionCatalog.definitions().stream().map(FusionDefinition::id).toList();}
    private static List<String> filters(){Set<String> options=new TreeSet<>();options.add("stats");for(var d:FusionCatalog.definitions())if(runtime().tuning(d.id()).enabled()){options.add(d.id());options.addAll(d.ingredients().keySet());}return List.copyOf(options);}
    private static boolean recipes(CommandSourceStack source){return !(source.getEntity() instanceof ServerPlayer p)||runtime().recipes(p);}
    private static int give(CommandSourceStack source,ServerPlayer target,String id,int tier){
        var d=runtime().items.definition(id.toLowerCase(Locale.ROOT));if(d==null)return fail(source,"Fusion not found.");
        if(FusionLevels.example(d,tier).isEmpty())return fail(source,"This recipe has no reachable tier at that level.");
        if(target.getInventory().getFreeSlot()<0)return fail(source,"The player inventory is full.");
        ItemStack book=runtime().items.fuse(FusionItems.sourceBook(source.getServer().registryAccess(),d,tier),d,UUID.randomUUID());
        if(!target.getInventory().add(book))return fail(source,"The book could not be added.");
        send(source,"Given: "+d.name()+" "+FusionLevels.roman(tier)+" → "+target.getGameProfile().getName());return 1;
    }
    private static int help(CommandSourceStack source){
        send(source,"FusionEnchanting — 36 fusion recipes; one fusion per item.");source.sendSuccess(()->link("[Open Fusion GUI Wiki]","/fusion guide"),false);
        send(source,"/fusion guide [id/enchant], /fusion book [id/enchant], /fusion recipes [page], /fusion info <id>, /fusion inspect");
        send(source,"/fusion extract (unbind into a Book), /fusion notify (toggle HUD), /fusion stats");
        send(source,"Anvil: enchanted book + enchanted book → fusion book. Apply the fusion book to equipment in an anvil.");
        if(source.hasPermission(2))send(source,"Admin: /fusion give <player> <id> [level], /fusion reload");return 1;
    }
    private static int guide(CommandSourceStack source,String filter)throws CommandSyntaxException{
        if("book".equalsIgnoreCase(filter))return book(source,"");
        return runtime().guide.open(source.getPlayerOrException(),filter)?1:fail(source,"No enabled recipe found. Use /fusion guide [id/enchant].");
    }
    private static int book(CommandSourceStack source,String filter)throws CommandSyntaxException{
        return runtime().guide.openBook(source.getPlayerOrException(),filter)?1:fail(source,"No enabled recipe found. Use /fusion book [id/enchant].");
    }
    private static int notifyToggle(CommandSourceStack source)throws CommandSyntaxException{
        ServerPlayer player=source.getPlayerOrException();
        boolean enabled=runtime().progression.toggleNotify(player);
        send(source,"Fusion HUD Notifications: "+(enabled?"ENABLED":"DISABLED"));
        return 1;
    }
    private static int stats(CommandSourceStack source)throws CommandSyntaxException{
        ServerPlayer player=source.getPlayerOrException();
        var prog=runtime().progression;
        int highest=prog.highestTier(player);
        send(source,"✦ Fusion Profile — "+player.getGameProfile().getName());
        send(source,"Books Created: "+prog.created(player)+" | Applied: "+prog.applied(player)+" | Extracted: "+prog.extracted(player));
        send(source,"Smithing Upgrades: "+prog.smithingUpgrades(player)+" | Abilities Triggered: "+prog.abilitiesTriggered(player)+" | Highest Tier: "+(highest>0?FusionLevels.roman(highest):"-"));
        send(source,"Milestones Unlocked: "+prog.unlocked(player).size()+"/"+FusionProgression.Milestone.values().length);
        source.sendSuccess(()->link("[Open Milestones GUI]","/fusion guide stats"),false);
        return 1;
    }
    private static int extract(CommandSourceStack source)throws CommandSyntaxException{
        ServerPlayer player=source.getPlayerOrException();
        if(!runtime().allowed(player))return fail(source,"You do not have permission to use fusions in this world.");
        ItemStack held=player.getMainHandItem();
        if(FusionItems.book(held))return fail(source,"This item is already a Fusion Book.");
        var stateOpt=runtime().items.read(held);
        if(stateOpt.isEmpty())return fail(source,"Hold fused equipment in your main hand to extract its Fusion Book.");
        boolean creative=player.getAbilities().instabuild;
        if(!creative&&player.experienceLevel<3)return fail(source,"Extracting a fusion requires 3 XP levels and 1 plain Book.");
        int bookSlot=-1;
        var inv=player.getInventory();
        for(int i=0;i<inv.getContainerSize();i++){
            ItemStack s=inv.getItem(i);
            if(s.is(Items.BOOK)&&!s.has(DataComponents.CUSTOM_NAME)&&!s.has(DataComponents.LORE)){bookSlot=i;break;}
        }
        if(bookSlot<0)return fail(source,"You need 1 plain Book in your inventory to extract the fusion into.");
        ItemStack bookStack=inv.getItem(bookSlot);
        if(bookStack.getCount()>1&&inv.getFreeSlot()<0)return fail(source,"Your inventory is full; make space before extracting.");
        var state=stateOpt.get();
        var registries=player.server.registryAccess();
        ItemStack carrier=new ItemStack(Items.ENCHANTED_BOOK);
        var stored=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        state.sources().forEach((k,lvl)->stored.set(registries.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.withDefaultNamespace(k))),lvl));
        carrier.set(DataComponents.STORED_ENCHANTMENTS,stored.toImmutable());
        ItemStack fusionBook=runtime().items.fuse(carrier,state.definition(),state.instance());

        ItemStack cleansed=runtime().items.strip(held);
        var remaining=new ItemEnchantments.Mutable(FusionItems.enchants(cleansed));
        for(String k:state.sources().keySet()){
            var holder=registries.lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.withDefaultNamespace(k)));
            remaining.set(holder,0);
        }
        cleansed.set(DataComponents.ENCHANTMENTS,remaining.toImmutable());
        player.setItemInHand(InteractionHand.MAIN_HAND,cleansed);
        if(!creative)player.giveExperienceLevels(-3);
        if(bookStack.getCount()==1){
            inv.setItem(bookSlot,fusionBook);
        }else{
            bookStack.shrink(1);
            inv.add(fusionBook);
        }
        runtime().progression.recordExtracted(player);
        send(source,"Extracted "+state.definition().name()+" "+FusionLevels.roman(state.level())+" into a Fusion Book!");
        return 1;
    }
    private static int recipes(CommandSourceStack source,int page){
        var catalog=FusionCatalog.definitions();int pages=(catalog.size()+5)/6;if(page>pages)return fail(source,"Available pages: 1–"+pages);
        send(source,"Fusion Recipes "+page+"/"+pages);source.sendSuccess(()->link("[Open recipe guide]","/fusion guide"),false);
        for(var d:catalog.subList((page-1)*6,Math.min(page*6,catalog.size())))source.sendSuccess(()->link(d.name()+" ["+d.equipment().label()+"]"+(runtime().tuning(d.id()).enabled()?"":" (disabled)"),"/fusion info "+d.id()),false);
        if(page<pages)source.sendSuccess(()->link("Next page →","/fusion recipes "+(page+1)),false);return 1;
    }
    private static int info(CommandSourceStack source,String id){
        var d=runtime().items.definition(id.toLowerCase(Locale.ROOT));if(d==null)return fail(source,"Fusion not found. See /fusion recipes.");var t=runtime().tuning(d.id());
        send(source,d.name()+" — "+d.equipment().label());send(source,"Full-tier recipe: "+RecipeText.equation(d));
        send(source,"Book cost: vanilla + "+t.extraLevels()+" levels. Applying: vanilla.");
        for(int tier=1;tier<=3;tier++)if(FusionLevels.example(d,tier).isPresent())send(source,FusionLevels.roman(tier)+": "+FusionDescriptions.describe(d,FusionLevels.profile(d,t,tier)));
        send(source,"Limits: "+d.limitation());return 1;
    }
    private static int fail(CommandSourceStack source,String text){source.sendFailure(Component.literal(text));return 0;}
    private static void send(CommandSourceStack source,String text){source.sendSuccess(()->Component.literal(text),false);}
    private static MutableComponent link(String text,String command){return Component.literal(text).withStyle(s->s.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,command)));}
}
