package org.leap.fusionenchanting.ability;

import org.leap.fusionenchanting.*;
import org.leap.fusionenchanting.api.*;
import org.leap.fusionenchanting.core.*;
import org.leap.fusionenchanting.item.FusionItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class FusionAbilities {
    public record Hit(ServerPlayer player,LivingEntity target,DamageSource source,FusionItems.State state,
                      FusionProjectile projectile,double bonus,double power){}
    private static final Set<String> OTHER=Set.of("soul_reaper","retribution","molten_guard","reactive_armor","deflection","fortified","bloodforged",
            "prospector","excavator","workhorse","miners_luck","preservation","prosperity","amphibious","glacial_step","soulbound_steps","wave_rider","master_angler","aquanaut");
    private record Use(UUID instance,int tick){}
    private static final class Motion{boolean wet;int wetTick;float fall;Vec3 last;Object level;}
    private final FusionRuntime runtime;
    private final Map<UUID,Use> ores=new HashMap<>(),catches=new HashMap<>();
    private final Map<UUID,Motion> motion=new HashMap<>();
    private final Map<UUID,Long> lastCooldownHint=new HashMap<>();
    public FusionAbilities(FusionRuntime runtime){this.runtime=runtime;}
    public void verifyCatalog(){Set<String> ids=new HashSet<>(CombatRules.IDS);ids.addAll(OTHER);if(!ids.equals(FusionCatalog.definitions().stream().map(FusionDefinition::id).collect(java.util.stream.Collectors.toSet())))throw new IllegalStateException("Fabric handlers and catalog differ");}
    public boolean canAffect(ServerPlayer player,LivingEntity target){
        return target.isAlive()&&!target.isRemoved()&&target.level()==player.level()&&target!=player&&!(target instanceof ArmorStand)
                &&!(target instanceof TamableAnimal pet&&pet.isTame())&&!target.getTags().contains("NPC")&&!target.getTags().contains("npc")
                &&(!(target instanceof ServerPlayer)||runtime.config().pvp());
    }
    public boolean permitted(ServerPlayer player,String id,LivingEntity target){return FusionEvents.allow(runtime.events.beforeAbility,new FusionEvents.Ability(player,id,target));}
    private boolean roll(ServerPlayer player,FusionItems.State state,LivingEntity target){
        String id=state.definition().id();
        if(!runtime.eligible(player)||!runtime.tuning(id).enabled())return false;
        if(!runtime.cooldowns.ready(player.getUUID(),id)){
            notifyCooldown(player,state);
            return false;
        }
        return player.getRandom().nextDouble()<runtime.profile(state).chance()&&permitted(player,id,target);
    }
    private boolean proc(ServerPlayer player,FusionItems.State state,LivingEntity target){if(!roll(player,state,target))return false;start(player,state);return true;}
    private void start(ServerPlayer player,FusionItems.State state){
        double cd=runtime.profile(state).cooldown();
        runtime.cooldowns.start(player.getUUID(),state.definition().id(),cd);
        runtime.progression.recordAbilityTriggered(player);
        notifyProc(player,state,cd);
    }
    private void notifyProc(ServerPlayer player,FusionItems.State state,double cd){
        if(!runtime.progression.notifyEnabled(player)||player.containerMenu instanceof FusionAnvil)return;
        String suffix=cd>0?" ("+FusionDescriptions.number(cd)+"s cooldown)":"";
        player.displayClientMessage(Component.literal("✦ "+state.definition().name()+" "+FusionLevels.roman(state.level())+" activated!"+suffix).withStyle(ChatFormatting.GOLD),true);
    }
    private void notifyCooldown(ServerPlayer player,FusionItems.State state){
        if(!runtime.progression.notifyEnabled(player)||player.containerMenu instanceof FusionAnvil)return;
        long now=System.nanoTime();
        if(now-lastCooldownHint.getOrDefault(player.getUUID(),0L)<1_500_000_000L)return;
        lastCooldownHint.put(player.getUUID(),now);
        double rem=runtime.cooldowns.remainingSeconds(player.getUUID(),state.definition().id());
        if(rem>0.05)player.displayClientMessage(Component.literal("⏳ "+state.definition().name()+" "+FusionLevels.roman(state.level())+" ("+FusionDescriptions.number(rem)+"s)").withStyle(ChatFormatting.RED),true);
    }
    public Hit plan(LivingEntity target,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer player)||!canAffect(player,target))return null;
        ItemStack weapon;FusionProjectile projectile=null;boolean charged;float fall=0;
        if(source.getDirectEntity() instanceof AbstractArrow arrow&&arrow instanceof FusionProjectile snapshot){
            if(snapshot.fusion$used())return null;weapon=snapshot.fusion$weapon();projectile=snapshot;charged=snapshot.fusion$force()>=.9f;
        }else if(source.getDirectEntity()==player&&source.is(DamageTypes.PLAYER_ATTACK)){
            var context=runtime.attack.get();if(context==null||context.player()!=player)return null;
            weapon=context.weapon();charged=context.charged();fall=context.fall();
        }else return null;
        var active=runtime.active(player,weapon);if(active.isEmpty())return null;var state=active.get();String id=state.definition().id();double power=runtime.profile(state).power();
        var facts=new CombatRules.Facts(charged,projectile!=null,source.getDirectEntity() instanceof ThrownTrident,fall,target.isOnFire(),
                target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE),target.getType().is(EntityTypeTags.ARTHROPOD),
                target.level().isRainingAt(target.blockPosition()),target.level().isThundering(),target.getHealth()/target.getMaxHealth());
        var bonus=CombatRules.bonus(id,facts,power);if(bonus.isEmpty()||!roll(player,state,target))return null;
        return new Hit(player,target,source,state,projectile,bonus.getAsDouble(),power);
    }
    public void afterHit(Hit hit){
        if(hit==null)return;start(hit.player(),hit.state());if(hit.projectile()!=null)hit.projectile().fusion$used(true);
        runtime.defer(()->{if(runtime.eligible(hit.player())&&canAffect(hit.player(),hit.target())&&runtime.tuning(hit.state().definition().id()).enabled())effect(hit);});
    }
    private void effect(Hit h){
        LivingEntity target=h.target();ServerPlayer player=h.player();double p=h.power();var profile=runtime.profile(h.state());
        switch(h.state().definition().id()){
            case "infernal_edge"->burn(target,3,p);
            case "blade_storm"->{
                potion(target,MobEffects.MOVEMENT_SLOWDOWN,2,0,profile);
                target.level().getEntitiesOfClass(Monster.class,target.getBoundingBox().inflate(3,2,3),mob->mob!=target&&canAffect(player,mob)&&player.hasLineOfSight(mob)&&mob.distanceToSqr(target)<=9)
                        .stream().limit(3).filter(mob->permitted(player,"blade_storm",mob)).forEach(mob->potion(mob,MobEffects.MOVEMENT_SLOWDOWN,2,0,profile));
                particles(target,ParticleTypes.SWEEP_ATTACK);
            }
            case "brutal_force"->{
                Vec3 direction=target.position().subtract(player.position()).multiply(1,0,1);if(direction.lengthSqr()<=.001)break;
                Vec3 current=target.getDeltaMovement(),horizontal=current.multiply(1,0,1),boosted=horizontal.add(direction.normalize().scale(.15*p));
                double cap=Math.max(1.2,horizontal.length());if(boosted.length()>cap)boosted=boosted.normalize().scale(cap);
                target.setDeltaMovement(boosted.x,Math.max(current.y,.15),boosted.z);target.hurtMarked=true;
            }
            case "executioner"->particles(target,ParticleTypes.DAMAGE_INDICATOR);
            case "holy_flame"->particles(target,ParticleTypes.SOUL_FIRE_FLAME);
            case "scorched_venom"->potion(target,MobEffects.WITHER,4,0,profile);
            case "flame_burst"->potion(target,MobEffects.WEAKNESS,3,0,profile);
            case "hellfire"->burn(target,5,p);
            case "impact"->potion(target,MobEffects.MOVEMENT_SLOWDOWN,2,1,profile);
            case "blast_arrow"->{potion(target,MobEffects.WEAKNESS,3,0,profile);particles(target,ParticleTypes.FLAME);}
            case "volley"->{potion(target,MobEffects.MOVEMENT_SLOWDOWN,3,0,profile);potion(player,MobEffects.MOVEMENT_SPEED,3,0,profile);}
            case "railshot"->particles(target,ParticleTypes.ELECTRIC_SPARK);
            case "stormcaller"->{LightningBolt bolt=EntityType.LIGHTNING_BOLT.create(player.serverLevel());if(bolt!=null){bolt.setVisualOnly(true);bolt.moveTo(target.position());player.serverLevel().addFreshEntity(bolt);}}
            case "tempest"->potion(target,MobEffects.MOVEMENT_SLOWDOWN,3,1,profile);
            case "meteor"->particles(target,ParticleTypes.CLOUD);
            case "shockwave"->potion(target,MobEffects.WEAKNESS,3,0,profile);
            case "comet"->burn(target,5,p);
            default->throw new IllegalStateException("Unhandled combat fusion");
        }
    }
    public void damaged(LivingEntity target,DamageSource source){
        if(!(target instanceof ServerPlayer player)||!runtime.eligible(player))return;
        if(!runtime.config().pvp()&&source.getEntity() instanceof ServerPlayer)return;
        Map<String,FusionItems.State> equipped=new HashMap<>();for(ItemStack armor:player.getArmorSlots())runtime.active(player,armor).ifPresent(state->equipped.merge(state.definition().id(),state,(a,b)->a.level()>=b.level()?a:b));
        for(var state:equipped.values()){String id=state.definition().id();var profile=runtime.profile(state);Runnable effect=null;LivingEntity context=player;
            if(source.getDirectEntity() instanceof LivingEntity attacker&&!source.is(DamageTypeTags.IS_PROJECTILE)&&canAffect(player,attacker)){
                if(id.equals("retribution"))effect=()->{if(canAffect(player,attacker))potion(attacker,MobEffects.WEAKNESS,3,0,profile);};
                if(id.equals("molten_guard"))effect=()->{if(canAffect(player,attacker))burn(attacker,3,profile.power());};context=attacker;
            }
            if(id.equals("reactive_armor")&&source.is(DamageTypeTags.IS_EXPLOSION))effect=()->potion(player,MobEffects.DAMAGE_RESISTANCE,3,0,profile);
            if(id.equals("deflection")&&source.is(DamageTypeTags.IS_PROJECTILE))effect=()->potion(player,MobEffects.MOVEMENT_SPEED,3,0,profile);
            if(effect!=null&&proc(player,state,context)){Runnable confirmed=effect;runtime.defer(()->{if(runtime.eligible(player)&&runtime.tuning(id).enabled())confirmed.run();});}
        }
    }
    public void killed(LivingEntity target,DamageSource source){
        if(!(source.getDirectEntity() instanceof ServerPlayer player)||!source.is(DamageTypes.PLAYER_ATTACK)||!runtime.eligible(player))return;
        runtime.active(player,player.getMainHandItem()).filter(s->s.definition().id().equals("soul_reaper")&&target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE))
                .ifPresent(s->{if(proc(player,s,player))potion(player,MobEffects.REGENERATION,4,0,runtime.profile(s));});
        if(!(target instanceof Monster))return;
        ItemStack selected=ItemStack.EMPTY;
        for(ItemStack armor:player.getArmorSlots())if(armor.getDamageValue()>selected.getDamageValue()&&runtime.active(player,armor).filter(s->s.definition().id().equals("bloodforged")).isPresent())selected=armor;
        if(!selected.isEmpty()){
            var state=runtime.items.read(selected).orElseThrow();
            if(proc(player,state,player))selected.setDamageValue(Math.max(0,selected.getDamageValue()-Math.max(1,(int)Math.round(4*runtime.profile(state).power()))));
        }
    }
    public void beforeMine(ServerPlayer player){
        var context=runtime.mining.get();if(context==null||context.player()!=player||!ore(context.block()))return;
        runtime.active(player,player.getMainHandItem()).filter(s->s.definition().id().equals("miners_luck")).ifPresent(s->ores.put(player.getUUID(),new Use(s.instance(),runtime.tick())));
    }
    public void mined(ServerPlayer player){
        var context=runtime.mining.get();if(context==null||context.player()!=player)return;
        runtime.active(player,player.getMainHandItem()).ifPresent(s->{String id=s.definition().id();
            if(id.equals("prospector")&&ore(context.block())&&proc(player,s,player))potion(player,MobEffects.NIGHT_VISION,12,0,runtime.profile(s));
            if(id.equals("excavator")&&player.isShiftKeyDown()&&proc(player,s,player))potion(player,MobEffects.DIG_SPEED,5,0,runtime.profile(s));
        });
    }
    public int consumeOreXp(ServerLevel level,Vec3 pos,int amount){
        var c=runtime.mining.get();if(c==null||c.player().serverLevel()!=level||c.tick()!=runtime.tick()||!ore(c.block())||pos.distanceToSqr(Vec3.atCenterOf(c.pos()))>.01||amount<=0)return amount;
        ItemStack item=c.player().getMainHandItem();var state=runtime.active(c.player(),item);
        if(state.isEmpty()||!state.get().definition().id().equals("prosperity")||runtime.items.read(c.tool()).filter(s->s.instance().equals(state.get().instance())).isEmpty())return amount;
        int points=FusionLevels.repairPoints(amount,item.getDamageValue(),runtime.profile(state.get()).power());if(points<1||!proc(c.player(),state.get(),c.player()))return amount;
        item.setDamageValue(Math.max(0,item.getDamageValue()-points*2));return amount-points;
    }
    public boolean avoidWear(ServerPlayer player,ItemStack item){
        var active=runtime.active(player,item);if(active.isEmpty())return false;var s=active.get();String id=s.definition().id();boolean condition=switch(id){
            case "workhorse"->player.getFoodData().getFoodLevel()>=16;
            case "preservation"->player.isShiftKeyDown();
            case "fortified"->true;
            case "miners_luck"->matches(ores.remove(player.getUUID()),s.instance());
            case "master_angler"->matches(catches.remove(player.getUUID()),s.instance());
            default->false;
        };return condition&&proc(player,s,player);
    }
    private boolean matches(Use use,UUID instance){return use!=null&&use.instance().equals(instance)&&runtime.tick()-use.tick()<=2;}
    public void caught(ServerPlayer player,ItemStack castRod,ItemStack usedRod){
        var cast=runtime.active(player,castRod);var used=runtime.active(player,usedRod);
        if(cast.isPresent()&&used.isPresent()&&cast.get().definition().id().equals("master_angler")&&cast.get().instance().equals(used.get().instance()))catches.put(player.getUUID(),new Use(cast.get().instance(),runtime.tick()));
        if(runtime.eligible(player)&&player.getRandom().nextDouble()<0.02){
            var enabled=FusionCatalog.definitions().stream().filter(d->runtime.tuning(d.id()).enabled()).toList();
            if(!enabled.isEmpty()){
                FusionDefinition chosen=enabled.get(player.getRandom().nextInt(enabled.size()));
                int tier=player.getRandom().nextDouble()<0.25&&FusionLevels.example(chosen,2).isPresent()?2:1;
                if(FusionLevels.example(chosen,tier).isEmpty())tier=3;
                ItemStack book=runtime.items.fuse(FusionItems.sourceBook(player.server.registryAccess(),chosen,tier),chosen,UUID.randomUUID());
                player.spawnAtLocation(book);
            }
        }
    }
    public void riptide(ServerPlayer player,ItemStack item){runtime.active(player,item).filter(s->s.definition().id().equals("wave_rider")).ifPresent(s->{if(proc(player,s,player))potion(player,MobEffects.DOLPHINS_GRACE,4,0,runtime.profile(s));});}
    public void tick(ServerPlayer player){
        if(!runtime.eligible(player)){motion.remove(player.getUUID());return;}
        Motion m=motion.computeIfAbsent(player.getUUID(),key->new Motion());boolean wet=player.isInWaterOrBubble();
        if(m.level!=player.level()||m.last!=null&&m.last.distanceToSqr(player.position())>256){m.wet=false;m.fall=0;}
        m.level=player.level();m.last=player.position();
        m.fall=Math.max(m.fall,player.fallDistance);var boots=runtime.active(player,player.getItemBySlot(EquipmentSlot.FEET));
        if(boots.isPresent()){
            String id=boots.get().definition().id();var profile=runtime.profile(boots.get());
            if(id.equals("amphibious")&&m.wet&&!wet&&player.onGround()&&runtime.tick()-m.wetTick<=40&&proc(player,boots.get(),player))potion(player,MobEffects.MOVEMENT_SPEED,4,0,profile);
            if(id.equals("glacial_step")&&player.onGround()&&m.fall>=3&&proc(player,boots.get(),player))potion(player,MobEffects.MOVEMENT_SPEED,3,0,profile);
            if(id.equals("soulbound_steps")&&runtime.tick()%10==0){BlockState floor=player.level().getBlockState(BlockPos.containing(player.getX(),player.getY()-.1,player.getZ()));
                if((floor.is(Blocks.SOUL_SAND)||floor.is(Blocks.SOUL_SOIL))&&proc(player,boots.get(),player))potion(player,MobEffects.JUMP,2,0,profile);}
        }
        if(wet){m.wet=true;m.wetTick=runtime.tick();}else if(player.onGround()||runtime.tick()-m.wetTick>40)m.wet=false;
        if(player.onGround()||wet||player.isFallFlying()||player.getAbilities().flying)m.fall=0;
        if(runtime.tick()%10==0&&wet)runtime.active(player,player.getItemBySlot(EquipmentSlot.HEAD)).filter(s->s.definition().id().equals("aquanaut"))
                .ifPresent(s->{if(proc(player,s,player))potion(player,MobEffects.NIGHT_VISION,12,0,runtime.profile(s));});
    }
    public static void potion(LivingEntity target,Holder<MobEffect> type,double seconds,int amplifier,double power){
        int ticks=Math.max(1,(int)Math.round(seconds*20*power));var old=target.getEffect(type);
        if(old!=null&&(old.getDuration()<0||old.getAmplifier()>amplifier||old.getAmplifier()==amplifier&&old.getDuration()>=ticks))return;
        target.addEffect(new MobEffectInstance(type,ticks,amplifier,true,false,true));
    }
    public static void potion(LivingEntity target,Holder<MobEffect> type,double seconds,int amplifier,FusionLevels.Profile profile){
        boolean fixed=type.equals(MobEffects.NIGHT_VISION)||type.equals(MobEffects.DOLPHINS_GRACE);
        potion(target,type,seconds,fixed?amplifier:profile.amplifier(amplifier),profile.power());
    }
    private static void burn(LivingEntity target,double seconds,double power){target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),(int)Math.round(seconds*20*power)));particles(target,ParticleTypes.FLAME);}
    private static void particles(LivingEntity target,SimpleParticleType type){if(target.level() instanceof ServerLevel world)world.sendParticles(type,target.getX(),target.getY()+1,target.getZ(),8,.35,.4,.35,.02);}
    private static boolean ore(BlockState block){String id=BuiltInRegistries.BLOCK.getKey(block.getBlock()).getPath();return id.endsWith("_ore")||block.is(Blocks.ANCIENT_DEBRIS);}
    public void clear(){ores.clear();catches.clear();motion.clear();}
    public void teleported(ServerPlayer player){motion.remove(player.getUUID());ores.remove(player.getUUID());catches.remove(player.getUUID());}
    public void prune(MinecraftServer server){ores.values().removeIf(use->runtime.tick()-use.tick()>2);catches.values().removeIf(use->runtime.tick()-use.tick()>2);motion.keySet().removeIf(id->server.getPlayerList().getPlayer(id)==null);}
}
