package dev.cyberimplants.client;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.implant.Arms;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Tags;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.implant.mobility.HydraulicLegs;
import dev.cyberimplants.network.MovementAbilityPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Predicate;

/**
 * Client-authoritative movement abilities (docs/design/08-technical-architecture.md, section 8.6): the client detects
 * the input and applies the velocity at once, then tells the server, which validates and bills the Charge.
 */
final class ClientMovement {
    private static boolean wasJumping;
    private static int airJumpsUsed;
    private static int lastDodgeTick = -1000;
    private static int leapCharge;
    private static int grip = -1;           // remaining grip ticks, -1 = not initialised
    private static boolean clinging;
    private static boolean soleAttached;
    private static int soleReport;
    private static boolean wasOnGround;
    private static int landedTick = -1000;
    private static boolean wasSneaking;
    private static boolean slamming;
    private static double slamStartY;
    private static int reboundUntil = -1000;
    private static boolean wasSprintDown;
    private static boolean redirectUsed;

    /** Elastic Recoil: a jump this many ticks after landing is higher. */
    private static final int ELASTIC_WINDOW_TICKS = 3;
    /** +30% jump height is sqrt(1.3) of the take-off speed. */
    private static final double ELASTIC_VELOCITY = Math.sqrt(1.3);
    /** Rebound: the jump must follow the slam's landing within 0.5 s. */
    private static final int REBOUND_WINDOW_TICKS = 10;

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.isPaused() || mc.screen != null || p.isSpectator() || ClientRig.restrained()) {
            wasJumping = false;
            return;
        }
        BodyState body = ClientBodyState.get();
        if (body.installed().isEmpty()) {
            reset();
            return;
        }
        Input input = p.input;
        boolean grounded = p.onGround() || p.onClimbable() || p.isInWater() || p.isInLava();
        if (grounded) {
            airJumpsUsed = 0;
            redirectUsed = false;
        }
        if (p.onGround() && !wasOnGround) {
            landedTick = p.tickCount;
        }

        dodge(mc, p, body);
        airJump(p, body, input, grounded);
        hydraulicLegs(mc, p, body);
        microgrip(p, body, input);
        magneticSoles(p, body, input);
        groundSlam(p, body, input, grounded);
        airControl(p, body, input, grounded);
        redirect(mc, p, body, grounded);
        wasJumping = input.jumping;
        wasSneaking = input.shiftKeyDown;
        wasOnGround = p.onGround();
    }

    private static void reset() {
        airJumpsUsed = 0;
        leapCharge = 0;
        clinging = false;
        soleAttached = false;
        wasJumping = false;
        slamming = false;
        redirectUsed = false;
    }

    // ---------------------------------------------------------------- Kerenzikov

    private static void dodge(Minecraft mc, LocalPlayer p, BodyState body) {
        boolean pressed = false;
        while (ModKeys.DODGE.consumeClick()) {
            pressed = true;
        }
        int tier = body.tierOf(ImplantIds.KERENZIKOV);
        if (!pressed || tier == 0 || p.isPassenger() || p.isFallFlying() || p.isInWater() || p.isSwimming()) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.KERENZIKOV, tier);
        if (body.cooldownRemaining(ImplantIds.KERENZIKOV) > 0 || p.tickCount - lastDodgeTick < stats.cooldownTicks()
                || !affordable(body, stats.chargeCost())) {
            return;
        }
        double f = p.input.forwardImpulse;
        double l = p.input.leftImpulse;
        if (f == 0 && l == 0) {
            f = -1; // no input: dodge backwards
        }
        double yaw = Math.toRadians(p.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 dir = forward.scale(f).add(left.scale(l)).normalize();
        double distance = stats.value("distance", 2.0);
        double speed = distance * (p.onGround() ? 0.45 : 0.12);
        Vec3 motion = p.getDeltaMovement();
        p.setDeltaMovement(dir.x * speed, p.onGround() ? Math.max(motion.y, 0.1) : motion.y, dir.z * speed);
        lastDodgeTick = p.tickCount;
        PacketDistributor.sendToServer(new MovementAbilityPayload(MovementAbilityPayload.Kind.DODGE, (float) dir.x, (float) dir.z));
    }

    // ---------------------------------------------------------------- Enhanced Tendons

    private static void airJump(LocalPlayer p, BodyState body, Input input, boolean grounded) {
        int tier = body.tierOf(ImplantIds.ENHANCED_TENDONS);
        if (tier == 0 || grounded || !input.jumping || wasJumping || p.isPassenger() || p.isFallFlying()
                || p.getAbilities().flying) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.ENHANCED_TENDONS, tier);
        if (airJumpsUsed >= (int) stats.value("air_jumps", 1) || !affordable(body, stats.chargeCost())) {
            return;
        }
        airJumpsUsed++;
        double power = p.getAttributeValue(Attributes.JUMP_STRENGTH) * stats.value("strength", 0.7);
        Vec3 motion = p.getDeltaMovement();
        p.setDeltaMovement(motion.x, power, motion.z);
        PacketDistributor.sendToServer(new MovementAbilityPayload(MovementAbilityPayload.Kind.AIR_JUMP, 0, 0));
    }

    // ---------------------------------------------------------------- Hydraulic Legs

    /** True while the user is building leap pressure; MovementInput uses it to swallow the jump and slow walking. */
    private static boolean chargingEligible(LocalPlayer p, BodyState body, boolean sneaking) {
        return body.has(ImplantIds.HYDRAULIC_LEGS) && sneaking && p.onGround()
                && body.cooldownRemaining(ImplantIds.HYDRAULIC_LEGS) == 0
                && affordable(body, Stats.of(ImplantIds.HYDRAULIC_LEGS, body.tierOf(ImplantIds.HYDRAULIC_LEGS)).chargeCost());
    }

    private static void hydraulicLegs(Minecraft mc, LocalPlayer p, BodyState body) {
        int tier = body.tierOf(ImplantIds.HYDRAULIC_LEGS);
        if (tier == 0) {
            leapCharge = 0;
            return;
        }
        TierStats stats = Stats.of(ImplantIds.HYDRAULIC_LEGS, tier);
        int fullTicks = Math.max(1, (int) stats.value("full_charge_ticks", 30));
        boolean holding = mc.options.keyJump.isDown() && p.input.shiftKeyDown && chargingEligible(p, body, true);
        if (holding) {
            if (leapCharge < fullTicks) {
                leapCharge++;
            }
            if (leapCharge % 4 == 0) {
                p.displayClientMessage(Component.translatable("cyberimplants.hydraulics.pressure",
                        Math.round(100.0 * leapCharge / fullTicks)), true);
            }
            return;
        }
        if (leapCharge >= 6) {
            leap(p, stats, Math.min(1.0f, (float) leapCharge / fullTicks));
        }
        leapCharge = 0;
    }

    private static void leap(LocalPlayer p, TierStats stats, float fraction) {
        double vy = HydraulicLegs.launchSpeedForHeight(stats.value("max_vertical", 4) * fraction);
        double flightTicks = Math.max(1.0, 2.0 * vy / 0.08);
        double horizontal = stats.value("max_horizontal", 6) * fraction / flightTicks;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
        p.setDeltaMovement(flat.x * horizontal, vy, flat.z * horizontal);
        PacketDistributor.sendToServer(new MovementAbilityPayload(MovementAbilityPayload.Kind.LEAP, fraction, 0));
    }

    // ---------------------------------------------------------------- Microgrip

    private static void microgrip(LocalPlayer p, BodyState body, Input input) {
        Arms.Side side = Arms.freeHandWith(p, ImplantIds.MICROGRIP);
        int tier = side == null ? 0 : Arms.moduleTier(body, side, ImplantIds.MICROGRIP);
        TierStats stats = Stats.of(ImplantIds.MICROGRIP, Math.max(tier, 1));
        int maxGrip = (int) (stats.value("stamina_ticks", 60));
        if (grip < 0 || grip > maxGrip) {
            grip = maxGrip;
        }
        if (p.onGround()) {
            grip = Math.min(maxGrip, grip + 2); // stamina refills twice as fast on the ground
        }
        boolean want = tier > 0 && input.shiftKeyDown && !p.onGround() && !p.isInWater() && grip > 0
                && gripSurface(p, st -> st.is(Tags.UNGRIPPABLE) || (tier < 3 && st.is(Tags.UNGRIPPABLE_SMOOTH)), true);
        if (!want) {
            clinging = tier > 0 && ceilingHang(p, stats, tier, input);
            return;
        }
        clinging = true;
        grip--;
        double climb = stats.value("climb_speed", 2.0) / 20.0;
        double y = input.forwardImpulse > 0 ? climb : input.forwardImpulse < 0 ? -climb : 0.0;
        p.setDeltaMovement(0, y, 0);
        p.fallDistance = 0;
        if (grip == 0) {
            p.displayClientMessage(Component.translatable("cyberimplants.microgrip.exhausted"), true);
        }
    }

    /**
     * Tier V Ceiling hang (Layer 5): Sneak under a grippable block to cling to its underside and crawl along it at
     * half the climb speed. Stamina drains x1.5 (alternately 1 and 2 ticks of grip per tick).
     */
    private static boolean ceilingHang(LocalPlayer p, TierStats stats, int tier, Input input) {
        if (!Stats.unlocked(ImplantIds.MICROGRIP, tier, "ceiling_hang") || !input.shiftKeyDown || p.onGround()
                || p.isInWater() || grip <= 0) {
            return false;
        }
        Level level = p.level();
        BlockPos above = BlockPos.containing(p.getX(), p.getBoundingBox().maxY + 0.2, p.getZ());
        BlockState state = level.getBlockState(above);
        if (state.isAir() || state.getCollisionShape(level, above).isEmpty() || state.is(Tags.UNGRIPPABLE)) {
            return false;
        }
        grip = Math.max(0, grip - (p.tickCount % 2 == 0 ? 2 : 1));
        double crawl = stats.value("climb_speed", 2.0) / 40.0;
        double yaw = Math.toRadians(p.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 move = forward.scale(input.forwardImpulse).add(left.scale(input.leftImpulse));
        move = move.lengthSqr() < 1.0E-4 ? Vec3.ZERO : move.normalize().scale(crawl);
        // A little lift cancels the next tick's gravity; the ceiling stops the rest.
        p.setDeltaMovement(move.x, 0.08, move.z);
        p.fallDistance = 0;
        if (grip == 0) {
            p.displayClientMessage(Component.translatable("cyberimplants.microgrip.exhausted"), true);
        }
        return true;
    }

    // ---------------------------------------------------------------- Magnetic Soles

    private static void magneticSoles(LocalPlayer p, BodyState body, Input input) {
        boolean on = Toggles.isOn(p, ImplantIds.MAGNETIC_SOLES) && body.charge() > 0;
        boolean attached = on && !p.onGround() && !p.isInWater() && gripSurface(p, st -> st.is(Tags.MAGNETIC), false);
        if (attached || (on && p.onGround() && p.horizontalCollision && gripSurface(p, st -> st.is(Tags.MAGNETIC), false)
                && input.forwardImpulse > 0)) {
            attached = true;
            TierStats stats = Stats.of(ImplantIds.MAGNETIC_SOLES, Math.max(1, body.tierOf(ImplantIds.MAGNETIC_SOLES)));
            double walk = p.getAttributeValue(Attributes.MOVEMENT_SPEED) * stats.value("wall_walk_speed", 0.5);
            double y = input.forwardImpulse > 0 ? walk : input.forwardImpulse < 0 ? -walk : 0.0;
            Vec3 motion = p.getDeltaMovement();
            p.setDeltaMovement(motion.x * 0.3, y, motion.z * 0.3);
            p.fallDistance = 0;
        }
        if (attached) {
            if (!soleAttached || ++soleReport >= 5) {
                soleReport = 0;
                PacketDistributor.sendToServer(new MovementAbilityPayload(MovementAbilityPayload.Kind.SOLES_ATTACHED, 0, 0));
            }
        } else if (soleAttached) {
            PacketDistributor.sendToServer(new MovementAbilityPayload(MovementAbilityPayload.Kind.SOLES_DETACHED, 0, 0));
        }
        soleAttached = attached;
    }

    /**
     * Whether a wall the player is pressed against qualifies: any solid block next to the body that is not in
     * matched by {@code tagged} (when {@code excluded} is false) or not matched (when it is true).
     */
    private static boolean gripSurface(LocalPlayer p, Predicate<BlockState> tagged, boolean excluded) {
        Level level = p.level();
        double reach = p.getBbWidth() / 2.0 + 0.35;
        for (double dy : new double[]{0.4, 1.2}) {
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                BlockPos pos = BlockPos.containing(p.getX() + d[0] * reach, p.getY() + dy, p.getZ() + d[1] * reach);
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.getCollisionShape(level, pos).isEmpty()) {
                    continue;
                }
                if (excluded != tagged.test(state)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- Hydraulic Legs: ground slam and Rebound

    /**
     * Tier III Ground slam: pressing Sneak mid-air (with at least two blocks of air below) dives down; the server
     * deals the landing damage. Tier V Rebound: a jump within 0.5 s of the landing is a free full-charge leap.
     */
    private static void groundSlam(LocalPlayer p, BodyState body, Input input, boolean grounded) {
        int tier = body.tierOf(ImplantIds.HYDRAULIC_LEGS);
        if (tier == 0) {
            slamming = false;
            return;
        }
        if (slamming) {
            if (grounded) {
                slamming = false;
                float fall = (float) Math.max(0.0, slamStartY - p.getY());
                PacketDistributor.sendToServer(new MovementAbilityPayload(MovementAbilityPayload.Kind.SLAM_LAND, fall, 0));
                if (Stats.unlocked(ImplantIds.HYDRAULIC_LEGS, tier, "rebound")) {
                    reboundUntil = p.tickCount + REBOUND_WINDOW_TICKS;
                }
            } else {
                Vec3 m = p.getDeltaMovement();
                p.setDeltaMovement(m.x * 0.6, Math.min(m.y, -1.5), m.z * 0.6);
            }
            return;
        }
        if (p.onGround() && input.jumping && !wasJumping && !input.shiftKeyDown && p.tickCount <= reboundUntil) {
            reboundUntil = -1000;
            reboundLeap(p, Stats.of(ImplantIds.HYDRAULIC_LEGS, tier));
            return;
        }
        if (grounded || !input.shiftKeyDown || wasSneaking || clinging || soleAttached || p.isFallFlying()
                || p.getAbilities().flying || p.isPassenger() || !Stats.unlocked(ImplantIds.HYDRAULIC_LEGS, tier, "ground_slam")
                || !airBelow(p, 2)) {
            return;
        }
        slamming = true;
        slamStartY = p.getY();
        Vec3 m = p.getDeltaMovement();
        p.setDeltaMovement(m.x * 0.3, -1.5, m.z * 0.3);
        PacketDistributor.sendToServer(new MovementAbilityPayload(MovementAbilityPayload.Kind.SLAM_START, 0, 0));
    }

    private static void reboundLeap(LocalPlayer p, TierStats stats) {
        double vy = HydraulicLegs.launchSpeedForHeight(stats.value("max_vertical", 4));
        double flightTicks = Math.max(1.0, 2.0 * vy / 0.08);
        double horizontal = stats.value("max_horizontal", 6) / flightTicks;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
        p.setDeltaMovement(flat.x * horizontal, vy, flat.z * horizontal);
        PacketDistributor.sendToServer(new MovementAbilityPayload(MovementAbilityPayload.Kind.LEAP, 1.0f, 1.0f));
    }

    /** Whether the {@code blocks} blocks under the player's feet have no collision. */
    private static boolean airBelow(LocalPlayer p, int blocks) {
        Level level = p.level();
        for (int i = 1; i <= blocks; i++) {
            BlockPos pos = BlockPos.containing(p.getX(), p.getY() - i, p.getZ());
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- Inertial Stabilizer

    private static void airControl(LocalPlayer p, BodyState body, Input input, boolean grounded) {
        int tier = body.tierOf(ImplantIds.INERTIAL_STABILIZER);
        if (tier == 0 || grounded || p.isFallFlying() || p.getAbilities().flying || clinging || soleAttached) {
            return;
        }
        double f = input.forwardImpulse;
        double l = input.leftImpulse;
        if (f == 0 && l == 0) {
            return;
        }
        double extra = 0.02 * Stats.of(ImplantIds.INERTIAL_STABILIZER, tier).value("air_control", 0.25);
        double yaw = Math.toRadians(p.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 dir = forward.scale(f).add(left.scale(l)).normalize().scale(extra);
        p.setDeltaMovement(p.getDeltaMovement().add(dir));
    }

    /**
     * Tier V Redirect (Layer 5): tapping Sprint mid-air turns the current velocity toward the look direction, once per
     * airtime. The server bills the Charge.
     */
    private static void redirect(Minecraft mc, LocalPlayer p, BodyState body, boolean grounded) {
        boolean down = mc.options.keySprint.isDown();
        boolean pressed = down && !wasSprintDown;
        wasSprintDown = down;
        int tier = body.tierOf(ImplantIds.INERTIAL_STABILIZER);
        if (!pressed || grounded || redirectUsed || p.isFallFlying() || p.getAbilities().flying || clinging || soleAttached
                || slamming || !Stats.unlocked(ImplantIds.INERTIAL_STABILIZER, tier, "redirect")
                || !affordable(body, Stats.of(ImplantIds.INERTIAL_STABILIZER, tier).value("redirect_charge", 10.0))) {
            return;
        }
        redirectUsed = true;
        double speed = Math.max(p.getDeltaMovement().length(), 0.4);
        Vec3 look = p.getLookAngle().scale(speed);
        p.setDeltaMovement(look);
        p.fallDistance = 0;
        PacketDistributor.sendToServer(new MovementAbilityPayload(MovementAbilityPayload.Kind.REDIRECT, (float) look.x, (float) look.z));
    }

    // ---------------------------------------------------------------- Kinetic Tendons

    /** Tier V Elastic Recoil (Layer 5): a jump within 3 ticks of landing is 30% higher. Jumps are client-authoritative. */
    static void onJump(LivingEvent.LivingJumpEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || mc.player == null) {
            return;
        }
        LocalPlayer p = mc.player;
        int tier = ClientBodyState.get().tierOf(ImplantIds.KINETIC_TENDONS);
        if (p.tickCount - landedTick <= ELASTIC_WINDOW_TICKS && Stats.unlocked(ImplantIds.KINETIC_TENDONS, tier, "elastic_recoil")) {
            Vec3 m = p.getDeltaMovement();
            p.setDeltaMovement(m.x, m.y * ELASTIC_VELOCITY, m.z);
        }
    }

    // ---------------------------------------------------------------- input event

    /** While Hydraulic Legs build pressure the jump is swallowed and walking is cut by 80%. */
    static void onMovementInput(Input input) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || ClientBodyState.get().installed().isEmpty()) {
            return;
        }
        if (mc.options.keyJump.isDown() && chargingEligible(p, ClientBodyState.get(), input.shiftKeyDown)) {
            input.jumping = false;
            input.forwardImpulse *= 0.2f;
            input.leftImpulse *= 0.2f;
        }
    }

    private static boolean affordable(BodyState body, double cost) {
        return cost <= 0 || !Body.chargeEnabled() || body.charge() + 1e-9 >= cost;
    }

    /** Progress 0-1 of the current leap charge, for the HUD. */
    static float leapProgress() {
        int tier = ClientBodyState.get().tierOf(ImplantIds.HYDRAULIC_LEGS);
        if (tier == 0 || leapCharge == 0) {
            return 0f;
        }
        return Mth.clamp(leapCharge / Math.max(1f, (float) Stats.of(ImplantIds.HYDRAULIC_LEGS, tier).value("full_charge_ticks", 30)), 0f, 1f);
    }

    private ClientMovement() {
    }
}
