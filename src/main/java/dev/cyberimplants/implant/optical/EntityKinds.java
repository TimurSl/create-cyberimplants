package dev.cyberimplants.implant.optical;

import dev.cyberimplants.implant.Tags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Threat classes shown by the Entity Scanner and exposed as {@code .kind} to scripts. */
public final class EntityKinds {
    public static String classify(Entity e) {
        if (e instanceof Player) {
            return "player";
        }
        if (e.getType().is(Tags.AUGMENT_RESISTANT)) {
            return "boss";
        }
        if (e instanceof Enemy) {
            return "hostile";
        }
        if (e instanceof NeutralMob) {
            return "neutral";
        }
        return "passive";
    }

    private EntityKinds() {
    }
}
