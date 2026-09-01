package tech.muddykat.engineered_schematics;

import tech.muddykat.engineered_schematics.common.CommonProxy;
import tech.muddykat.engineered_schematics.event.IMCReceiver;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@Mod(modid = EngineeredSchematics.MODID, name = EngineeredSchematics.NAME, acceptedMinecraftVersions = "[1.12.2,1.13)", dependencies = "required-after:immersiveengineering@[0.12-92,);required-after:forge@[14.23.5.2847,);")
public class EngineeredSchematics {
    public static final String MODID = "engineered_schematics";
    public static final String NAME = "Engineered Schematics";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(MODID) {
        @Override @Nonnull public ItemStack createIcon() { return new ItemStack(ESRegistry.SCHEMATIC_ITEM); }
    };
    private static final Map<String, ItemStack> ES_FORMATION_TEMPLATE = new HashMap<>();
    @SidedProxy(clientSide = "tech.muddykat.engineered_schematics.client.ClientProxy", serverSide = "tech.muddykat.engineered_schematics.common.CommonProxy")
    public static CommonProxy proxy;
    @Instance(MODID) public static EngineeredSchematics instance;

    @EventHandler public void preInit(FMLPreInitializationEvent event) { proxy.preInit(); }

    @EventHandler public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, proxy);
        proxy.init();
    }

    @EventHandler public void postInit(FMLPostInitializationEvent event) { proxy.postInit(); }

    @EventHandler public void processIMC(FMLInterModComms.IMCEvent event) { IMCReceiver.processIMC(event); }

    public static ResourceLocation rl(String path) { return new ResourceLocation(MODID, path); }

    public static ResourceLocation makeTextureLocation(String name) { return new ResourceLocation(MODID, "textures/gui/" + name + ".png"); }

    public static void setTemplateFormationItem(String uniqueName, ItemStack item) { ES_FORMATION_TEMPLATE.put(uniqueName, item); }

    public static boolean hasFormationItem(String uniqueName) { return ES_FORMATION_TEMPLATE.containsKey(uniqueName); }

    public static ItemStack getFormationItem(String uniqueName) { return ES_FORMATION_TEMPLATE.get(uniqueName); }
}
