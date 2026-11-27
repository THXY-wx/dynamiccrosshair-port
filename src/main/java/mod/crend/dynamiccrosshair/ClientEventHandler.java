package mod.crend.dynamiccrosshair;

import mod.crend.dynamiccrosshair.bobbing.Bobbing;
import mod.crend.dynamiccrosshair.compat.CrosshairSelectionCompat;
import mod.crend.dynamiccrosshair.config.GuiDynamicCrosshairConfig;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ClientEventHandler {

    public static final KeyBinding CONFIG_KEY = new KeyBinding(
            "key.dynamiccrosshair.config", Keyboard.KEY_F9, "key.categories.dynamiccrosshair");

    private int pendingX;
    private int pendingY;
    private int pendingZ;
    private int pendingFace;
    private int pendingTicks;
    private boolean pendingReplaceable;
    private Block pendingBlock;

    public static void registerKeyBinding() {
        ClientRegistry.registerKeyBinding(CONFIG_KEY);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            CrosshairSelectionCompat.restoreAfterFrame();
        }
        Bobbing.tick(mc);
    }

    @SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.LOWEST)
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            CrosshairSelectionCompat.suspendForFrame();
        } else {
            try {
                mod.crend.dynamiccrosshair.render.CrosshairOverlay.renderFrame(
                        Minecraft.getMinecraft(), event.renderTickTime);
            } finally {
                // Restore in the same frame; leaving it false across the tick boundary let
                // Crosshair-Selection save enabled=false when its config screen was opened.
                CrosshairSelectionCompat.restoreAfterFrame();
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (CONFIG_KEY.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.displayGuiScreen(new GuiDynamicCrosshairConfig(mc.currentScreen));
        }
    }

    /**
     * A right click only becomes a placement if the block actually appears. Remember the click and
     * verify on the next ticks so opening a chest with a block in hand does not trigger the ring.
     */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (event.entityPlayer != mc.thePlayer || event.world == null || !event.world.isRemote) return;
        if (event.isCanceled() || event.useItem == Event.Result.DENY) return;
        ItemStack held = event.entityPlayer.getCurrentEquippedItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) return;
        Block block = Block.getBlockFromItem(held.getItem());
        if (block == null) return;
        pendingX = event.x;
        pendingY = event.y;
        pendingZ = event.z;
        pendingFace = event.face;
        pendingReplaceable = event.world.getBlock(event.x, event.y, event.z)
                .isReplaceable(event.world, event.x, event.y, event.z);
        pendingBlock = block;
        pendingTicks = 5;
    }

    private void checkPendingPlacement(Minecraft mc) {
        if (pendingTicks <= 0 || pendingBlock == null || mc.theWorld == null) return;
        pendingTicks--;
        ForgeDirection direction = ForgeDirection.getOrientation(pendingFace);
        int x = pendingX + direction.offsetX;
        int y = pendingY + direction.offsetY;
        int z = pendingZ + direction.offsetZ;
        // Blocks placed into a replaceable target (tall grass, snow, ...) go into the clicked
        // position; otherwise they go on the clicked face.
        boolean placed = pendingReplaceable
                ? mc.theWorld.getBlock(pendingX, pendingY, pendingZ) == pendingBlock
                : mc.theWorld.getBlock(x, y, z) == pendingBlock;
        if (placed) {
            Bobbing.onPlace();
            pendingTicks = 0;
            pendingBlock = null;
        } else if (pendingTicks == 0) {
            pendingBlock = null;
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        int button = Mouse.getEventButton();
        boolean state = Mouse.getEventButtonState();
        if (button == 0 && state) {
            Bobbing.onAttack(mc);
        } else if (button == 1 && state) {
            // Placement feedback is now driven by the right mouse button itself, so it also fires
            // when the click does not leave a block behind (tools, chests, air).
            Bobbing.onPlace();
        }
    }
}
