package org.leap.fusionenchanting;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FusionEnchanting implements ModInitializer {
    public static final Logger LOGGER=LoggerFactory.getLogger("fusionenchanting");
    private static FusionRuntime runtime;
    @Override public void onInitialize(){
        try{runtime=new FusionRuntime(FabricLoader.getInstance().getConfigDir().resolve("fusionenchanting.json"));}
        catch(Exception error){throw new IllegalStateException("FusionEnchanting configuration could not be loaded",error);}
        LOGGER.info("FusionEnchanting Fabric: 36 book-only fusions, Minecraft 1.21 / Java 21");
    }
    public static FusionRuntime runtime(){if(runtime==null)throw new IllegalStateException("FusionEnchanting not initialized");return runtime;}
}
