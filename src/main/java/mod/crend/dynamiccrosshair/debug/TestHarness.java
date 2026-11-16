package mod.crend.dynamiccrosshair.debug;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import mod.crend.dynamiccrosshair.DynamicCrosshair;
import mod.crend.dynamiccrosshair.config.Config;
import mod.crend.dynamiccrosshair.bobbing.Bobbing;
import mod.crend.dynamiccrosshair.config.GuiDynamicCrosshairConfig;
import mod.crend.dynamiccrosshair.crosshair.CrosshairHandler;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Automated 1.7.10 client test. Activated by a {@code dynamiccrosshair-test.flag} file in the game
 * directory (or -Ddynamiccrosshair.test=true): creates a flat test world, poses the player through
 * every crosshair state, saves screenshots and shuts the game down.
 */
public class TestHarness {

    private static final int WAIT_TICKS = 12;

    private boolean checkedFlag;
    private boolean enabled;
    private boolean worldStarted;
    private boolean launchRequested;
    private int launchWait;
    private int ticks;
    private int state;
    private int waiter;
    private final List<String> results = new ArrayList<>();
    private EntityCow cow;

    public static void postInit() {
        DynamicCrosshair.LOGGER.info("Dynamic Crosshair test harness loaded (flag file: dynamiccrosshair-test.flag)");
    }

    private boolean isEnabled(Minecraft mc) {
        if (!checkedFlag) {
            checkedFlag = true;
            // Only ever active in a deobfuscated development environment: the flag file alone must
            // never be able to shut down a normal player's game. GTNH also loads the "mcp" mod, so
            // that check would be wrong there; use the launchwrapper blackboard instead.
            boolean development;
            try {
                Object deobf = net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment");
                development = Boolean.TRUE.equals(deobf);
            } catch (Throwable t) {
                development = false;
            }
            // -Ddynamiccrosshair.test.force=true is only used to run this harness inside a real
            // instance for one verification run; it is never set by the shipped game.
            boolean forced = Boolean.getBoolean("dynamiccrosshair.test.force");
            enabled = forced || (development && (Boolean.getBoolean("dynamiccrosshair.test")
                    || new File(mc.mcDataDir, "dynamiccrosshair-test.flag").exists()));
            if (enabled) DynamicCrosshair.LOGGER.info("Dynamic Crosshair test harness armed");
        }
        return enabled;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled(mc)) return;

        if (!worldStarted) {
            launchWait++;
            if (!launchRequested && mc.theWorld == null && mc.thePlayer == null
                    && mc.currentScreen instanceof net.minecraft.client.gui.GuiMainMenu) {
                WorldSettings settings = new WorldSettings(123456789L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT);
                settings.enableCommands();
                mc.launchIntegratedServer("dctest", "Dynamic Crosshair AutoTest", settings);
                launchRequested = true;
            }
            if (launchWait > 6000) {
                DynamicCrosshair.LOGGER.error("Test world did not load within 300 seconds, aborting");
                mc.shutdown();
            }
            if (mc.theWorld != null && mc.thePlayer != null && mc.currentScreen == null) {
                MinecraftServer server = mc.getIntegratedServer();
                String folder = server == null ? null : server.getFolderName();
                if (!"dctest".equals(folder)) {
                    // Never modify a world the harness did not create itself.
                    DynamicCrosshair.LOGGER.warn("Refusing to run the test harness in existing world folder '{}'", folder);
                    enabled = false;
                    return;
                }
                worldStarted = true;
                ticks = 0;
                // The automated window often loses focus; do not let vanilla open the pause menu.
                mc.gameSettings.pauseOnLostFocus = false;
                mc.displayGuiScreen(null);
                setupScene(mc);
                DynamicCrosshair.LOGGER.info("Test world ready at {},{},{}", (int) mc.thePlayer.posX, (int) mc.thePlayer.posY, (int) mc.thePlayer.posZ);
            }
            return;
        }

        ticks++;
        if (mc.currentScreen != null && state != 14) {
            mc.displayGuiScreen(null);
        }
        if (state == 7 && mc.thePlayer != null) {
            aimAtCow(mc.thePlayer);
        }
        if ((state == 9 || state == 10) && mc.thePlayer != null) {
            applyWalkingBob(mc.thePlayer);
        }
        if (waiter > 0) {
            waiter--;
            if (waiter == 0) {
                screenshot(mc, "state-" + state + "-" + stateName(state));
                logBobbing(mc);
                state++;
                if (state >= stateCount()) {
                    finish(mc);
                    return;
                }
                applyState(mc, state);
                waiter = state == 11 ? 3 : (state == 12 ? 1 : WAIT_TICKS);
            }
            return;
        }

        if (ticks == 1) {
            applyState(mc, 0);
            waiter = WAIT_TICKS;
        }
    }

    private int stateCount() {
        return 15;
    }

    private String stateName(int index) {
        switch (index) {
            case 0: return "air-empty";
            case 1: return "block-empty";
            case 2: return "chest-empty";
            case 3: return "block-pickaxe";
            case 4: return "air-sword";
            case 5: return "air-block";
            case 6: return "air-food";
            case 7: return "entity-empty";
            case 8: return "air-bow-drawing";
            case 9: return "bobbing-on";
            case 10: return "bobbing-off";
            case 11: return "attack-animation";
            case 12: return "place-shake";
            case 13: return "tic-crossbow";
            case 14: return "config-gui";
            default: return "state" + index;
        }
    }

    private int px;
    private int py;
    private int pz;
    private double cowX;
    private double cowZ;

    private void setupScene(Minecraft mc) {
        MinecraftServer server = mc.getIntegratedServer();
        if (server == null || server.worldServers.length == 0) {
            DynamicCrosshair.LOGGER.error("No integrated server world available for the test scene");
            return;
        }
        // Blocks must be changed on the server: client-side setBlock calls are overwritten by
        // chunk updates from the integrated server.
        World world = server.worldServers[0];
        EntityPlayer player = mc.thePlayer;
        px = (int) Math.floor(player.posX);
        py = (int) Math.floor(player.posY);
        pz = (int) Math.floor(player.posZ);

        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                world.setBlock(px + dx, py - 1, pz + dz, Blocks.stone);
                for (int dy = 0; dy <= 3; dy++) {
                    world.setBlockToAir(px + dx, py + dy, pz + dz);
                }
            }
        }
        world.setBlock(px + 3, py, pz, Blocks.chest);

        cowX = px + 0.5D;
        cowZ = pz + 5.5D;
        EntityCow entity = new EntityCow(world);
        entity.setPosition(cowX, py, cowZ);
        world.spawnEntityInWorld(entity);

        player.rotationPitch = 0.0F;
        player.rotationYaw = 0.0F;
        player.prevRotationPitch = 0.0F;
        player.prevRotationYaw = 0.0F;
        player.onGround = true;
        DynamicCrosshair.LOGGER.info("Test scene built at {},{},{}", px, py, pz);
    }

    private void applyState(Minecraft mc, int index) {
        EntityPlayer player = mc.thePlayer;
        World world = mc.theWorld;
        player.clearItemInUse();
        setItem(player, null);

        switch (index) {
            case 0:
                lookAtSky(player);
                break;
            case 1:
                lookAtFloor(player);
                break;
            case 2:
                lookAt(player, px + 3.5D, py + 0.5D, pz + 0.5D);
                break;
            case 3:
                setItem(player, new ItemStack(Items.diamond_pickaxe));
                lookAtFloor(player);
                break;
            case 4:
                setItem(player, new ItemStack(Items.diamond_sword));
                lookAtSky(player);
                break;
            case 5:
                setItem(player, new ItemStack(Blocks.dirt));
                lookAtSky(player);
                break;
            case 6:
                setItem(player, new ItemStack(Items.bread));
                lookAtSky(player);
                break;
            case 7:
                aimAtCow(player);
                break;
            case 8:
                setItem(player, new ItemStack(Items.bow));
                lookAtSky(player);
                // Half drawn: the central gap should be visible without scaling the crosshair.
                player.setItemInUse(player.getCurrentEquippedItem(), 36000);
                break;
            case 9:
                Config.bobbingEnabled = true;
                setItem(player, new ItemStack(Items.diamond_pickaxe));
                lookAtSky(player);
                applyWalkingBob(player);
                break;
            case 10:
                Config.bobbingEnabled = false;
                setItem(player, new ItemStack(Items.diamond_pickaxe));
                lookAtSky(player);
                applyWalkingBob(player);
                break;
            case 11:
                Config.bobbingEnabled = true;
                setItem(player, new ItemStack(Items.diamond_sword));
                lookAtSky(player);
                Bobbing.onAttack(mc);
                DynamicCrosshair.LOGGER.info("Attack animation triggered for the test");
                break;
            case 12:
                Config.bobbingEnabled = true;
                setItem(player, new ItemStack(Blocks.dirt));
                lookAtSky(player);
                Bobbing.onPlace();
                break;
            case 13:
                Config.bobbingEnabled = true;
                Item crossbow = cpw.mods.fml.common.registry.GameRegistry.findItem("tconstruct", "crossbow");
                if (crossbow != null) {
                    ItemStack stack = new ItemStack(crossbow);
                    stack.setTagCompound(new NBTTagCompound());
                    setItem(player, stack);
                }
                lookAtSky(player);
                break;
            case 14:
                mc.displayGuiScreen(new GuiDynamicCrosshairConfig(null));
                break;
            default:
                break;
        }
    }

    private static void setItem(EntityPlayer player, ItemStack stack) {
        player.inventory.mainInventory[0] = stack;
        player.inventory.currentItem = 0;
    }

    private static void applyWalkingBob(EntityPlayer player) {
        player.cameraYaw = 0.45F;
        player.distanceWalkedModified += 0.12F;
    }

    private void aimAtCow(EntityPlayer player) {
        lookAt(player, cowX, py + 0.8D, cowZ);
    }

    private void lookAtFloor(EntityPlayer player) {
        lookAt(player, px + 0.5D, py - 0.5D, pz + 2.5D);
    }

    private void lookAtSky(EntityPlayer player) {
        lookAt(player, px + 0.5D, py + 4.0D, pz + 8.0D);
    }

    private static void lookAt(EntityPlayer player, double x, double y, double z) {
        double dx = x - player.posX;
        double dy = y - (player.posY + player.getEyeHeight());
        double dz = z - player.posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontal) * 180.0D / Math.PI));
        player.rotationYaw = yaw;
        player.prevRotationYaw = yaw;
        player.rotationPitch = pitch;
        player.prevRotationPitch = pitch;
    }

    private void logBobbing(Minecraft mc) {
        Bobbing.Transform transform = Bobbing.compute(mc, 1.0F, mc.displayWidth, mc.displayHeight);
        DynamicCrosshair.LOGGER.info("Bobbing transform dx={} dy={} rotation={} scale={}/{}",
                transform.dx, transform.dy, transform.rotation, transform.scaleX, transform.scaleY);
    }

    private void screenshot(Minecraft mc, String name) {
        try {
            if (mc.currentScreen instanceof GuiDynamicCrosshairConfig) {
                name = "state-" + state + "-config-gui";
            }
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            String hit = mc.objectMouseOver == null ? "null"
                    : mc.objectMouseOver.typeOfHit + (mc.objectMouseOver.entityHit != null ? ":" + mc.objectMouseOver.entityHit.getClass().getSimpleName() : "");
            results.add(name + " hit=" + hit + " state=" + CrosshairHandler.getState() + " show=" + CrosshairHandler.shouldShow());
            DynamicCrosshair.LOGGER.info("Screenshot saved: {} hit={} state={} show={}", name, hit,
                    CrosshairHandler.getState(), CrosshairHandler.shouldShow());
        } catch (Throwable t) {
            DynamicCrosshair.LOGGER.error("Screenshot failed", t);
            results.add(name + " FAILED " + t);
        }
    }

    private void finish(Minecraft mc) {
        Config.bobbingEnabled = true;
        try {
            if (mc.currentScreen instanceof GuiDynamicCrosshairConfig) mc.displayGuiScreen(null);
            File out = new File(mc.mcDataDir, "dynamiccrosshair-test-results.txt");
            try (PrintWriter writer = new PrintWriter(new FileWriter(out))) {
                writer.println("Dynamic Crosshair 1.7.10 automated test");
                writer.println("states=" + stateCount());
                for (String line : results) writer.println(line);
            }
            new File(mc.mcDataDir, "dynamiccrosshair-test.done").createNewFile();
        } catch (Exception e) {
            DynamicCrosshair.LOGGER.error("Failed to write test results", e);
        }
        DynamicCrosshair.LOGGER.info("Dynamic Crosshair test finished, shutting down");
        mc.shutdown();
    }
}
