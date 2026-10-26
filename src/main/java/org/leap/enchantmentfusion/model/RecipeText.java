package org.leap.enchantmentfusion.model;

import java.util.*;

/** Compact book/chat labels; conservative wrapping fits the default vanilla book font. */
public final class RecipeText {
    private RecipeText() {}
    public static String enchantName(String key) {
        StringBuilder result=new StringBuilder();
        for(String part:key.split("_")) {
            if(!result.isEmpty())result.append(' ');
            result.append(Set.of("of","the").contains(part)?part:Character.toUpperCase(part.charAt(0))+part.substring(1));
        }
        return result.toString();
    }
    public static String ingredient(String key,int level) {
        if(level<1||level>10)throw new IllegalArgumentException("Enchantment level outside guide range");
        return enchantName(key)+" "+List.of("I","II","III","IV","V","VI","VII","VIII","IX","X").get(level-1);
    }
    public static String equation(FusionDefinition d) {
        return d.ingredients().entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .map(e->ingredient(e.getKey(),e.getValue())).collect(java.util.stream.Collectors.joining(" + "))+" → "+d.name()+" I";
    }
    public static List<String> wrap(String text,int width) {
        if(width<1||width>80)throw new IllegalArgumentException("Invalid guide width");
        List<String> lines=new ArrayList<>();
        for(String paragraph:text.split("\\R",-1)) {
            if(paragraph.isEmpty()){lines.add("");continue;}
            StringBuilder line=new StringBuilder();
            for(String token:paragraph.split("\\s+")) {
                while(token.length()>width) {
                    if(!line.isEmpty()){lines.add(line.toString());line.setLength(0);}
                    lines.add(token.substring(0,width));token=token.substring(width);
                }
                if(token.isEmpty())continue;
                if(!line.isEmpty()&&line.length()+1+token.length()>width){lines.add(line.toString());line.setLength(0);}
                if(!line.isEmpty())line.append(' ');line.append(token);
            }
            if(!line.isEmpty())lines.add(line.toString());
        }
        return List.copyOf(lines);
    }
}
