package dev.cyberimplants.compat.create;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/** Real goggle bridge: the Machine Analyzer shows everything Engineer's Goggles show, without a helmet slot. */
final class GoggleBridgeImpl implements GoggleBridge {
    @Override
    public void registerWearingPredicate(Predicate<Player> predicate) {
        GogglesItem.addIsWearingPredicate(predicate);
    }
}
