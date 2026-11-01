package org.leap.enchantmentfusion.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** English gameplay text uses the same effective profile as the ability handlers. */
public final class FusionDescriptions {
    private FusionDescriptions(){}
    public static String number(double value){return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    private static String seconds(double base,FusionLevels.Profile p){return number(Math.max(.05,Math.round(base*20*p.power())/20d))+"s";}
    private static String effect(String name,int base,FusionLevels.Profile p){return name+" "+FusionLevels.roman(p.amplifier(base)+1);}
    public static String describe(FusionDefinition d,FusionLevels.Profile p){
        String power=number(p.power()),chance=number(p.chance()*100)+"%";
        String text=switch(d.id()){
            case "infernal_edge"->"Fully charged hits ignite targets for "+seconds(3,p)+". Burning targets take +"+number(2*p.power())+" damage.";
            case "blade_storm"->"Fully charged hits inflict "+effect("Slowness",0,p)+" for "+seconds(2,p)+" on the target and up to 3 nearby monsters.";
            case "brutal_force"->"Fully charged hits deal +"+power+" damage and add a bounded knockback boost.";
            case "executioner"->"Fully charged hits deal +"+number(4*p.power())+" damage to targets at or below 25% health.";
            case "holy_flame"->"Fully charged hits deal +"+number(3*p.power())+" damage to undead and show soul flames.";
            case "soul_reaper"->"Direct melee kills on undead grant "+effect("Regeneration",0,p)+" for "+seconds(4,p)+".";
            case "scorched_venom"->"Fully charged hits inflict "+effect("Wither",0,p)+" for "+seconds(4,p)+" on arthropods.";
            case "flame_burst"->"Fully charged hits inflict "+effect("Weakness",0,p)+" for "+seconds(3,p)+" on burning targets.";
            case "hellfire"->"Fully drawn arrows deal +"+number(2*p.power())+" damage and ignite targets for "+seconds(5,p)+".";
            case "impact"->"Fully drawn arrows inflict "+effect("Slowness",1,p)+" for "+seconds(2,p)+".";
            case "blast_arrow"->"Fully drawn arrows deal +"+power+" damage and inflict "+effect("Weakness",0,p)+" for "+seconds(3,p)+".";
            case "volley"->"A Multishot hit inflicts "+effect("Slowness",0,p)+" and grants "+effect("Speed",0,p)+" for "+seconds(3,p)+".";
            case "railshot"->"The first successful hit from a piercing projectile deals +"+number(3*p.power())+" damage.";
            case "prospector"->"Mining ore grants Night Vision for "+seconds(12,p)+".";
            case "excavator"->"Mining while sneaking grants "+effect("Haste",0,p)+" for "+seconds(5,p)+".";
            case "workhorse"->chance+" chance to prevent tool wear while your food level is at least 16.";
            case "miners_luck"->chance+" chance to prevent pickaxe wear when mining ore.";
            case "preservation"->chance+" chance to prevent tool wear while sneaking.";
            case "prosperity"->"Spend up to "+FusionLevels.repairBudget(p.power())+" dropped ore XP to repair up to "+(FusionLevels.repairBudget(p.power())*2)+" durability.";
            case "retribution"->"Melee attackers receive "+effect("Weakness",0,p)+" for "+seconds(3,p)+".";
            case "molten_guard"->"Melee attackers are ignited for "+seconds(3,p)+".";
            case "reactive_armor"->"Taking explosion damage grants "+effect("Resistance",0,p)+" for "+seconds(3,p)+".";
            case "deflection"->"Taking projectile damage grants "+effect("Speed",0,p)+" for "+seconds(3,p)+".";
            case "fortified"->chance+" chance to prevent wear on this armor piece.";
            case "bloodforged"->"Direct melee monster kills repair up to "+Math.max(1,Math.round(4*p.power()))+" durability on your most damaged Bloodforged armor piece.";
            case "amphibious"->"Leaving water onto solid ground grants "+effect("Speed",0,p)+" for "+seconds(4,p)+".";
            case "glacial_step"->"Landing after falling at least 3 blocks grants "+effect("Speed",0,p)+" for "+seconds(3,p)+".";
            case "soulbound_steps"->"Standing on soul sand or soul soil refreshes "+effect("Jump Boost",0,p)+" for "+seconds(2,p)+".";
            case "stormcaller"->"Thrown trident hits in rain deal +"+number(2*p.power())+" damage and show cosmetic lightning.";
            case "tempest"->"Thrown trident hits during a thunderstorm inflict "+effect("Slowness",1,p)+" for "+seconds(3,p)+".";
            case "wave_rider"->"Using Riptide grants Dolphin's Grace for "+seconds(4,p)+".";
            case "meteor"->"Mace smash attacks from at least 3 blocks deal +"+number(3*p.power())+" damage.";
            case "shockwave"->"Mace smash attacks from at least 3 blocks inflict "+effect("Weakness",0,p)+" for "+seconds(3,p)+".";
            case "comet"->"Mace smash attacks from at least 3 blocks deal +"+number(2*p.power())+" damage and ignite targets for "+seconds(5,p)+".";
            case "master_angler"->chance+" chance to prevent rod wear after a successful catch.";
            case "aquanaut"->"Being underwater refreshes Night Vision for "+seconds(12,p)+".";
            default->throw new IllegalArgumentException("Missing description: "+d.id());
        };
        return text+(p.cooldown()>0?" Cooldown: "+number(p.cooldown())+"s.":"");
    }
}
