package org.leap.fusionenchanting.config;

import com.google.gson.*;
import org.leap.fusionenchanting.core.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Immutable, strictly validated settings. Loading never mutates the running configuration. */
public record FusionConfig(boolean pvp, Set<String> disabledWorlds, Map<String,FusionTuning> tunings) {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public FusionConfig { disabledWorlds=Set.copyOf(disabledWorlds);tunings=Map.copyOf(tunings); }
    public static FusionConfig defaults(){
        Map<String,FusionTuning> map=new LinkedHashMap<>();FusionCatalog.definitions().forEach(d->map.put(d.id(),d.defaults()));
        return new FusionConfig(false,Set.of(),map);
    }
    public static FusionConfig load(Path path) throws IOException {
        if(!Files.exists(path))writeDefaults(path);
        if(Files.size(path)>262144)throw new IllegalArgumentException("Configuration exceeds 256 KiB");
        return parse(Files.readString(path));
    }
    public static FusionConfig parse(String text){
        JsonElement parsed=JsonParser.parseString(text);
        if(!parsed.isJsonObject())throw new IllegalArgumentException("Config must be an object");
        JsonObject root=parsed.getAsJsonObject();keys(root,Set.of("config-version","allow-pvp-abilities","disabled-worlds","fusions"));
        if(integer(root,"config-version",1)!=1)throw new IllegalArgumentException("Unsupported config-version");
        boolean pvp=bool(root,"allow-pvp-abilities",false);Set<String> worlds=new HashSet<>();
        if(root.has("disabled-worlds")){
            if(!root.get("disabled-worlds").isJsonArray())throw new IllegalArgumentException("disabled-worlds must be an array");
            for(JsonElement e:root.getAsJsonArray("disabled-worlds")){
                if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IllegalArgumentException("World names must be strings");
                worlds.add(e.getAsString());
            }
        }
        JsonObject fusions=root.has("fusions")?object(root.get("fusions")):new JsonObject();
        keys(fusions,defaults().tunings().keySet());Map<String,FusionTuning> map=new LinkedHashMap<>();
        for(var d:FusionCatalog.definitions()){
            JsonObject section=fusions.has(d.id())?object(fusions.get(d.id())):new JsonObject();
            keys(section,Set.of("enabled","extra-levels","cooldown-seconds","chance","power"));
            map.put(d.id(),new FusionTuning(bool(section,"enabled",true),integer(section,"extra-levels",d.extraLevels()),
                    number(section,"cooldown-seconds",d.cooldown()),number(section,"chance",d.chance()),number(section,"power",d.power())));
        }
        return new FusionConfig(pvp,worlds,map);
    }
    public static void writeDefaults(Path path)throws IOException{
        JsonObject root=new JsonObject();root.addProperty("config-version",1);root.addProperty("allow-pvp-abilities",false);root.add("disabled-worlds",new JsonArray());
        JsonObject recipes=new JsonObject();
        for(var d:FusionCatalog.definitions()){
            JsonObject s=new JsonObject();s.addProperty("enabled",true);s.addProperty("extra-levels",d.extraLevels());s.addProperty("cooldown-seconds",d.cooldown());
            s.addProperty("chance",d.chance());s.addProperty("power",d.power());recipes.add(d.id(),s);
        }
        root.add("fusions",recipes);Path absolute=path.toAbsolutePath();Files.createDirectories(absolute.getParent());
        Path temp=Files.createTempFile(absolute.getParent(),"fusion-config-",".tmp");
        try{
            Files.writeString(temp,JSON.toJson(root)+"\n");
            try{Files.move(temp,absolute,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,absolute);}
        }finally{Files.deleteIfExists(temp);}
    }
    private static JsonObject object(JsonElement e){if(!e.isJsonObject())throw new IllegalArgumentException("Expected config object");return e.getAsJsonObject();}
    private static void keys(JsonObject o,Set<String> allowed){for(String key:o.keySet())if(!allowed.contains(key))throw new IllegalArgumentException("Unknown setting: "+key);}
    private static boolean bool(JsonObject o,String k,boolean fallback){
        if(!o.has(k))return fallback;JsonElement e=o.get(k);
        if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException(k+" must be boolean");return e.getAsBoolean();
    }
    private static double number(JsonObject o,String k,double fallback){
        if(!o.has(k))return fallback;JsonElement e=o.get(k);
        if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber()||!Double.isFinite(e.getAsDouble()))throw new IllegalArgumentException(k+" must be finite number");return e.getAsDouble();
    }
    private static int integer(JsonObject o,String k,int fallback){double n=number(o,k,fallback);if(n!=Math.rint(n)||n<Integer.MIN_VALUE||n>Integer.MAX_VALUE)throw new IllegalArgumentException(k+" must be an integer");return (int)n;}
}
