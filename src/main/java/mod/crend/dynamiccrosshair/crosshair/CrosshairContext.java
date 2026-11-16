package mod.crend.dynamiccrosshair.crosshair;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/** Snapshot of everything the evaluator needs for one frame. */
public class CrosshairContext {

    public final Minecraft mc;
    public final EntityPlayer player;
    public final World world;
    public final MovingObjectPosition target;
    public final ItemStack held;

    public final boolean withBlock;
    public final boolean withEntity;
    public final Block block;
    public final int meta;
    public final int x;
    public final int y;
    public final int z;
    public final Entity entity;

    public CrosshairContext(Minecraft mc) {
        this.mc = mc;
        this.player = mc.thePlayer;
        this.world = mc.theWorld;
        this.target = mc.objectMouseOver;
        this.held = player == null ? null : player.getCurrentEquippedItem();

        boolean blockHit = target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
        boolean entityHit = target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY;
        this.withEntity = entityHit && target.entityHit != null;
        this.withBlock = blockHit && !withEntity && world != null;
        this.entity = withEntity ? target.entityHit : null;
        if (withBlock) {
            this.x = target.blockX;
            this.y = target.blockY;
            this.z = target.blockZ;
            this.block = world.getBlock(x, y, z);
            this.meta = world.getBlockMetadata(x, y, z);
        } else {
            this.x = this.y = this.z = 0;
            this.block = null;
            this.meta = 0;
        }
    }

    public boolean shouldInteract() {
        return player != null;
    }

    public String blockId() {
        if (block == null) return "";
        Object name = Block.blockRegistry.getNameForObject(block);
        return name == null ? "" : name.toString();
    }

    public String itemId() {
        if (held == null) return "";
        Object name = net.minecraft.item.Item.itemRegistry.getNameForObject(held.getItem());
        return name == null ? "" : name.toString();
    }

    public int itemMeta() {
        return held == null ? 0 : held.getItemDamage();
    }
}
