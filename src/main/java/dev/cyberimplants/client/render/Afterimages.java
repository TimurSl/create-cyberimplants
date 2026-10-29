package dev.cyberimplants.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.cyberimplants.client.ClientAppearance;
import dev.cyberimplants.config.ClientConfig;
import dev.cyberimplants.implant.Appearance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sandevistan afterimages: while a player's field runs, every few ticks their current pose is frozen into a ghost that
 * stays where it was and fades out, each one a different colour, so a dash leaves a trail of coloured copies behind.
 * Poses are captured on a private player model, so the trail works for the local player in first person too.
 */
public final class Afterimages {
    private static final int GREEN = 0x39FF88;
    private static final int MAX_PER_PLAYER = 48;
    private static final double MAX_DISTANCE_SQR = 96 * 96;

    private record Ghost(Vec3 pos, float bodyYaw, float scale, boolean crouching, boolean slim, ResourceLocation skin,
                         PartPose[] poses, boolean[] visible, int rgb, long born, boolean local) {
    }

    private static final Map<Integer, Deque<Ghost>> TRAILS = new HashMap<>();
    private static PlayerModel<AbstractClientPlayer> wide;
    private static PlayerModel<AbstractClientPlayer> slim;
    private static int colourStep;

    public static void bake(EntityModelSet models) {
        wide = new PlayerModel<>(models.bakeLayer(ModelLayers.PLAYER), false);
        slim = new PlayerModel<>(models.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    public static void clear() {
        TRAILS.clear();
    }

    private static ModelPart[] parts(PlayerModel<?> m) {
        return new ModelPart[]{m.head, m.hat, m.body, m.jacket, m.rightArm, m.rightSleeve, m.leftArm, m.leftSleeve,
                m.rightLeg, m.rightPants, m.leftLeg, m.leftPants};
    }

    private static final PlayerModelPart[] LAYERS = {null, PlayerModelPart.HAT, null, PlayerModelPart.JACKET, null,
            PlayerModelPart.RIGHT_SLEEVE, null, PlayerModelPart.LEFT_SLEEVE, null, PlayerModelPart.RIGHT_PANTS_LEG, null,
            PlayerModelPart.LEFT_PANTS_LEG};

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            TRAILS.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        long now = mc.level.getGameTime();
        int lifetime = ClientConfig.get(ClientConfig.AFTERIMAGE_LIFETIME);
        for (Iterator<Deque<Ghost>> it = TRAILS.values().iterator(); it.hasNext(); ) {
            Deque<Ghost> trail = it.next();
            while (!trail.isEmpty() && now - trail.peekFirst().born > lifetime) {
                trail.pollFirst();
            }
            if (trail.isEmpty()) {
                it.remove();
            }
        }
        if (!ClientConfig.get(ClientConfig.AFTERIMAGES) || wide == null) {
            TRAILS.clear();
            return;
        }
        int spacing = ClientConfig.get(ClientConfig.AFTERIMAGE_SPACING);
        for (AbstractClientPlayer player : mc.level.players()) {
            if (!ClientAppearance.has(player, Appearance.SANDEVISTAN) || player.isInvisible() || player.isSpectator()
                    || player.distanceToSqr(mc.player) > MAX_DISTANCE_SQR) {
                continue;
            }
            Deque<Ghost> trail = TRAILS.computeIfAbsent(player.getId(), id -> new ArrayDeque<>());
            Ghost last = trail.peekLast();
            if (last != null && (now - last.born < spacing
                    || last.pos.distanceToSqr(player.position()) < 0.04 && !player.swinging)) {
                continue; // standing still leaves no trail; swinging does
            }
            trail.addLast(capture(player, now, player == mc.player));
            while (trail.size() > MAX_PER_PLAYER) {
                trail.pollFirst();
            }
        }
    }

    private static Ghost capture(AbstractClientPlayer player, long now, boolean local) {
        boolean isSlim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        PlayerModel<AbstractClientPlayer> model = isSlim ? slim : wide;

        // Pose the model the way LivingEntityRenderer and PlayerRenderer would, at the end of this tick.
        float speed = Math.min(player.walkAnimation.speed(1.0f), 1.0f);
        float position = player.walkAnimation.position(1.0f);
        float headYaw = Mth.wrapDegrees(player.yHeadRot - player.yBodyRot);
        model.attackTime = player.getAttackAnim(1.0f);
        model.riding = player.isPassenger();
        model.young = player.isBaby();
        model.crouching = player.isCrouching();
        HumanoidModel.ArmPose main = player.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        HumanoidModel.ArmPose off = player.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
        model.rightArmPose = rightHanded ? main : off;
        model.leftArmPose = rightHanded ? off : main;
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            model.attackTime = 0;
        }
        model.prepareMobModel(player, position, speed, 1.0f);
        model.setupAnim(player, position, speed, player.tickCount, headYaw, player.getXRot());

        ModelPart[] parts = parts(model);
        PartPose[] poses = new PartPose[parts.length];
        boolean[] visible = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) {
            poses[i] = parts[i].storePose();
            visible[i] = LAYERS[i] == null || player.isModelPartShown(LAYERS[i]);
        }
        int rgb = GREEN;
        if (ClientConfig.get(ClientConfig.AFTERIMAGE_RAINBOW)) {
            rgb = Mth.hsvToRgb((colourStep++ * 0.07f) % 1.0f, 0.8f, 1.0f);
        }
        return new Ghost(player.position(), player.yBodyRot, player.getScale(), player.isCrouching(), isSlim,
                player.getSkin().texture(), poses, visible, rgb, now, local);
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || TRAILS.isEmpty() || wide == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = mc.level.getGameTime() + partial;
        int lifetime = ClientConfig.get(ClientConfig.AFTERIMAGE_LIFETIME);
        float opacity = ClientConfig.get(ClientConfig.AFTERIMAGE_OPACITY).floatValue();
        Vec3 cam = event.getCamera().getPosition();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Set<RenderType> used = new HashSet<>();
        List<Ghost> all = new ArrayList<>();
        TRAILS.values().forEach(all::addAll);
        for (Ghost ghost : all) {
            float age = (float) ((now - ghost.born) / lifetime);
            if (age >= 1 || age < 0) {
                continue;
            }
            // Don't fill the screen with your own ghost when backing up in first person.
            if (ghost.local && firstPerson && ghost.pos.add(0, 1.4, 0).distanceToSqr(cam) < 1.6 * 1.6) {
                continue;
            }
            float fade = 1 - age;
            PlayerModel<AbstractClientPlayer> model = ghost.slim ? slim : wide;
            ModelPart[] parts = parts(model);
            for (int i = 0; i < parts.length; i++) {
                parts[i].loadPose(ghost.poses[i]);
                parts[i].visible = ghost.visible[i];
            }
            pose.pushPose();
            pose.translate(ghost.pos.x - cam.x, ghost.pos.y - cam.y + (ghost.crouching ? -0.125 * ghost.scale : 0), ghost.pos.z - cam.z);
            pose.mulPose(Axis.YP.rotationDegrees(180.0f - ghost.bodyYaw));
            pose.scale(-1.0f, -1.0f, 1.0f);
            float s = 0.9375f * ghost.scale;
            pose.scale(s, s, s);
            pose.translate(0.0f, -1.501f, 0.0f);

            // The skin, tinted towards the ghost's colour and see-through...
            int tint = mixWhite(ghost.rgb, 0.35f);
            int alpha = Mth.clamp((int) (opacity * fade * 255), 0, 255);
            RenderType skin = RenderType.entityTranslucent(ghost.skin);
            used.add(skin);
            model.renderToBuffer(pose, buffers.getBuffer(skin), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, alpha << 24 | tint);
            // ...and an additive neon silhouette over it (base parts only, the outer layers would look blocky).
            for (int i = 0; i < parts.length; i++) {
                parts[i].visible = LAYERS[i] == null;
            }
            RenderType neon = RenderType.eyes(ImplantModels.GLOW);
            used.add(neon);
            model.renderToBuffer(pose, buffers.getBuffer(neon), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    0xFF000000 | scale(ghost.rgb, 0.55f * fade * opacity));
            pose.popPose();
        }
        for (ModelPart part : parts(wide)) {
            part.visible = true;
        }
        for (ModelPart part : parts(slim)) {
            part.visible = true;
        }
        for (RenderType type : used) {
            buffers.endBatch(type);
        }
    }

    private static int mixWhite(int rgb, float white) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = (int) (r + (255 - r) * white);
        g = (int) (g + (255 - g) * white);
        b = (int) (b + (255 - b) * white);
        return r << 16 | g << 8 | b;
    }

    private static int scale(int rgb, float k) {
        int r = (int) (((rgb >> 16) & 0xFF) * k);
        int g = (int) (((rgb >> 8) & 0xFF) * k);
        int b = (int) ((rgb & 0xFF) * k);
        return Mth.clamp(r, 0, 255) << 16 | Mth.clamp(g, 0, 255) << 8 | Mth.clamp(b, 0, 255);
    }

    private Afterimages() {
    }
}
