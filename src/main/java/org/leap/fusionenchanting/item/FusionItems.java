package org.leap.fusionenchanting.item;

import org.leap.fusionenchanting.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.*;
import java.util.*;
import java.util.stream.Collectors;

/** Vanilla custom_data and lore components; no custom item/registry entries sent to clients. */
public final class FusionItems {
    public record State(FusionDefinition definition,UUID instance,Map<String,Integer> sources){public int level(){return FusionLevels.level(definition,sources);}}
    public static final String DATA="fusionenchanting",LORE="leap:enchantment_fusion";
    private final Map<String,FusionDefinition> definitions=FusionCatalog.definitions().stream().collect(Collectors.toUnmodifiableMap(FusionDefinition::id,d->d));
    private java.util.function.Function<String,FusionTuning> tunings=id->definitions.get(id).defaults();
    public void tunings(java.util.function.Function<String,FusionTuning> value){tunings=Objects.requireNonNull(value);}
    public FusionDefinition definition(String id){return definitions.get(id);}
    public boolean marked(ItemStack item){return item!=null&&!item.isEmpty()&&item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).contains(DATA);}
    public static boolean book(ItemStack item){return item!=null&&item.is(Items.ENCHANTED_BOOK)&&item.getCount()==1;}
    public static Map<String,Integer> enchantments(ItemStack item){
        if(item==null||item.isEmpty())return Map.of();Map<String,Integer> result=new HashMap<>();
        for(var e:enchants(item).entrySet())e.getKey().unwrapKey().ifPresent(key->{
            if(key.location().getNamespace().equals("minecraft"))result.put(key.location().getPath(),e.getIntValue());
        });return result;
    }
    public static ItemEnchantments enchants(ItemStack item){return item.getOrDefault(item.is(Items.ENCHANTED_BOOK)?DataComponents.STORED_ENCHANTMENTS:DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);}
    public static boolean legalSources(ItemStack item,FusionDefinition definition){
        for(var e:enchants(item).entrySet())if(e.getKey().unwrapKey().filter(k->k.location().getNamespace().equals("minecraft")&&definition.ingredients().containsKey(k.location().getPath())).isPresent()
                &&e.getIntValue()>e.getKey().value().getMaxLevel())return false;return true;
    }
    public Optional<State> read(ItemStack item){
        if(!marked(item)||item.getCount()!=1)return Optional.empty();CompoundTag root=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(!root.contains(DATA,Tag.TAG_COMPOUND))return Optional.empty();CompoundTag data=root.getCompound(DATA);
        if(!data.contains("schema",Tag.TAG_INT)||data.getInt("schema")!=1||!data.contains("id",Tag.TAG_STRING)||!data.contains("instance",Tag.TAG_STRING)||!data.contains("sources",Tag.TAG_COMPOUND))return Optional.empty();
        String id=data.getString("id"),identity=data.getString("instance");if(id.length()>48||identity.length()!=36)return Optional.empty();
        FusionDefinition d=definition(id);if(d==null||!book(item)&&!accepts(d.equipment(),item))return Optional.empty();
        CompoundTag bound=data.getCompound("sources");if(!bound.getAllKeys().equals(d.ingredients().keySet())||!legalSources(item,d))return Optional.empty();
        var actual=enchantments(item);Map<String,Integer> sources=new HashMap<>();
        for(var e:d.ingredients().entrySet()){
            if(!bound.contains(e.getKey(),Tag.TAG_INT))return Optional.empty();int n=bound.getInt(e.getKey());
            if(n<1||actual.getOrDefault(e.getKey(),0)!=n)return Optional.empty();sources.put(e.getKey(),n);
        }
        try{return Optional.of(new State(d,UUID.fromString(identity),Map.copyOf(sources)));}catch(IllegalArgumentException e){return Optional.empty();}
    }
    public ItemStack fuse(ItemStack nativeResult,FusionDefinition d,UUID identity){
        ItemStack result=nativeResult.copyWithCount(1);
        CompoundTag previous=result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(DATA);
        boolean originallyVisible=previous.contains("source_tooltip",Tag.TAG_BYTE)?previous.getBoolean("source_tooltip"):enchants(result).showInTooltip;
        CompoundTag data=new CompoundTag();data.putBoolean("source_tooltip",originallyVisible);data.putInt("schema",1);data.putString("id",d.id());data.putString("instance",identity.toString());
        CompoundTag sources=new CompoundTag();var actual=enchantments(result);d.ingredients().keySet().forEach(key->sources.putInt(key,actual.getOrDefault(key,0)));data.put("sources",sources);
        CustomData.update(DataComponents.CUSTOM_DATA,result,root->root.put(DATA,data));
        result.set(book(result)?DataComponents.STORED_ENCHANTMENTS:DataComponents.ENCHANTMENTS,enchants(result).withTooltip(false));
        int level=Math.max(1,FusionLevels.level(d,actual));
        List<Component> lore=ownLoreRemoved(result);lore.add(line(d.name()+" "+FusionLevels.roman(level),ChatFormatting.GOLD));
        if(book(result))lore.add(line("Applies to: "+d.equipment().label(),ChatFormatting.DARK_GRAY));
        RecipeText.wrap(FusionDescriptions.describe(d,FusionLevels.profile(d,tunings.apply(d.id()),level)),50).forEach(text->lore.add(line(text,ChatFormatting.GRAY)));result.set(DataComponents.LORE,new ItemLore(lore));return result;
    }
    public ItemStack strip(ItemStack input){
        ItemStack result=input.copy();CompoundTag data=result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(DATA);
        if(data.contains("source_tooltip",Tag.TAG_BYTE))result.set(book(result)?DataComponents.STORED_ENCHANTMENTS:DataComponents.ENCHANTMENTS,enchants(result).withTooltip(data.getBoolean("source_tooltip")));
        CustomData.update(DataComponents.CUSTOM_DATA,result,root->root.remove(DATA));
        List<Component> lore=ownLoreRemoved(result);if(lore.isEmpty())result.remove(DataComponents.LORE);else result.set(DataComponents.LORE,new ItemLore(lore));return result;
    }
    private static List<Component> ownLoreRemoved(ItemStack item){return new ArrayList<>(item.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines().stream().filter(line->!LORE.equals(line.getStyle().getInsertion())).toList());}
    private static Component line(String text,ChatFormatting color){return Component.literal(text).withStyle(s->s.withColor(color).withItalic(false).withInsertion(LORE));}
    public static boolean accepts(Equipment category,ItemStack item){return category.accepts(BuiltInRegistries.ITEM.getKey(item.getItem()).getPath().toUpperCase(Locale.ROOT));}
    public static ItemStack ingredientBook(HolderLookup.Provider registries,String key,int level){
        ItemStack book=new ItemStack(Items.ENCHANTED_BOOK);var enchants=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchants.set(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.withDefaultNamespace(key))),level);
        book.set(DataComponents.STORED_ENCHANTMENTS,enchants.toImmutable());return book;
    }
    public static ItemStack sourceBook(HolderLookup.Provider registries,FusionDefinition d){
        return sourceBook(registries,d.ingredients());
    }
    public static ItemStack sourceBook(HolderLookup.Provider registries,FusionDefinition d,int tier){
        return sourceBook(registries,FusionLevels.example(d,tier).orElseThrow());
    }
    private static ItemStack sourceBook(HolderLookup.Provider registries,Map<String,Integer> ingredients){
        ItemStack book=new ItemStack(Items.ENCHANTED_BOOK);var enchants=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        ingredients.forEach((key,level)->enchants.set(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.withDefaultNamespace(key))),level));
        book.set(DataComponents.STORED_ENCHANTMENTS,enchants.toImmutable());return book;
    }
}
