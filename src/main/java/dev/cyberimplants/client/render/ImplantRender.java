package dev.cyberimplants.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cyberimplants.client.ClientAppearance;
import dev.cyberimplants.client.CyberUi;
import dev.cyberimplants.config.ClientConfig;
import dev.cyberimplants.implant.Appearance;
import dev.cyberimplants.implant.Arms;
import dev.cyberimplants.implant.ImplantCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * Draws installed implants on players from their {@link Appearance} bits: plating on cybernetic arms, the Retractable
 * Blade (out while deployed and that hand is empty), the Ballistic Module's launcher, manipulator claws, glowing
 * optics, neural ports, a deck along the spine, the Core on the chest and leg actuators. Third person through
 * {@link Layer}; the arm in first person through {@link #onRenderArm}.
 */
public final class ImplantRender {
    private static final int METAL_TINT = 0xFFFFFFFF;

    /** Added to both player renderers (wide and slim arms). */
    public static final class Layer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        public Layer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!shown(player)) {
                return;
            }
            int bits = ClientAppearance.of(player);
            if (bits == 0) {
                return;
            }
            PlayerModel<AbstractClientPlayer> model = getParentModel();
            boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
            for (Arms.Side side : Arms.Side.values()) {
                ModelPart arm = side == Arms.Side.LEFT ? model.leftArm : model.rightArm;
                arm(pose, buffers, light, player, bits, side, arm, slim);
            }
            part(pose, buffers, light, model.head, "optics", bits, Appearance.OPTICS, ImplantCategory.OPTICAL);
            part(pose, buffers, light, model.head, "neural", bits, Appearance.NEURAL, ImplantCategory.NEURAL);
            part(pose, buffers, light, model.body, "deck", bits, Appearance.DECK, ImplantCategory.CYBERDECK);
            part(pose, buffers, light, model.body, "core", bits, Appearance.CORE, ImplantCategory.CORE);
            part(pose, buffers, light, model.body, "subdermal", bits, Appearance.SUBDERMAL, ImplantCategory.SUBDERMAL);
            part(pose, buffers, light, model.rightLeg, "legs_right", bits, Appearance.MOBILITY, ImplantCategory.MOBILITY);
            part(pose, buffers, light, model.leftLeg, "legs_left", bits, Appearance.MOBILITY, ImplantCategory.MOBILITY);
        }
    }

    /** Whether this player's implants are drawn at all (config, invisibility, camouflage). */
    private static boolean shown(AbstractClientPlayer player) {
        if (!ClientConfig.get(ClientConfig.IMPLANT_MODELS) || player.isInvisible()) {
            return false;
        }
        return player == Minecraft.getInstance().player || ClientConfig.get(ClientConfig.OTHER_PLAYERS_IMPLANTS);
    }

    /** First person: draw the vanilla arm ourselves, then the implants on it, using the arm's posed part. */
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        if (!ClientConfig.get(ClientConfig.FIRST_PERSON_IMPLANTS) || !ClientConfig.get(ClientConfig.IMPLANT_MODELS)) {
            return;
        }
        Arms.Side side = event.getArm() == HumanoidArm.LEFT ? Arms.Side.LEFT : Arms.Side.RIGHT;
        int bits = ClientAppearance.of(player);
        if ((bits & armBits(side)) == 0
                || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        if (side == Arms.Side.RIGHT) {
            renderer.renderRightHand(pose, buffers, light, player);
        } else {
            renderer.renderLeftHand(pose, buffers, light, player);
        }
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        arm(pose, buffers, light, player, bits, side, side == Arms.Side.LEFT ? model.leftArm : model.rightArm,
                player.getSkin().model() == PlayerSkin.Model.SLIM);
    }

    private static int armBits(Arms.Side side) {
        return side == Arms.Side.LEFT
                ? Appearance.FRAME_LEFT | Appearance.BLADE_LEFT | Appearance.BALLISTIC_LEFT | Appearance.MANIPULATOR_LEFT
                : Appearance.FRAME_RIGHT | Appearance.BLADE_RIGHT | Appearance.BALLISTIC_RIGHT | Appearance.MANIPULATOR_RIGHT;
    }

    private static void arm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, int bits,
                            Arms.Side side, ModelPart arm, boolean slim) {
        boolean left = side == Arms.Side.LEFT;
        if ((bits & armBits(side)) == 0) {
            return;
        }
        String suffix = left ? "_left" : "_right";
        boolean handEmpty = player.getItemInHand(Arms.handOf(player, side)).isEmpty();
        boolean bladeOut = (bits & (left ? Appearance.BLADE_LEFT : Appearance.BLADE_RIGHT)) != 0
                && (bits & Appearance.BLADE_DEPLOYED) != 0 && handEmpty;

        pose.pushPose();
        arm.translateAndRotate(pose);
        // The arm cube is off-centre from its pivot: wide arms span -3..1 (right) or -1..3 (left), slim arms one pixel less.
        float centre = slim ? 0.5f : 1.0f;
        pose.translate((left ? centre : -centre) / 16.0f, 0, 0);
        if (slim) {
            pose.scale(0.78f, 1, 1);
        }
        int limbs = CyberUi.categoryColor(ImplantCategory.LIMBS);
        draw(pose, buffers, light, "frame" + suffix, bits, left ? Appearance.FRAME_LEFT : Appearance.FRAME_RIGHT, limbs);
        draw(pose, buffers, light, "blade_housing" + suffix, bits, left ? Appearance.BLADE_LEFT : Appearance.BLADE_RIGHT, limbs);
        if (bladeOut) {
            draw(pose, buffers, light, "blade" + suffix, bits, left ? Appearance.BLADE_LEFT : Appearance.BLADE_RIGHT, 0xFFFF2BD6);
        }
        draw(pose, buffers, light, "ballistic" + suffix, bits, left ? Appearance.BALLISTIC_LEFT : Appearance.BALLISTIC_RIGHT, 0xFFFFB830);
        draw(pose, buffers, light, "manipulator" + suffix, bits, left ? Appearance.MANIPULATOR_LEFT : Appearance.MANIPULATOR_RIGHT,
                CyberUi.CYAN);
        pose.popPose();
    }

    private static void part(PoseStack pose, MultiBufferSource buffers, int light, ModelPart limb, String name, int bits, int flag,
                             ImplantCategory category) {
        if ((bits & flag) == 0) {
            return;
        }
        pose.pushPose();
        limb.translateAndRotate(pose);
        draw(pose, buffers, light, name, bits, flag, CyberUi.categoryColor(category));
        pose.popPose();
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, int light, String name, int bits, int flag, int glowColor) {
        if ((bits & flag) == 0) {
            return;
        }
        ModelPart metal = ImplantModels.part(name);
        ModelPart glow = ImplantModels.part(name + "_glow");
        if (metal == null || glow == null) {
            return;
        }
        VertexConsumer solid = buffers.getBuffer(RenderType.entityCutoutNoCull(ImplantModels.TEXTURE));
        metal.render(pose, solid, light, OverlayTexture.NO_OVERLAY, METAL_TINT);
        VertexConsumer lit = buffers.getBuffer(RenderType.eyes(ImplantModels.GLOW));
        glow.render(pose, lit, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFF000000 | glowColor);
    }

    private ImplantRender() {
    }
}
