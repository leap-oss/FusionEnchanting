package org.leap.enchantmentfusion.model;

import java.util.*;

/** Tiers come from the native source levels, never from combining two fusion identities. */
public final class FusionLevels {
    public record Profile(int level,double power,double cooldown,double chance) {
        public int amplifier(int base){return base+(level==3?1:0);}
    }
    private static final Set<String> WEAR=Set.of("workhorse","miners_luck","preservation","fortified","master_angler");
    private FusionLevels(){}
    public static int level(FusionDefinition recipe,Map<String,Integer> sources){
        int progress=0,total=0;boolean full=true;
        for(var e:recipe.ingredients().entrySet()){
            int actual=sources.getOrDefault(e.getKey(),0);if(actual<1)return 0;
            progress+=Math.min(actual,e.getValue())-1;total+=e.getValue()-1;full&=actual>=e.getValue();
        }
        if(full)return 3;
        return progress>0&&progress*3>=total?2:1;
    }
    public static Profile profile(FusionDefinition recipe,FusionTuning tuning,int level){
        if(level<1||level>3)throw new IllegalArgumentException("Fusion level must be I–III");
        double multiplier=switch(level){case 1->1;case 2->1.75;default->2.5;};
        double cooldown=tuning.cooldown()*(level==1?1:level==2?.85:.7);
        double chance=tuning.chance()==1?1:Math.min(WEAR.contains(recipe.id())?.6:1,tuning.chance()*multiplier);
        return new Profile(level,tuning.power()*multiplier,cooldown,chance);
    }
    public static String roman(int level){return switch(level){case 1->"I";case 2->"II";case 3->"III";default->throw new IllegalArgumentException("Invalid fusion level");};}
    public static Optional<Map<String,Integer>> example(FusionDefinition recipe,int target){
        List<String> keys=recipe.ingredients().keySet().stream().sorted().toList();String a=keys.get(0),b=keys.get(1);
        Map<String,Integer> best=null;int cost=Integer.MAX_VALUE;double distance=Double.POSITIVE_INFINITY;
        for(int x=1;x<=recipe.ingredients().get(a);x++)for(int y=1;y<=recipe.ingredients().get(b);y++){
            Map<String,Integer> pair=Map.of(a,x,b,y);if(level(recipe,pair)!=target)continue;
            double balance=Math.abs((x-1d)/Math.max(1,recipe.ingredients().get(a)-1)-(y-1d)/Math.max(1,recipe.ingredients().get(b)-1));
            if(x+y<cost||x+y==cost&&balance<distance){best=pair;cost=x+y;distance=balance;}
        }
        return Optional.ofNullable(best);
    }
    public static int repairBudget(double power){return Math.max(1,(int)Math.ceil(2*power));}
    public static int repairPoints(int available,int damage,double power){return Math.min(repairBudget(power),Math.min(Math.max(0,available),Math.max(0,(damage+1)/2)));}
}
