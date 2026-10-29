package dev.cyberimplants.fx;

import dev.cyberimplants.network.ScreenFxPayload;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * The visual and audio signature of every implant effect (docs/design/01-core-systems.md, section 1.9 rule 5: "every
 * effect is visible"). Particles and sounds go to everyone nearby, so other players can read what an augmented player
 * is doing; {@link #screen} adds a screen-space effect for the user alone. Server side only.
 *
 * <p>Sounds are layered vanilla sounds with shifted pitch, so the mod ships no audio files.
 */
public final class ImplantFx {
    // Neon palette, the same hues as the interface (client/CyberUi).
    public static final Vector3f CYAN = rgb(0x19D9FF);
    public static final Vector3f MAGENTA = rgb(0xFF2BD6);
    public static final Vector3f GREEN = rgb(0x39FF88);
    public static final Vector3f AMBER = rgb(0xFFB830);
    public static final Vector3f RED = rgb(0xFF3A5C);
    public static final Vector3f VIOLET = rgb(0xB45CFF);
    public static final Vector3f WHITE = rgb(0xE6F4FF);
    public static final Vector3f ICE = rgb(0x8FB4E0);

    // ---------------------------------------------------------------- building blocks

    public static DustParticleOptions dust(Vector3f color, float scale) {
        return new DustParticleOptions(color, scale);
    }

    /** {@code count} particles scattered around a point (vanilla's offset spread). */
    public static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    public static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double dx, double dy, double dz,
                             double speed) {
        level.sendParticles(particle, at.x, at.y, at.z, count, dx, dy, dz, speed);
    }

    /** One particle flying in {@code direction} at {@code speed} (a zero count makes the offset a velocity). */
    public static void shoot(ServerLevel level, ParticleOptions particle, Vec3 at, Vec3 direction, double speed) {
        level.sendParticles(particle, at.x, at.y, at.z, 0, direction.x, direction.y, direction.z, speed);
    }

    /** A horizontal ring of {@code points} particles. */
    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        double offset = level.random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < points; i++) {
            double a = offset + Math.PI * 2 * i / points;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius,
                    1, 0, 0, 0, 0);
        }
    }

    /**
     * A ring that flies outward from the centre: a shockwave. Use a particle that keeps its launch velocity (cloud,
     * end rod, spark); dust throws most of it away and barely moves.
     */
    public static void shockwave(ServerLevel level, ParticleOptions particle, Vec3 center, int points, double speed) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            shoot(level, particle, center, new Vec3(Math.cos(a), 0, Math.sin(a)), speed);
        }
    }

    /** Particles spread evenly over a sphere (a Fibonacci lattice, so there are no clumps at the poles). */
    public static void sphere(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        double spin = level.random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < points; i++) {
            double y = 1 - 2.0 * (i + 0.5) / points;
            double r = Math.sqrt(1 - y * y);
            double a = spin + golden * i;
            level.sendParticles(particle, center.x + Math.cos(a) * r * radius, center.y + y * radius,
                    center.z + Math.sin(a) * r * radius, 1, 0, 0, 0, 0);
        }
    }

    /** A straight line of particles. */
    public static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double spacing) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(1, (int) (delta.length() / spacing));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale((double) i / steps));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** A jagged electric arc between two points. */
    public static void arc(ServerLevel level, Vec3 from, Vec3 to, Vector3f color) {
        RandomSource random = level.random;
        Vec3 delta = to.subtract(from);
        int segments = Math.max(3, (int) (delta.length() * 3));
        Vec3 prev = from;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double jitter = i == segments ? 0 : 0.25 * Math.sin(t * Math.PI);
            Vec3 next = from.add(delta.scale(t)).add((random.nextDouble() - 0.5) * jitter * 2,
                    (random.nextDouble() - 0.5) * jitter * 2, (random.nextDouble() - 0.5) * jitter * 2);
            line(level, ParticleTypes.ELECTRIC_SPARK, prev, next, 0.25);
            prev = next;
        }
        line(level, dust(color, 0.6f), from, to, 0.5);
    }

    public static void sound(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void sound(Entity at, Holder<SoundEvent> sound, float volume, float pitch) {
        sound(at, sound.value(), volume, pitch);
    }

    /** A screen effect for the user alone. */
    public static void screen(ServerPlayer player, ScreenFx effect, int ticks, float strength) {
        PacketDistributor.sendToPlayer(player, new ScreenFxPayload(effect.name(), ticks, strength));
    }

    public static void endScreen(ServerPlayer player, ScreenFx effect) {
        screen(player, effect, 0, 0);
    }

    private static Vec3 chest(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.55, 0);
    }

    private static Vec3 feet(Entity e) {
        return e.position().add(0, 0.05, 0);
    }

    // ---------------------------------------------------------------- neural

    public static void sandevistanStart(ServerPlayer player, Vec3 center, double radius, int ticks) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.BEACON_ACTIVATE, 1.0f, 0.6f);
        sound(player, SoundEvents.CONDUIT_ACTIVATE, 0.8f, 1.6f);
        sound(player, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 0.6f, 0.7f);
        ring(level, dust(GREEN, 1.4f), center.add(0, 0.2, 0), radius, Mth.clamp((int) (radius * 10), 20, 60));
        ring(level, dust(MAGENTA, 1.0f), center.add(0, 0.2, 0), radius * 0.5, 16);
        shockwave(level, ParticleTypes.END_ROD, center.add(0, 1.0, 0), 24, radius * 0.06);
        burst(level, ParticleTypes.FLASH, center.add(0, 1.0, 0), 1, 0, 0);
        screen(player, ScreenFx.SANDEVISTAN, ticks, 1f);
    }

    /** The field's dome and the user's afterimage trail. Called every tick while the field holds. */
    public static void sandevistanTick(ServerPlayer player, Vec3 center, double radius, long age) {
        ServerLevel level = player.serverLevel();
        if (age % 4 == 0) {
            ring(level, dust(GREEN, 1.0f), center.add(0, 0.1, 0), radius, Mth.clamp((int) (radius * 8), 16, 48));
        }
        if (age % 6 == 3) {
            sphere(level, dust(age % 12 == 3 ? GREEN : MAGENTA, 0.8f), center.add(0, 1.0, 0), radius, 28);
        }
        // Afterimages: a faint silhouette of dust left where the user was.
        if (age % 2 == 0 && player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3) {
            Vec3 at = player.position();
            burst(level, dust(age % 4 == 0 ? GREEN : MAGENTA, 1.2f), at.add(0, 0.9, 0), 6, 0.18, 0.5, 0.18, 0);
        }
        if (age % 10 == 0) {
            sound(player, SoundEvents.BEACON_AMBIENT, 0.4f, 2.0f);
        }
    }

    public static void sandevistanEnd(ServerPlayer player, Vec3 center) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.BEACON_DEACTIVATE, 0.9f, 0.7f);
        sound(player, SoundEvents.CONDUIT_DEACTIVATE, 0.6f, 1.4f);
        burst(level, ParticleTypes.REVERSE_PORTAL, center.add(0, 1.0, 0), 40, 0.8, 0.05);
        endScreen(player, ScreenFx.SANDEVISTAN);
    }

    public static void kerenzikovDodge(ServerPlayer player, double dirX, double dirZ) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.9f, 1.4f);
        sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.5f, 1.8f);
        Vec3 chest = chest(player);
        // Three fading afterimages strung out behind the dodge.
        for (int i = 1; i <= 3; i++) {
            Vec3 at = chest.subtract(dirX * 0.45 * i, 0, dirZ * 0.45 * i);
            burst(level, dust(VIOLET, 1.4f - i * 0.25f), at, 10 - i * 2, 0.15, 0.45, 0.15, 0);
        }
        burst(level, ParticleTypes.REVERSE_PORTAL, chest, 14, 0.3, 0.05);
        screen(player, ScreenFx.KERENZIKOV, 8, 1f);
    }

    public static void aesSidestep(ServerPlayer player, Vec3 arrowAt) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.7f, 1.8f);
        sound(player, SoundEvents.NOTE_BLOCK_BIT, 0.6f, 1.9f);
        burst(level, dust(CYAN, 1.2f), chest(player), 14, 0.3, 0.5, 0.3, 0);
        burst(level, ParticleTypes.CLOUD, chest(player), 8, 0.3, 0.5, 0.3, 0.02);
        // A cyan tracer where the system saw the arrow.
        line(level, dust(CYAN, 0.6f), arrowAt, chest(player), 0.6);
        screen(player, ScreenFx.EVADE, 10, 1f);
    }

    public static void reflexTrigger(ServerPlayer player) {
        sound(player, SoundEvents.NOTE_BLOCK_HAT, 0.3f, 2.0f);
    }

    // ---------------------------------------------------------------- optical

    public static void camoOn(ServerPlayer player, int ticks) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.8f);
        sound(player, SoundEvents.ILLUSIONER_CAST_SPELL, 0.6f, 1.6f);
        sphere(level, dust(ICE, 0.9f), chest(player), 0.9, 40);
        burst(level, ParticleTypes.REVERSE_PORTAL, chest(player), 20, 0.35, 0.6, 0.35, 0.02);
        screen(player, ScreenFx.CAMO, ticks, 1f);
    }

    /** The faint shimmer that gives a cloaked user away when they move, and always in rain or water. */
    public static void camoShimmer(ServerPlayer player) {
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3;
        boolean wet = player.isInWaterOrRain();
        if (!moving && !wet) {
            return;
        }
        burst(player.serverLevel(), dust(ICE, 0.5f), chest(player), wet ? 4 : 2, 0.25, 0.5, 0.25, 0);
    }

    public static void camoOff(ServerPlayer player, boolean broken) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.6f, broken ? 0.7f : 1.2f);
        if (broken) {
            sound(player, SoundEvents.GLASS_BREAK, 0.5f, 1.6f);
        }
        burst(level, dust(ICE, 1.0f), chest(player), broken ? 30 : 16, 0.3, 0.6, 0.3, 0);
        burst(level, ParticleTypes.PORTAL, chest(player), 20, 0.3, 0.6, 0.3, 0.4);
        endScreen(player, ScreenFx.CAMO);
    }

    public static void scanPulse(ServerPlayer player, double radius, int found) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 1.6f);
        sound(player, SoundEvents.BEACON_POWER_SELECT, 0.6f, 2.0f);
        if (found > 0) {
            sound(player, SoundEvents.NOTE_BLOCK_PLING, 0.5f, 1.4f + Math.min(found, 8) * 0.06f);
        }
        Vec3 at = feet(player).add(0, 0.3, 0);
        // The ping: a cyan wave racing out to the scan radius.
        shockwave(level, ParticleTypes.END_ROD, at.add(0, 0.6, 0), 32, radius * 0.03);
        shockwave(level, ParticleTypes.ELECTRIC_SPARK, at, 24, radius * 0.04);
        ring(level, dust(CYAN, 1.0f), at, 1.2, 20);
        ring(level, dust(CYAN, 1.2f), at, radius * 0.5, Mth.clamp((int) (radius * 3), 16, 48));
        ring(level, dust(CYAN, 1.5f), at, radius, Mth.clamp((int) (radius * 5), 24, 72));
        screen(player, ScreenFx.SCAN, 24, Mth.clamp(found / 8f, 0.2f, 1f));
    }

    /** A reveal blip over one scanned entity, coloured by threat class. */
    public static void scanMark(ServerPlayer player, Entity target, String kind) {
        Vector3f color = switch (kind) {
            case "hostile", "boss" -> RED;
            case "neutral" -> AMBER;
            case "player" -> CYAN;
            default -> GREEN;
        };
        Vec3 top = target.position().add(0, target.getBbHeight() + 0.4, 0);
        burst(player.serverLevel(), dust(color, 1.2f), top, 6, 0.1, 0.1, 0.1, 0);
    }

    public static void tag(ServerPlayer player, Entity target) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.9f);
        sound(player, SoundEvents.LODESTONE_COMPASS_LOCK, 0.8f, 1.5f);
        Vec3 center = chest(target);
        double r = Math.max(0.6, target.getBbWidth());
        ring(level, dust(RED, 1.0f), center, r, 20);
        ring(level, dust(RED, 0.7f), center.add(0, 0.25, 0), r * 0.7, 14);
        line(level, dust(RED, 0.5f), player.getEyePosition().add(0, -0.2, 0), center, 0.8);
        screen(player, ScreenFx.TAG, 10, 1f);
    }

    /** A small reticle that keeps marking the tagged target for anyone watching. */
    public static void tagHold(ServerPlayer player, Entity target) {
        Vec3 top = target.position().add(0, target.getBbHeight() + 0.35, 0);
        ring(player.serverLevel(), dust(RED, 0.6f), top, 0.3, 8);
    }

    public static void opticToggle(ServerPlayer player, boolean on) {
        if (on) {
            sound(player, SoundEvents.SPYGLASS_USE, 0.8f, 1.4f);
        } else {
            sound(player, SoundEvents.SPYGLASS_STOP_USING, 0.8f, 1.0f);
        }
        sound(player, SoundEvents.NOTE_BLOCK_BIT, 0.4f, on ? 1.6f : 1.0f);
        burst(player.serverLevel(), dust(on ? AMBER : WHITE, 0.6f), player.getEyePosition(), 6, 0.12, 0.05, 0.12, 0);
    }

    // ---------------------------------------------------------------- circulatory

    public static void defib(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.TOTEM_USE, 1.0f, 1.2f);
        sound(player, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f, 1.6f);
        sound(player, SoundEvents.WARDEN_HEARTBEAT, 1.0f, 1.0f);
        Vec3 chest = chest(player);
        burst(level, ParticleTypes.ELECTRIC_SPARK, chest, 40, 0.5, 0.8, 0.5, 0.35);
        burst(level, ParticleTypes.FLASH, chest, 1, 0, 0);
        burst(level, ParticleTypes.TOTEM_OF_UNDYING, chest, 30, 0.4, 0.6, 0.4, 0.4);
        shockwave(level, ParticleTypes.ELECTRIC_SPARK, feet(player).add(0, 0.2, 0), 32, 0.4);
        ring(level, dust(GREEN, 1.5f), feet(player).add(0, 0.2, 0), 1.2, 24);
        arc(level, chest.add(0.6, 0.6, 0), chest, GREEN);
        arc(level, chest.add(-0.6, 0.6, 0), chest, GREEN);
        screen(player, ScreenFx.DEFIB, 50, 1f);
    }

    public static void adrenaline(ServerPlayer player, int ticks) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.WARDEN_HEARTBEAT, 1.0f, 1.4f);
        sound(player, SoundEvents.PLAYER_BREATH, 0.6f, 1.3f);
        burst(level, dust(RED, 1.3f), chest(player), 20, 0.35, 0.5, 0.35, 0);
        burst(level, ParticleTypes.DAMAGE_INDICATOR, chest(player), 4, 0.3, 0.3, 0.3, 0.1);
        screen(player, ScreenFx.ADRENALINE, ticks, 1f);
    }

    /** A pulse of red at the heart and a faint trail while the boost lasts. */
    public static void adrenalineTick(ServerPlayer player, long age) {
        if (age % 10 == 0) {
            sound(player, SoundEvents.WARDEN_HEARTBEAT, 0.5f, 1.6f);
        }
        if (age % 2 == 0) {
            burst(player.serverLevel(), dust(RED, 0.8f), player.position().add(0, 0.6, 0), 2, 0.2, 0.3, 0.2, 0);
        }
    }

    public static void regenerate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        burst(level, dust(GREEN, 0.7f), chest(player), 5, 0.3, 0.5, 0.3, 0);
        burst(level, ParticleTypes.HAPPY_VILLAGER, chest(player), 2, 0.3, 0.4, 0.3, 0);
        if (player.tickCount % 60 == 0) {
            sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.25f, 1.8f);
        }
    }

    public static void ers(ServerPlayer player, int responses) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.NOTE_BLOCK_PLING, 1.0f, 0.7f);
        sound(player, SoundEvents.NOTE_BLOCK_PLING, 1.0f, 0.5f);
        sound(player, SoundEvents.BELL_RESONATE, 0.4f, 1.6f);
        ring(level, dust(RED, 1.4f), feet(player).add(0, 0.1, 0), 1.0, 24);
        ring(level, dust(RED, 1.0f), feet(player).add(0, 2.1, 0), 0.6, 14);
        screen(player, ScreenFx.ERS, 24, Mth.clamp(responses / 3f, 0.4f, 1f));
    }

    public static void bufferFull(ServerPlayer player) {
        sound(player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.4f, 1.8f);
        burst(player.serverLevel(), dust(AMBER, 0.8f), chest(player), 10, 0.3, 0.4, 0.3, 0);
    }

    // ---------------------------------------------------------------- subdermal

    public static void shellOn(ServerPlayer player, int ticks) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.ANVIL_PLACE, 0.8f, 1.5f);
        sound(player, SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 0.8f);
        sound(player, SoundEvents.IRON_GOLEM_REPAIR, 0.7f, 0.8f);
        sphere(level, dust(AMBER, 1.2f), chest(player), 0.85, 48);
        burst(level, ParticleTypes.CRIT, chest(player), 20, 0.4, 0.6, 0.4, 0.1);
        screen(player, ScreenFx.SHELL, ticks, 1f);
    }

    public static void shellHold(ServerPlayer player, long age) {
        if (age % 5 == 0) {
            sphere(player.serverLevel(), dust(AMBER, 0.7f), chest(player), 0.85, 16);
        }
    }

    public static void shellOff(ServerPlayer player) {
        sound(player, SoundEvents.IRON_TRAPDOOR_OPEN, 0.7f, 0.7f);
        burst(player.serverLevel(), dust(AMBER, 0.9f), chest(player), 18, 0.4, 0.6, 0.4, 0);
        endScreen(player, ScreenFx.SHELL);
    }

    public static void shieldAbsorb(ServerPlayer player, double absorbed, boolean broken) {
        ServerLevel level = player.serverLevel();
        if (broken) {
            sound(player, SoundEvents.SHIELD_BREAK, 0.9f, 1.3f);
            sound(player, SoundEvents.GLASS_BREAK, 0.7f, 1.4f);
            sphere(level, dust(GREEN, 1.3f), chest(player), 0.95, 56);
            burst(level, ParticleTypes.ELECTRIC_SPARK, chest(player), 24, 0.5, 0.7, 0.5, 0.3);
            screen(player, ScreenFx.SHIELD_BREAK, 16, 1f);
            return;
        }
        sound(player, SoundEvents.SHIELD_BLOCK, 0.7f, 1.6f);
        sound(player, SoundEvents.AMETHYST_BLOCK_HIT, 0.8f, 1.4f);
        sphere(level, dust(GREEN, 0.8f), chest(player), 0.95, (int) Mth.clamp(12 + absorbed * 6, 12, 40));
        screen(player, ScreenFx.SHIELD_HIT, 8, (float) Mth.clamp(absorbed / 4.0, 0.3, 1.0));
    }

    public static void shieldFull(ServerPlayer player) {
        sound(player, SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.9f);
        sphere(player.serverLevel(), dust(GREEN, 0.6f), chest(player), 0.95, 24);
    }

    public static void shieldEmergency(ServerPlayer player) {
        sound(player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.9f, 1.4f);
        sphere(player.serverLevel(), dust(GREEN, 1.1f), chest(player), 0.95, 48);
    }

    public static void shock(ServerPlayer player, Entity target, boolean chained) {
        ServerLevel level = player.serverLevel();
        if (!chained) {
            sound(player, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.9f);
            sound(player, SoundEvents.BEEHIVE_WORK, 0.8f, 2.0f);
            burst(level, ParticleTypes.ELECTRIC_SPARK, chest(player), 16, 0.4, 0.6, 0.4, 0.25);
            screen(player, ScreenFx.SHOCK, 6, 1f);
        }
        arc(level, chest(chained ? target : player), chest(target), ICE);
        burst(level, ParticleTypes.ELECTRIC_SPARK, chest(target), 14, 0.3, 0.4, 0.3, 0.2);
        burst(level, ParticleTypes.SMOKE, chest(target), 4, 0.2, 0.3, 0.2, 0.01);
    }

    /** Draws the arc between the first victim and the one the discharge jumps to (Tier III). */
    public static void shockChain(ServerPlayer player, Entity from, Entity to) {
        ServerLevel level = player.serverLevel();
        arc(level, chest(from), chest(to), ICE);
        burst(level, ParticleTypes.ELECTRIC_SPARK, chest(to), 10, 0.3, 0.4, 0.3, 0.2);
        sound(to, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.3f, 2.0f);
    }

    // ---------------------------------------------------------------- skeletal

    public static void powerStrike(ServerPlayer player, Entity target) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
        sound(player, SoundEvents.PISTON_EXTEND, 0.6f, 1.6f);
        sound(player, SoundEvents.ANVIL_LAND, 0.25f, 1.8f);
        Vec3 hit = chest(target);
        Vec3 dir = hit.subtract(player.getEyePosition()).normalize();
        burst(level, ParticleTypes.CRIT, hit, 16, 0.3, 0.3, 0.3, 0.4);
        burst(level, ParticleTypes.SWEEP_ATTACK, hit, 1, 0, 0);
        burst(level, dust(AMBER, 1.3f), hit, 10, 0.25, 0.3, 0.25, 0);
        for (int i = 0; i < 8; i++) {
            shoot(level, ParticleTypes.CLOUD, hit, dir.add(level.random.nextGaussian() * 0.3, level.random.nextGaussian() * 0.3,
                    level.random.nextGaussian() * 0.3), 0.25);
        }
        screen(player, ScreenFx.STRIKE, 6, 1f);
    }

    public static void landing(ServerPlayer player, double fall, double recovered) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.PISTON_CONTRACT, 0.7f, 0.6f);
        sound(player, SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.8f);
        shockwave(level, ParticleTypes.CLOUD, feet(player).add(0, 0.1, 0), 20, Mth.clamp(fall * 0.02, 0.1, 0.35));
        if (recovered > 0) {
            sound(player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.5f, 1.8f);
            burst(level, dust(AMBER, 1.0f), feet(player).add(0, 0.5, 0), 14, 0.4, 0.3, 0.4, 0);
            burst(level, ParticleTypes.ELECTRIC_SPARK, feet(player).add(0, 0.3, 0), 10, 0.4, 0.1, 0.4, 0.1);
        }
        screen(player, ScreenFx.LANDING, 8, (float) Mth.clamp(fall / 20.0, 0.3, 1.0));
    }

    // ---------------------------------------------------------------- limbs

    private static Vec3 hand(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        return player.getEyePosition().add(look.scale(0.6)).add(right.scale(0.35)).add(0, -0.35, 0);
    }

    public static void blade(ServerPlayer player, boolean out) {
        ServerLevel level = player.serverLevel();
        if (out) {
            sound(player, SoundEvents.IRON_GOLEM_ATTACK, 0.7f, 1.4f);
            sound(player, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 1.6f);
        } else {
            sound(player, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.7f, 1.4f);
            sound(player, SoundEvents.CHAIN_PLACE, 0.8f, 1.2f);
        }
        burst(level, out ? ParticleTypes.CRIT : ParticleTypes.SMOKE, hand(player), out ? 10 : 4, 0.08, 0.15);
        if (out) {
            burst(level, ParticleTypes.ELECTRIC_SPARK, hand(player), 6, 0.08, 0.1);
        }
    }

    public static void ballisticFire(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.CROSSBOW_SHOOT, 0.6f, 1.8f);
        sound(player, SoundEvents.FIRECHARGE_USE, 0.35f, 2.0f);
        Vec3 muzzle = hand(player).add(player.getLookAngle().scale(0.3));
        burst(level, ParticleTypes.SMOKE, muzzle, 5, 0.04, 0.02);
        burst(level, ParticleTypes.FLAME, muzzle, 2, 0.02, 0.01);
        shoot(level, ParticleTypes.CRIT, muzzle, player.getLookAngle(), 1.2);
    }

    public static void ballisticReload(ServerPlayer player) {
        sound(player, SoundEvents.CROSSBOW_LOADING_END, 0.8f, 1.0f);
        sound(player, SoundEvents.IRON_TRAPDOOR_OPEN, 0.5f, 1.8f);
        burst(player.serverLevel(), ParticleTypes.SMOKE, hand(player), 3, 0.05, 0.01);
    }

    public static void manipulate(ServerPlayer player, Vec3 target) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.CHAIN_PLACE, 0.6f, 1.8f);
        sound(player, SoundEvents.NOTE_BLOCK_BIT, 0.4f, 2.0f);
        burst(level, dust(CYAN, 1.0f), target, 10, 0.2, 0.2, 0.2, 0);
        ring(level, dust(CYAN, 0.7f), target, 0.35, 10);
    }

    // ---------------------------------------------------------------- mobility

    public static void airJump(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.PISTON_EXTEND, 0.5f, 1.7f);
        sound(player, SoundEvents.PHANTOM_FLAP, 0.6f, 1.4f);
        shockwave(level, ParticleTypes.CLOUD, feet(player), 14, 0.15);
        ring(level, dust(GREEN, 1.0f), feet(player), 0.5, 12);
    }

    public static void leap(ServerPlayer player, float power) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.PISTON_EXTEND, 0.9f, 0.6f);
        sound(player, SoundEvents.GOAT_LONG_JUMP, 0.8f, 0.8f);
        sound(player, SoundEvents.FIRE_EXTINGUISH, 0.5f + 0.4f * power, 1.4f);
        Vec3 at = feet(player);
        shockwave(level, ParticleTypes.CLOUD, at.add(0, 0.1, 0), 24, 0.15 + 0.2 * power);
        burst(level, ParticleTypes.LARGE_SMOKE, at, (int) (6 + 10 * power), 0.3, 0.05, 0.3, 0.02);
        burst(level, dust(AMBER, 1.2f), at.add(0, 0.2, 0), 10, 0.3, 0.1, 0.3, 0);
        screen(player, ScreenFx.LEAP, 14, power);
    }

    public static void soles(ServerPlayer player, boolean on) {
        if (on) {
            sound(player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.4f, 1.8f);
        } else {
            sound(player, SoundEvents.RESPAWN_ANCHOR_DEPLETE, 0.4f, 1.8f);
        }
        burst(player.serverLevel(), ParticleTypes.ELECTRIC_SPARK, feet(player), 8, 0.25, 0.05, 0.25, 0.05);
    }

    public static void solesClamp(ServerPlayer player) {
        sound(player, SoundEvents.CHAIN_PLACE, 0.7f, 0.8f);
        burst(player.serverLevel(), ParticleTypes.ELECTRIC_SPARK, feet(player), 10, 0.25, 0.1, 0.25, 0.1);
    }

    /** Sparks where the soles grip the wall. */
    public static void solesHold(ServerPlayer player) {
        burst(player.serverLevel(), ParticleTypes.ELECTRIC_SPARK, feet(player).add(0, 0.1, 0), 1, 0.2, 0.05, 0.2, 0.02);
    }

    public static void dash(ServerPlayer player, double fraction) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f);
        sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 0.7f);
        Vec3 look = player.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z).normalize();
        Vec3 at = player.position().add(0, 0.8, 0);
        for (int i = 0; i < 6; i++) {
            Vec3 p = at.add(back.scale(i * 0.4));
            burst(level, dust(i % 2 == 0 ? MAGENTA : CYAN, 1.3f), p, 4, 0.12, 0.4, 0.12, 0);
        }
        burst(level, ParticleTypes.CLOUD, feet(player), 10, 0.3, 0.05, 0.3, 0.05);
        burst(level, ParticleTypes.FIREWORK, at, (int) (6 + 10 * fraction), 0.2, 0.3, 0.2, 0.08);
        screen(player, ScreenFx.DASH, 12, (float) fraction);
    }

    /** A streak of magenta behind a player who is sprinting with a full Momentum gauge. */
    public static void momentumTrail(ServerPlayer player) {
        burst(player.serverLevel(), dust(MAGENTA, 0.7f), player.position().add(0, 0.4, 0), 1, 0.1, 0.2, 0.1, 0);
    }

    public static void momentumFull(ServerPlayer player) {
        sound(player, SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.6f);
        sound(player, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.4f, 1.6f);
        ring(player.serverLevel(), dust(MAGENTA, 1.0f), player.position().add(0, 0.2, 0), 0.7, 16);
    }

    // ---------------------------------------------------------------- body

    /** An activation that did not happen (no Charge, cooling down, ...). */
    public static void fizzle(ServerPlayer player) {
        burst(player.serverLevel(), ParticleTypes.SMOKE, chest(player), 4, 0.2, 0.3, 0.2, 0.01);
    }

    public static void glitch(ServerPlayer player, boolean implantDown) {
        ServerLevel level = player.serverLevel();
        sound(player, SoundEvents.BEACON_DEACTIVATE, implantDown ? 0.8f : 0.5f, implantDown ? 1.2f : 1.8f);
        sound(player, SoundEvents.SCULK_CLICKING, 0.8f, 0.6f);
        burst(level, ParticleTypes.ELECTRIC_SPARK, player.getEyePosition(), implantDown ? 16 : 8, 0.3, 0.3, 0.3, 0.15);
        burst(level, dust(MAGENTA, 0.8f), chest(player), 8, 0.3, 0.5, 0.3, 0);
        screen(player, ScreenFx.GLITCH, implantDown ? 16 : 10, implantDown ? 1f : 0.6f);
    }

    private static Vector3f rgb(int rgb) {
        return new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
    }

    private ImplantFx() {
    }
}
