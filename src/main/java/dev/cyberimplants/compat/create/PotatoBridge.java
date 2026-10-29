package dev.cyberimplants.compat.create;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Potato Cannon projectile types, for Ballistic Module ammo and Ballistic Visor arcs. */
public interface PotatoBridge {
    record ProjectileProfile(double gravity, double drag, double speed, float damage) {
    }

    default Optional<ProjectileProfile> profile(ItemStack ammo) {
        return Optional.empty();
    }
}
