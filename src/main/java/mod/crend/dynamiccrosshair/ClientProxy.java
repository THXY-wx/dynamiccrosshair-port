package mod.crend.dynamiccrosshair;

import mod.crend.dynamiccrosshair.config.Config;
import mod.crend.dynamiccrosshair.debug.TestHarness;
import mod.crend.dynamiccrosshair.render.CrosshairOverlay;
import mod.crend.dynamiccrosshair.render.CrosshairRenderer;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        Config.load(event.getSuggestedConfigurationFile());
        CrosshairRenderer.init();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new CrosshairOverlay());
        FMLCommonHandler.instance().bus().register(new ClientEventHandler());
        FMLCommonHandler.instance().bus().register(new TestHarness());
        ClientEventHandler.registerKeyBinding();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        TestHarness.postInit();
    }
}
