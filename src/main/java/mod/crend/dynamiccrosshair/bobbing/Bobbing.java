package mod.crend.dynamiccrosshair.bobbing;

import mod.crend.dynamiccrosshair.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * 1.7.10 implementation of CrosshairBobbingMod's core effects: view bobbing, sneak rotation,
 * item switch shift, attack kick, eating wobble and bow draw scaling.
 */
public final class Bobbing {

    private static final float MIN_ATTACK_SCALE = 1.4F;
    private static final float MAX_ATTACK_SCALE = 2.0F;
    private static final float MIN_ATTACK_DEGREES = 15.0F;
    private static final float MAX_ATTACK_DEGREES = 25.0F;
    private static final long ATTACK_ANIM_LEN = 200L;
    private static final long CRIT_ANIM_LEN = 300L;
    private static final float CRIT_SCALE = 2.0F;
    private static final long ITEM_SWITCH_ANIM_LEN = 180L;

    private static long attackStart;
    private static boolean crit;
    private static int critDirection = 1;
    private static float attackDamage = 0.6F;
    private static float attackDegrees = 20.0F;
    private static float attackScale = 1.7F;
    private static long itemSwitchStart;
    private static long placeStart;
    private static float placeAmplitude;
    private static int lastSlot = -1;
    private static float sneakProgress;

    private Bobbing() {}

    public static void tick(Minecraft mc) {
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;
        float target = player.isSneaking() ? 1.0F : 0.0F;
        sneakProgress += (target - sneakProgress) * 0.25F;
        int slot = player.inventory.currentItem;
        if (lastSlot == -1) lastSlot = slot;
        if (slot != lastSlot) {
            lastSlot = slot;
            itemSwitchStart = System.currentTimeMillis();
        }
    }

    /** Placement feedback: a short, damped wobble instead of a ring. */
    public static void onPlace() {
        placeStart = System.currentTimeMillis();
        placeAmplitude = 11.0F + (float) Math.random() * 6.0F;
        if (Math.random() < 0.5D) placeAmplitude = -placeAmplitude;
    }

    public static void onAttack(Minecraft mc) {
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;
        boolean critical = player.fallDistance > 0.0F && !player.onGround && !player.isOnLadder()
                && !player.isInWater() && !player.isRiding() && !player.isSneaking();
        float random = (float) Math.random();
        long now = System.currentTimeMillis();
        if (critical || !crit || now > attackStart + CRIT_ANIM_LEN) {
            attackDamage = Math.min(1.0F, 0.55F + random * 0.45F);
            attackStart = now;
            attackDegrees = random * (MAX_ATTACK_DEGREES - MIN_ATTACK_DEGREES) + MIN_ATTACK_DEGREES;
            attackScale = random * (MAX_ATTACK_SCALE - MIN_ATTACK_SCALE) + MIN_ATTACK_SCALE;
            crit = critical;
            critDirection *= -1;
        }
    }

    public static Transform compute(Minecraft mc, float partialTicks, int width, int height) {
        Transform transform = new Transform();
        EntityPlayer player = mc.thePlayer;
        if (player == null || !Config.bobbingEnabled) return transform;

        float strength = Config.bobStrength;
        float distance = centerDepth(mc, player, partialTicks);
        float tanHalfFov = (float) Math.tan(Math.toRadians(mc.gameSettings.fovSetting) * 0.5D);
        if (tanHalfFov < 0.05F) tanHalfFov = 0.05F;
        float aspect = displayAspect(mc);

        if (Config.bobView && mc.gameSettings.viewBobbing) {
            Matrix4f matrix = viewBobbingMatrix(player, partialTicks);
            Vector4f point = Matrix4f.transform(matrix, new Vector4f(0.0F, 0.0F, -distance, 1.0F), null);
            if (point != null && point.z < -0.001F) {
                float ndcX = (point.x / -point.z) * (1.0F / tanHalfFov) / aspect;
                float ndcY = (point.y / -point.z) * (1.0F / tanHalfFov);
                transform.dx += ndcX * (width * 0.5F) * strength;
                transform.dy += -ndcY * (height * 0.5F) * strength;
            }
        }

        if (Config.bobItemSwitch) {
            long since = System.currentTimeMillis() - itemSwitchStart;
            if (itemSwitchStart > 0 && since < ITEM_SWITCH_ANIM_LEN) {
                float progress = 1.0F - (float) since / ITEM_SWITCH_ANIM_LEN;
                float eye = 1.0F;
                transform.dx += (0.08F * progress) / (distance * tanHalfFov) / aspect * (width * 0.5F) * strength;
                transform.dy += (0.3F * progress) / (distance * tanHalfFov) * (height * 0.5F) * strength;
            }
        }

        if (Config.bobSneak) {
            transform.rotation += -sneakProgress * 70.0F * strength;
        }

        if (Config.bobEat) {
            ItemStack using = player.getItemInUse();
            if (using != null && using.getItemUseAction() == EnumAction.eat) {
                float duration = Math.max(1, player.getItemInUseDuration());
                float progress = 1.0F - (float) player.getItemInUseCount() / duration;
                if (progress < 0.2F) {
                    transform.rotation += (MathHelper.sin(Math.min(1.0F, progress * 5.0F) * 0.5F * (float) Math.PI)) * 135.0F * strength;
                } else if (progress < 0.9F) {
                    float x = (progress - 0.2F) / 0.7F;
                    transform.rotation += (MathHelper.sin(x * 6.0F * (float) Math.PI) * -30.0F + 45.0F) * strength;
                } else {
                    transform.rotation += (MathHelper.sin(Math.min(1.0F, (progress - 0.9F) * 10.0F) * 0.5F * (float) Math.PI) * -135.0F + 45.0F) * strength;
                }
            }
        }

        if (Config.bobBow) {
            float bowProgress = -1.0F;
            ItemStack held = player.getCurrentEquippedItem();
            if (held != null && player.isUsingItem() && held.getItem() instanceof ItemBow) {
                float duration = Math.max(1, player.getItemInUseDuration());
                bowProgress = 1.0F - (float) player.getItemInUseCount() / duration;
            } else if (mod.crend.dynamiccrosshair.compat.TinkersCompat.isWindup(held)) {
                bowProgress = mod.crend.dynamiccrosshair.compat.TinkersCompat.progress(held, player);
                if (bowProgress <= 0.001F) bowProgress = -1.0F;
            }
            if (bowProgress >= 0.0F && bowProgress <= 1.0F) {
                // Crosshair-Selection animates the central gap (uBowOpen), never the size:
                // the arms start spread apart and converge as the weapon charges.
                // Eight discrete stages: each stage reveals one more sprite pixel ring.
                transform.gap = Math.round((1.0F - bowProgress) * 7.0F * strength);
            }
        }

        if (Config.placeFeedback && placeStart > 0L) {
            long now = System.currentTimeMillis();
            long since = now - placeStart;
            if (since < 0L || since > 260L) {
                placeStart = 0L;
            } else {
                float time = since / 260.0F;
                float damping = 1.0F - time;
                float wave = (float) Math.sin(time * Math.PI * 3.0F) * damping;
                transform.rotation += wave * placeAmplitude * strength;
                transform.dx += wave * 3.5F * strength;
                transform.dy += (float) Math.sin(time * Math.PI * 2.0F) * damping * 1.8F * strength;
                float scale = 1.0F + Math.abs(wave) * 0.06F * strength;
                transform.scaleX *= scale;
                transform.scaleY *= scale;
            }
        }

        if (Config.bobAttack && attackStart > 0) {
            long now = System.currentTimeMillis();
            if (crit && now < attackStart + CRIT_ANIM_LEN) {
                float x = (now - attackStart) / (float) CRIT_ANIM_LEN;
                transform.rotation += MathHelper.sin(x * 0.5F * (float) Math.PI) * 360.0F * critDirection * strength;
                transform.scaleX *= 1.0F + MathHelper.sin(x * (float) Math.PI) * (CRIT_SCALE - 1.0F) * strength;
                transform.scaleY *= 1.0F + MathHelper.sin(x * (float) Math.PI) * (CRIT_SCALE - 1.0F) * strength;
            } else if (!crit && now < attackStart + ATTACK_ANIM_LEN) {
                float x = (now - attackStart) / (float) ATTACK_ANIM_LEN;
                float curve = MathHelper.sin(x * (float) Math.PI);
                transform.rotation += curve * attackDamage * attackDegrees * strength;
                transform.scaleX *= 1.0F + curve * attackDamage * (attackScale - 1.0F) * strength;
                transform.scaleY *= 1.0F + curve * attackDamage * (attackScale - 1.0F) * strength;
            }
        }

        transform.rotation = MathHelper.wrapAngleTo180_float(transform.rotation);
        return transform;
    }

    private static Matrix4f viewBobbingMatrix(EntityPlayer player, float partialTicks) {
        float walked = player.distanceWalkedModified - player.prevDistanceWalkedModified;
        float phase = -(player.distanceWalkedModified + walked * partialTicks);
        float cameraYaw = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * partialTicks;
        float cameraPitch = player.prevCameraPitch + (player.cameraPitch - player.prevCameraPitch) * partialTicks;

        float translateX = MathHelper.sin(phase * (float) Math.PI) * cameraYaw * 0.5F;
        float translateY = -Math.abs(MathHelper.cos(phase * (float) Math.PI) * cameraYaw);
        float rotateZ = MathHelper.sin(phase * (float) Math.PI) * cameraYaw * 3.0F;
        float rotateX1 = Math.abs(MathHelper.cos(phase * (float) Math.PI - 0.2F) * cameraYaw) * 5.0F;

        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.translate(new Vector3f(translateX, translateY, 0.0F));
        matrix.rotate((float) Math.toRadians(rotateZ), new Vector3f(0.0F, 0.0F, 1.0F));
        matrix.rotate((float) Math.toRadians(rotateX1), new Vector3f(1.0F, 0.0F, 0.0F));
        matrix.rotate((float) Math.toRadians(cameraPitch), new Vector3f(1.0F, 0.0F, 0.0F));
        return matrix;
    }

    private static float centerDepth(Minecraft mc, EntityPlayer player, float partialTicks) {
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            double dx = hit.hitVec.xCoord - player.posX;
            double dy = hit.hitVec.yCoord - (player.posY + player.getEyeHeight());
            double dz = hit.hitVec.zCoord - player.posZ;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            return MathHelper.clamp_float(distance, 1.0F, 8.0F);
        }
        float reach = mc.playerController == null ? 4.5F : mc.playerController.getBlockReachDistance();
        return MathHelper.clamp_float(reach, 1.0F, 8.0F);
    }

    private static float displayAspect(Minecraft mc) {
        if (mc.displayHeight <= 0) return 1.0F;
        return (float) mc.displayWidth / (float) mc.displayHeight;
    }

    public static class Transform {
        public float dx;
        public float dy;
        public float rotation;
        public float scaleX = 1.0F;
        public float scaleY = 1.0F;
        /** Extra central gap in GUI pixels while a bow/crossbow is being drawn. */
        public float gap;
    }
}
