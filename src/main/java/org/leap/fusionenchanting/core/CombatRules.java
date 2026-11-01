package org.leap.fusionenchanting.core;

import java.util.*;

/** Pure conditions/bonus arithmetic, shared by runtime planning and regression cases. */
public final class CombatRules {
    public record Facts(boolean charged,boolean projectile,boolean trident,float fall,boolean burning,
                        boolean undead,boolean arthropod,boolean wet,boolean thunder,double healthFraction){}
    public static final Set<String> IDS=Set.of("infernal_edge","blade_storm","brutal_force","executioner","holy_flame","scorched_venom","flame_burst",
            "hellfire","impact","blast_arrow","volley","railshot","stormcaller","tempest","meteor","shockwave","comet");
    private CombatRules(){}
    public static OptionalDouble bonus(String id,Facts f,double power){
        boolean full=!f.projectile()&&f.charged();double result;
        switch(id){
            case "infernal_edge"->{if(!full)return OptionalDouble.empty();result=f.burning()?2:0;}
            case "blade_storm"->{if(!full)return OptionalDouble.empty();result=0;}
            case "brutal_force"->{if(!full)return OptionalDouble.empty();result=1;}
            case "executioner"->{if(!full||f.healthFraction()>.25)return OptionalDouble.empty();result=4;}
            case "holy_flame"->{if(!full||!f.undead())return OptionalDouble.empty();result=3;}
            case "scorched_venom"->{if(!full||!f.arthropod())return OptionalDouble.empty();result=0;}
            case "flame_burst"->{if(!full||!f.burning())return OptionalDouble.empty();result=0;}
            case "hellfire"->{if(!f.projectile()||!f.charged())return OptionalDouble.empty();result=2;}
            case "impact"->{if(!f.projectile()||!f.charged())return OptionalDouble.empty();result=0;}
            case "blast_arrow"->{if(!f.projectile()||!f.charged())return OptionalDouble.empty();result=1;}
            case "volley"->{if(!f.projectile())return OptionalDouble.empty();result=0;}
            case "railshot"->{if(!f.projectile())return OptionalDouble.empty();result=3;}
            case "stormcaller"->{if(!f.trident()||!f.wet())return OptionalDouble.empty();result=2;}
            case "tempest"->{if(!f.trident()||!f.thunder())return OptionalDouble.empty();result=0;}
            case "meteor"->{if(f.projectile()||f.fall()<3)return OptionalDouble.empty();result=3;}
            case "shockwave"->{if(f.projectile()||f.fall()<3)return OptionalDouble.empty();result=0;}
            case "comet"->{if(f.projectile()||f.fall()<3)return OptionalDouble.empty();result=2;}
            default->{return OptionalDouble.empty();}
        }
        return OptionalDouble.of(result*power);
    }
    /** Converts XP already scheduled to drop; never creates repair without spending existing XP. */
    public static int repairPoints(int available,int damage){return Math.min(2,Math.min(Math.max(0,available),Math.max(0,(damage+1)/2)));}
}
