package mod.crend.dynamiccrosshair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = DynamicCrosshair.MOD_ID,
        name = DynamicCrosshair.MOD_NAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.7.10]",
        acceptableRemoteVersions = "*",
        guiFactory = "mod.crend.dynamiccrosshair.config.GuiFactory")
public class DynamicCrosshair {

    public static final String MOD_ID = "dynamiccrosshair";
    public static final String MOD_NAME = "Dynamic Crosshair";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @SidedProxy(
            clientSide = "mod.crend.dynamiccrosshair.ClientProxy",
            serverSide = "mod.crend.dynamiccrosshair.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
