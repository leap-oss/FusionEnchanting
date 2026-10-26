package org.leap.enchantmentfusion.model;

import org.leap.enchantmentfusion.service.Cooldowns;
import java.util.*;

/** Book-only recipe, native XP surcharge, balance bounds and shared-cooldown regressions. */
public final class CoreRulesTest {
    private static int checks;
    public static void main(String[] args) {
        var catalog = FusionCatalog.definitions();
        check(catalog.size() == 36, "Catalogue incomplete");
        check(catalog.stream().map(FusionDefinition::id).distinct().count() == 36, "Duplicate fusion ID");
        check(catalog.stream().map(d -> d.ingredients().keySet()).distinct().count() == 36, "Duplicate source pair");
        for (var d : catalog) {
            var keys = new ArrayList<>(d.ingredients().keySet());
            Map<String,Integer> left = Map.of(keys.get(0), d.ingredients().get(keys.get(0))), right = Map.of(keys.get(1), d.ingredients().get(keys.get(1)));
            check(FusionRules.matchesBooks(d, left, right, d.ingredients(), false, d.defaults()), "Two source books should fuse: " + d.id());
            check(FusionRules.matchesBooks(d, right, left, d.ingredients(), false, d.defaults()), "Book order broke matching");
            check(!FusionRules.matchesBooks(d, Map.of(), d.ingredients(), d.ingredients(), false, d.defaults()), "One book supplied both sources without contribution");
            check(!FusionRules.matchesBooks(d, left, right, d.ingredients(), true, d.defaults()), "Two fusion identities could combine");
            check(!FusionRules.matchesBooks(d, left, right, d.ingredients(), false, new FusionTuning(false,d.extraLevels(),d.cooldown(),d.chance(),d.power())), "Disabled fusion matched");
            for (var key : keys) {
                var low = new HashMap<>(d.ingredients()); low.compute(key,(k,n)->n-1);
                check(!FusionRules.matchesBooks(d,left,right,low,false,d.defaults()), "Insufficient source level matched");
            }
            check(d.extraLevels() >= 2 && d.extraLevels() <= 5, "Default surcharge outside vanilla profile");
            check(FusionRules.creationCost(4,d.extraLevels()) == 4 + d.extraLevels(), "Vanilla base cost replaced instead of added");
        }
        check(FusionRules.creationCost(37,3) == 40, "Too Expensive cap was silently bypassed");
        check(FusionRules.creationCost(Integer.MAX_VALUE,12) == Integer.MAX_VALUE, "XP cost overflow");
        check(FusionRules.creationCost(5,0) == 5, "Zero surcharge must retain vanilla cost");
        invalid(() -> FusionRules.creationCost(-1,3));
        invalid(() -> new FusionTuning(true,13,1,1,1));
        invalid(() -> new FusionTuning(true,-1,1,1,1));
        invalid(() -> new FusionTuning(true,3,Double.NaN,1,1));
        invalid(() -> new FusionTuning(true,3,1,Double.POSITIVE_INFINITY,1));
        invalid(() -> new FusionTuning(true,3,1,1,-1));
        invalid(() -> new FusionTuning(true,3,1,1.01,1));
        var byId = new HashMap<String,FusionDefinition>(); catalog.forEach(d -> byId.put(d.id(),d));
        for (String id : List.of("workhorse","miners_luck","preservation","fortified","master_angler"))
            check(byId.get(id).chance() <= .20,"Excessive additive durability bonus");
        check(byId.get("fortified").chance() == .10, "Armor wear balance regressed");
        long[] now = {0}; var cooldowns = new Cooldowns(() -> now[0]); UUID player = UUID.randomUUID();
        check(cooldowns.ready(player,"volley"),"New ability not ready"); cooldowns.start(player,"volley",8);
        check(!cooldowns.ready(player,"volley"),"Weapon swap bypassed cooldown");
        check(cooldowns.ready(player,"impact"),"Different fusion incorrectly blocked");
        check(cooldowns.ready(UUID.randomUUID(),"volley"),"Different player incorrectly blocked");
        now[0]=7_999_999_999L; check(!cooldowns.ready(player,"volley"),"Early cooldown expiry");
        now[0]++; cooldowns.prune(); check(cooldowns.ready(player,"volley"),"Cooldown never expired");
        System.out.println("CORE TESTS PASS: " + checks + " checks across 36 book-only recipes, XP rules, balance bounds and cooldowns.");
    }
    private static void check(boolean condition,String message) { checks++; if (!condition) throw new AssertionError(message); }
    private static void invalid(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Invalid value accepted");
    }
}
