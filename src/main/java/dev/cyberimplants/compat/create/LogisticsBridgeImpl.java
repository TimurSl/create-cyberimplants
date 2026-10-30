package dev.cyberimplants.compat.create;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/** Create 6 logistics: reads a linked block's network frequency, the network's stock summary, and places requests. */
final class LogisticsBridgeImpl implements LogisticsBridge {
    @Override
    public Optional<UUID> network(ServerPlayer player, BlockPos pos) {
        if (!player.level().isLoaded(pos)) {
            return Optional.empty();
        }
        LogisticallyLinkedBehaviour link = BlockEntityBehaviour.get(player.level(), pos, LogisticallyLinkedBehaviour.TYPE);
        if (link == null || link.freqId == null || !link.mayInteract(player)) {
            return Optional.empty();
        }
        return Optional.of(link.freqId);
    }

    @Override
    public OptionalInt stock(UUID network, ItemStack item) {
        InventorySummary summary = LogisticsManager.getSummaryOfNetwork(network, false);
        return summary == null ? OptionalInt.empty() : OptionalInt.of(summary.getCountOf(item));
    }

    @Override
    public boolean request(UUID network, ItemStack item, int count, String address) {
        PackageOrderWithCrafts order = PackageOrderWithCrafts.simple(List.of(new BigItemStack(item.copyWithCount(1), count)));
        return LogisticsManager.broadcastPackageRequest(network, LogisticallyLinkedBehaviour.RequestType.REDSTONE, order, null, address);
    }
}
