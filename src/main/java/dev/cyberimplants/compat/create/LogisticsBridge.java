package dev.cyberimplants.compat.create;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.OptionalInt;

/** Create 6 stock network: stock reads and package requests (Machine Interface tier 5, built last). */
public interface LogisticsBridge {
    default OptionalInt stock(ServerPlayer owner, String network, ResourceLocation item) {
        return OptionalInt.empty();
    }

    default boolean request(ServerPlayer owner, String network, ResourceLocation item, int count, String address) {
        return false;
    }
}
