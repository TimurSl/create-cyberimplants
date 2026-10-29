package dev.cyberimplants.body;

import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantFlag;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.ImplantItem;
import dev.cyberimplants.implant.ImplantRuntime;
import dev.cyberimplants.implant.ImplantType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Installation and removal rules (docs/design/01-core-systems.md, sections 1.1 and 1.5), as used by the Implantation Rig. */
public final class BodyOps {
    public enum Result {
        OK,
        UNKNOWN_IMPLANT,
        WRONG_SLOT,
        OCCUPIED,
        NO_CORE,
        DUPLICATE,
        NO_ARM_FRAME,
        NO_CHASSIS,
        HOST_TIER,
        OVERLOAD,
        EMPTY_SLOT,
        HAS_DEPENDENTS,
        /** The host (chassis or arm frame) has no free socket at that index. */
        NO_SOCKET,
        /** The chassis cannot carry another module of that kind. */
        TOO_MANY;

        public String translationKey() {
            return "cyberimplants.rig." + name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean ok() {
            return this == OK;
        }
    }

    /** How many slots of the region are usable: deck sockets follow the chassis tier, arm sockets the frame tier. */
    public static int effectiveSlots(BodyState s, BodyRegion region) {
        return switch (region) {
            case DECK -> {
                InstalledImplant chassis = s.installed().get(new SlotId(BodyRegion.SPINE, 0));
                yield chassis == null ? 0 : Math.min(region.slots(),
                        (int) Stats.of(ImplantIds.DECK_CHASSIS, chassis.tier()).value("module_sockets", 3));
            }
            case LEFT_SOCKET -> frameSockets(s, BodyRegion.LEFT_ARM, region);
            case RIGHT_SOCKET -> frameSockets(s, BodyRegion.RIGHT_ARM, region);
            default -> region.slots();
        };
    }

    private static int frameSockets(BodyState s, BodyRegion frameRegion, BodyRegion socketRegion) {
        InstalledImplant frame = s.installed().get(new SlotId(frameRegion, 0));
        return frame == null ? 0 : Math.min(socketRegion.slots(),
                (int) Stats.of(ImplantIds.CYBERNETIC_ARM, frame.tier()).value("sockets", 1));
    }

    /** Deck modules that may be installed several times: RAM and Storage are stackable. */
    private static boolean stackable(ResourceLocation typeId) {
        return typeId.equals(ImplantIds.DECK_RAM) || typeId.equals(ImplantIds.DECK_STORAGE);
    }

    public static Result canInstall(BodyState s, ResourceLocation typeId, int tier, SlotId slot) {
        ImplantType type = ImplantCatalog.get(typeId);
        if (type == null) {
            return Result.UNKNOWN_IMPLANT;
        }
        if (!ImplantCatalog.regionsFor(type).contains(slot.region())) {
            return Result.WRONG_SLOT;
        }
        if (s.installed().containsKey(slot)) {
            return Result.OCCUPIED;
        }
        if (!typeId.equals(ImplantIds.CORE) && !Body.hasCore(s)) {
            return Result.NO_CORE;
        }
        // No duplicates within a region. Arm sockets are separate regions, so the same module may sit in both arms.
        for (Map.Entry<SlotId, InstalledImplant> e : s.installed().entrySet()) {
            if (e.getKey().region() == slot.region() && e.getValue().type().equals(typeId) && !stackable(typeId)
                    && !typeId.equals(ImplantIds.DECK_COPROCESSOR)) {
                return Result.DUPLICATE;
            }
        }
        if (type.has(ImplantFlag.ARM_MODULE)) {
            BodyRegion frameRegion = slot.region() == BodyRegion.LEFT_SOCKET ? BodyRegion.LEFT_ARM : BodyRegion.RIGHT_ARM;
            InstalledImplant frame = s.installed().get(new SlotId(frameRegion, 0));
            if (frame == null) {
                return Result.NO_ARM_FRAME;
            }
            if (tier > frame.tier()) {
                return Result.HOST_TIER;
            }
        }
        if (type.has(ImplantFlag.DECK_MODULE)) {
            InstalledImplant chassis = s.installed().get(new SlotId(BodyRegion.SPINE, 0));
            if (chassis == null) {
                return Result.NO_CHASSIS;
            }
            if (tier > chassis.tier()) {
                return Result.HOST_TIER;
            }
            if (typeId.equals(ImplantIds.DECK_COPROCESSOR)) {
                long have = s.installed().values().stream().filter(i -> i.type().equals(typeId)).count();
                int max = (int) Stats.of(ImplantIds.DECK_CHASSIS, chassis.tier()).value("max_coprocessors", 1);
                if (have >= max) {
                    return Result.TOO_MANY;
                }
            }
        }
        if (slot.index() >= effectiveSlots(s, slot.region())) {
            return Result.NO_SOCKET;
        }
        double after = Body.totalLoad(s) + Body.loadOf(typeId, tier);
        double capacity = typeId.equals(ImplantIds.CORE) ? Body.capacity(s.withImplant(slot, new InstalledImplant(typeId, tier))) : Body.capacity(s);
        double margin = typeId.equals(ImplantIds.CORE) ? Body.overloadMargin(s.withImplant(slot, new InstalledImplant(typeId, tier))) : Body.overloadMargin(s);
        if (after > capacity * (1 + margin) + 1e-9) {
            return Result.OVERLOAD;
        }
        return Result.OK;
    }

    /** Every free slot the implant could go into right now. */
    public static List<SlotId> candidateSlots(BodyState s, ResourceLocation typeId, int tier) {
        ImplantType type = ImplantCatalog.get(typeId);
        List<SlotId> out = new ArrayList<>();
        if (type == null) {
            return out;
        }
        for (BodyRegion region : ImplantCatalog.regionsFor(type)) {
            for (int i = 0; i < effectiveSlots(s, region); i++) {
                SlotId slot = new SlotId(region, i);
                if (canInstall(s, typeId, tier, slot).ok()) {
                    out.add(slot);
                }
            }
        }
        return out;
    }

    public static Result canRemove(BodyState s, SlotId slot) {
        InstalledImplant item = s.installed().get(slot);
        if (item == null) {
            return Result.EMPTY_SLOT;
        }
        if (item.type().equals(ImplantIds.CORE) && s.installed().size() > 1) {
            return Result.HAS_DEPENDENTS;
        }
        if (slot.region() == BodyRegion.LEFT_ARM && s.installed().keySet().stream().anyMatch(k -> k.region() == BodyRegion.LEFT_SOCKET)) {
            return Result.HAS_DEPENDENTS;
        }
        if (slot.region() == BodyRegion.RIGHT_ARM && s.installed().keySet().stream().anyMatch(k -> k.region() == BodyRegion.RIGHT_SOCKET)) {
            return Result.HAS_DEPENDENTS;
        }
        if (slot.region() == BodyRegion.SPINE && s.installed().keySet().stream().anyMatch(k -> k.region() == BodyRegion.DECK)) {
            return Result.HAS_DEPENDENTS;
        }
        return Result.OK;
    }

    /** Installs one item from the stack. The stack is only shrunk on success. */
    public static Result install(ServerPlayer player, SlotId slot, ItemStack stack) {
        if (!(stack.getItem() instanceof ImplantItem item)) {
            return Result.UNKNOWN_IMPLANT;
        }
        int tier = ImplantItem.tierOf(stack);
        BodyState s = BodyState.get(player);
        Result r = canInstall(s, item.typeId(), tier, slot);
        if (!r.ok()) {
            return r;
        }
        Body.update(player, st -> {
            BodyState next = st.withImplant(slot, new InstalledImplant(item.typeId(), tier));
            if (item.typeId().equals(ImplantIds.CORE)) {
                // A freshly installed Core starts with a quarter of its battery.
                next = next.withCharge(Math.max(next.charge(), Body.maxCharge(next) * 0.25), 0);
            }
            return next;
        });
        stack.shrink(1);
        ImplantRuntime.bodyChanged(player);
        return Result.OK;
    }

    /** Removes the implant and hands the item back to the player (dropping it if the inventory is full). */
    public static Result remove(ServerPlayer player, SlotId slot) {
        BodyState s = BodyState.get(player);
        Result r = canRemove(s, slot);
        if (!r.ok()) {
            return r;
        }
        InstalledImplant implant = s.installed().get(slot);
        ItemStack stack = ImplantItem.stackOf(implant.type(), implant.tier());
        Body.update(player, st -> {
            BodyState next = st.withoutImplant(slot);
            return next.withCharge(Math.min(next.charge(), Body.maxCharge(next)), Math.min(next.buffer(), Body.bufferCapacity(next)));
        });
        ImplantRuntime.bodyChanged(player);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return Result.OK;
    }

    private BodyOps() {
    }
}
