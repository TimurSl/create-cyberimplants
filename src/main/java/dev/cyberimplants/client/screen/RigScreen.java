package dev.cyberimplants.client.screen;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyOps;
import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.client.ClientBodyState;
import dev.cyberimplants.client.CyberUi;
import dev.cyberimplants.client.CyberUi.CyberButton;
import dev.cyberimplants.implant.ImplantItem;
import dev.cyberimplants.network.RigActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The Implantation Rig body screen: pick an implant from the inventory on the left, then click an empty body slot to
 * install it, or click an occupied slot to remove it. The server validates every operation.
 */
public class RigScreen extends Screen {
    private static final int ROW = 14;
    private static final int LIST_W = 150;
    private static final int SLOT_W = 168;

    private final BlockPos rig;
    private int selectedInventorySlot = -1;
    private BodyState lastBody;
    private String lastInventory = "";

    public RigScreen(BlockPos rig) {
        super(Component.translatable("cyberimplants.rig.title"));
        this.rig = rig;
    }

    @Override
    protected void init() {
        lastBody = ClientBodyState.get();
        lastInventory = inventorySignature();
        BodyState body = lastBody;

        // Left: implants carried in the inventory.
        int y = 30;
        Inventory inventory = minecraft.player.getInventory();
        int shown = 0;
        for (int slot = 0; slot < inventory.getContainerSize() && shown < 16; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof ImplantItem item)) {
                continue;
            }
            int slotIndex = slot;
            boolean selected = slot == selectedInventorySlot;
            Component label = Component.literal(selected ? "> " : "").append(stack.getHoverName())
                    .append(" [" + ImplantItem.tierOf(stack) + "] L" + Body.loadOf(item.typeId(), ImplantItem.tierOf(stack)));
            addRenderableWidget(CyberButton.of(10, y, LIST_W, ROW, label, b -> {
                selectedInventorySlot = slotIndex;
                rebuildWidgets();
            }, selected ? CyberUi.MAGENTA : CyberUi.CYAN));
            y += ROW + 1;
            shown++;
        }

        // Right: the body, two columns of slots.
        List<SlotId> slots = new ArrayList<>();
        for (BodyRegion region : BodyRegion.values()) {
            for (int i = 0; i < BodyOps.effectiveSlots(body, region); i++) {
                slots.add(new SlotId(region, i));
            }
        }
        int perColumn = (slots.size() + 1) / 2;
        for (int n = 0; n < slots.size(); n++) {
            SlotId slot = slots.get(n);
            int column = n / perColumn;
            int row = n % perColumn;
            int x = 10 + LIST_W + 12 + column * (SLOT_W + 6);
            InstalledImplant installed = body.installed().get(slot);
            Component region = Component.translatable("cyberimplants.region." + slot.region().getSerializedName());
            Component text;
            if (installed != null) {
                text = Component.literal("").append(region).append(" " + (slot.index() + 1) + ": ")
                        .append(Component.translatable("item.cyberimplants." + installed.type().getPath()))
                        .append(" [" + installed.tier() + "]  x");
            } else {
                text = Component.literal("").append(region).append(" " + (slot.index() + 1) + ": ")
                        .append(Component.translatable("cyberimplants.rig.empty"));
            }
            CyberButton button = CyberButton.of(x, 30 + row * (ROW + 1), SLOT_W, ROW, text,
                    b -> onSlotClicked(slot, installed != null), installed != null ? CyberUi.GREEN : CyberUi.CYAN);
            button.active = installed != null || canInstallSelected(body, slot);
            addRenderableWidget(button);
        }
        addRenderableWidget(CyberButton.of(width / 2 - 40, height - 26, 80, 20, Component.translatable("gui.done"), b -> onClose(), CyberUi.MAGENTA));
    }

    private boolean canInstallSelected(BodyState body, SlotId slot) {
        ItemStack stack = selectedStack();
        if (stack == null) {
            return false;
        }
        ImplantItem item = (ImplantItem) stack.getItem();
        return BodyOps.canInstall(body, item.typeId(), ImplantItem.tierOf(stack), slot).ok();
    }

    private ItemStack selectedStack() {
        if (selectedInventorySlot < 0 || selectedInventorySlot >= minecraft.player.getInventory().getContainerSize()) {
            return null;
        }
        ItemStack stack = minecraft.player.getInventory().getItem(selectedInventorySlot);
        return stack.getItem() instanceof ImplantItem ? stack : null;
    }

    private void onSlotClicked(SlotId slot, boolean occupied) {
        if (occupied) {
            PacketDistributor.sendToServer(new RigActionPayload(rig, false, slot.region().ordinal(), slot.index(), -1));
        } else if (selectedStack() != null) {
            PacketDistributor.sendToServer(new RigActionPayload(rig, true, slot.region().ordinal(), slot.index(), selectedInventorySlot));
        }
    }

    private String inventorySignature() {
        StringBuilder sb = new StringBuilder();
        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof ImplantItem) {
                sb.append(i).append(':').append(stack.getCount()).append(':').append(ImplantItem.tierOf(stack)).append(';');
            }
        }
        return sb.toString();
    }

    @Override
    public void tick() {
        // The server changes the body and the inventory; rebuild when we notice.
        if (ClientBodyState.get() != lastBody || !inventorySignature().equals(lastInventory)) {
            if (selectedStack() == null) {
                selectedInventorySlot = -1;
            }
            rebuildWidgets();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        CyberUi.backdrop(g, width, height, minecraft.player == null ? 0 : minecraft.player.tickCount + partialTick);
        int rightX = 10 + LIST_W + 12;
        CyberUi.panel(g, 6, 26, LIST_W + 8, Math.max(40, height - 60), CyberUi.CYAN);
        CyberUi.panel(g, rightX - 4, 26, width - rightX - 2, Math.max(40, height - 60), CyberUi.MAGENTA);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); // draws the backdrop and panels, then the buttons
        BodyState body = ClientBodyState.get();
        CyberUi.title(g, font, title, width - font.width(title) - 20, 6, CyberUi.MAGENTA);
        double capacity = Body.capacity(body);
        double margin = Body.overloadMargin(body);
        int load = Body.totalLoad(body);
        String loadText = Component.translatable("cyberimplants.rig.load", load, Math.round(capacity),
                Math.round(capacity * (1 + margin))).getString();
        boolean strained = Body.strained(body);
        g.drawString(font, loadText + "  " + Component.translatable(strained ? "cyberimplants.load.strained" : "cyberimplants.load.stable").getString(),
                10, 8, strained ? CyberUi.AMBER : CyberUi.GREEN, true);
        double fraction = capacity <= 0 ? 0 : load / (capacity * (1 + margin));
        CyberUi.meter(g, 10, 19, LIST_W, 3, fraction, strained ? CyberUi.AMBER : CyberUi.GREEN);
        g.drawString(font, Component.translatable("cyberimplants.rig.inventory"), 10 + LIST_W + 12, 18, CyberUi.TEXT_DIM, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
