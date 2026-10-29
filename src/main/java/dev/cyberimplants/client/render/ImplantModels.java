package dev.cyberimplants.client.render;

import dev.cyberimplants.CyberImplants;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Geometry of the implants drawn on players. Units are model pixels in the space of the limb they sit on; arm parts are
 * built for an arm centred on x = 0 (the renderer shifts them onto the real arm) with the outer side at -x for the
 * right arm, mirrored for the left. Every part has a metal half ({@link #TEXTURE}) and a glowing half ({@code _glow},
 * drawn full-bright and tinted with the implant category's colour).
 *
 * <p>Texture zones: (0,0) gunmetal plates, (32,0) blade steel, (0,32) black carbon, (32,32) brass actuators.
 */
public final class ImplantModels {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(CyberImplants.id("implants"), "main");
    public static final ResourceLocation TEXTURE = CyberImplants.id("textures/entity/implants.png");
    public static final ResourceLocation GLOW = CyberImplants.id("textures/entity/implants_glow.png");

    private static ModelPart root;

    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, ImplantModels::create);
    }

    public static void bake(EntityModelSet models) {
        root = models.bakeLayer(LAYER);
    }

    /** A baked part, or null before the first resource load. */
    public static ModelPart part(String name) {
        return root == null ? null : root.getChild(name);
    }

    private static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (boolean left : new boolean[]{false, true}) {
            String side = left ? "_left" : "_right";
            Arm a = new Arm(left);

            // Cybernetic Arm frame: shoulder pauldron, forearm plating, brass elbow and wrist rings.
            a.metal(0, 0, -2, -2, -2, 4, 4, 4, 0.5f)
                    .metal(0, 16, -2, 5, -2, 4, 4.5f, 4, 0.35f)
                    .metal(32, 32, -2, 3.6f, -2, 4, 1, 4, 0.5f)
                    .metal(32, 40, -2, 9.4f, -2, 4, 0.6f, 4, 0.45f);
            a.glow(-2.75f, -1, -1, 0.3f, 2, 2)
                    .glow(-2.6f, 5.5f, -0.5f, 0.3f, 3.5f, 1);
            a.add(root, "frame" + side);

            // Retractable Blade: the forearm housing is always there; the blade itself only while deployed.
            a.metal(0, 32, -3.3f, 4.5f, -1.5f, 1.1f, 5, 3, 0)
                    .glow(-3.45f, 5, -0.4f, 0.2f, 3.5f, 0.8f);
            a.add(root, "blade_housing" + side);
            a.metal(32, 0, -3.05f, 7, -1.25f, 0.5f, 9, 2.5f, 0)
                    .metal(40, 0, -3.05f, 16, -1.0f, 0.5f, 3, 1.8f, 0)
                    .metal(48, 0, -3.05f, 19, -0.7f, 0.5f, 2, 1.0f, 0)
                    .metal(52, 8, -3.05f, 21, -0.45f, 0.5f, 1, 0.5f, 0)
                    .glow(-3.1f, 8, -1.45f, 0.6f, 11, 0.3f);
            a.add(root, "blade" + side);

            // Ballistic Module: launcher housing along the outer forearm, a barrel past the wrist and a drum.
            a.metal(0, 16, -3.6f, 3.5f, -2, 1.7f, 5.5f, 4, 0)
                    .metal(0, 40, -3.3f, 8.5f, -0.9f, 1.2f, 4.5f, 1.8f, 0)
                    .metal(32, 48, -3.5f, 12.6f, -1.1f, 1.6f, 1, 2.2f, 0)
                    .metal(48, 32, -3.4f, 4.5f, 2, 1.4f, 3, 1.6f, 0)
                    .glow(-3.75f, 4.5f, -1.4f, 0.2f, 3, 0.5f)
                    .glow(-3.1f, 13.5f, -0.5f, 0.8f, 0.3f, 1);
            a.add(root, "ballistic" + side);

            // Remote Manipulator: a ring of three claw prongs around the wrist.
            a.metal(32, 32, -2.4f, 8.8f, -2.4f, 4.8f, 0.8f, 4.8f, 0)
                    .metal(32, 48, -1.9f, 10, -2.6f, 0.8f, 2.2f, 0.8f, 0)
                    .metal(32, 48, 1.1f, 10, -2.6f, 0.8f, 2.2f, 0.8f, 0)
                    .metal(32, 48, -0.4f, 10, 1.8f, 0.8f, 2.2f, 0.8f, 0)
                    .glow(-0.5f, 9.0f, -2.55f, 1, 0.4f, 0.2f);
            a.add(root, "manipulator" + side);

            // Mobility: knee cap, calf piston, glowing piston rod. Legs share the arm convention (outer at -x).
            a.metal(0, 0, -1.5f, 3.5f, -2.7f, 3, 2.2f, 0.7f, 0)
                    .metal(32, 32, -1, 5, 2, 2, 5, 1, 0)
                    .glow(-0.3f, 6, 3, 0.6f, 3, 0.2f);
            a.add(root, "legs" + side);
        }

        // Head (head pivot at the neck; the head cube spans -4..4, -8..0, -4..4).
        Arm head = new Arm(false);
        head.metal(0, 0, -4.7f, -5.5f, -3, 0.7f, 2, 2.5f, 0)
                .glow(-3, -4, -4.3f, 2, 1, 0.3f)
                .glow(1, -4, -4.3f, 2, 1, 0.3f)
                .glow(-4.8f, -5, -2.2f, 0.1f, 1, 1);
        head.add(root, "optics");
        head.metal(0, 0, -1.5f, -4.5f, 3.9f, 3, 2.5f, 0.6f, 0)
                .glow(-1, -3.8f, 4.45f, 0.7f, 0.7f, 0.2f)
                .glow(0.3f, -3.8f, 4.45f, 0.7f, 0.7f, 0.2f);
        head.add(root, "neural");

        // Body (pivot at the neck; the body cube spans -4..4, 0..12, -2..2).
        Arm body = new Arm(false);
        body.metal(0, 32, -1, 0.5f, 2, 2, 11, 1, 0)
                .glow(-0.5f, 2, 2.9f, 1, 1, 0.3f)
                .glow(-0.5f, 5, 2.9f, 1, 1, 0.3f)
                .glow(-0.5f, 8, 2.9f, 1, 1, 0.3f);
        body.add(root, "deck");
        body.metal(32, 32, -1.2f, 3, -2.45f, 2.4f, 2.4f, 0.5f, 0)
                .glow(-0.6f, 3.6f, -2.6f, 1.2f, 1.2f, 0.2f);
        body.add(root, "core");
        // Subdermal: glowing seams across the collarbone and down the flanks, just over the jacket layer.
        body.glow(-3.5f, 1.2f, -2.4f, 7, 0.3f, 0.1f)
                .glow(-3.6f, 2, -2.4f, 0.3f, 6, 0.1f)
                .glow(3.3f, 2, -2.4f, 0.3f, 6, 0.1f);
        body.add(root, "subdermal");

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Collects one implant's metal and glow boxes, mirrored for the left side. */
    private static final class Arm {
        private final boolean mirror;
        private CubeListBuilder metal = CubeListBuilder.create();
        private CubeListBuilder glow = CubeListBuilder.create();

        Arm(boolean mirror) {
            this.mirror = mirror;
        }

        Arm metal(int u, int v, float x, float y, float z, float w, float h, float d, float grow) {
            metal.texOffs(u, v).addBox(mirror ? -(x + w) : x, y, z, w, h, d, new CubeDeformation(grow));
            return this;
        }

        Arm glow(float x, float y, float z, float w, float h, float d) {
            glow.texOffs(0, 0).addBox(mirror ? -(x + w) : x, y, z, w, h, d);
            return this;
        }

        void add(PartDefinition root, String name) {
            root.addOrReplaceChild(name, metal, PartPose.ZERO);
            root.addOrReplaceChild(name + "_glow", glow, PartPose.ZERO);
            metal = CubeListBuilder.create();
            glow = CubeListBuilder.create();
        }
    }

    private ImplantModels() {
    }
}
