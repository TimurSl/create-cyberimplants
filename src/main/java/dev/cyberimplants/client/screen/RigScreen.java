package dev.cyberimplants.client.screen;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyOps;
import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.client.ClientBodyState;
import dev.cyberimplants.client.ClientRig;
import dev.cyberimplants.client.CyberUi;
import dev.cyberimplants.client.CyberUi.CyberButton;
import dev.cyberimplants.client.ui.Anim;
import dev.cyberimplants.client.ui.BootSequence;
import dev.cyberimplants.client.ui.Draw;
import dev.cyberimplants.client.ui.EffectPreview;
import dev.cyberimplants.client.ui.SkeletonView;
import dev.cyberimplants.client.ui.StatFormat;
import dev.cyberimplants.client.ui.UiSounds;
import dev.cyberimplants.compat.create.block.RigBlockEntity;
import dev.cyberimplants.compat.create.block.RigView;
import dev.cyberimplants.config.Layers;
import dev.cyberimplants.data.ImplantStats;
import dev.cyberimplants.data.ImplantStatsRegistry;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantCategory;
import dev.cyberimplants.implant.ImplantItem;
import dev.cyberimplants.implant.ImplantType;
import dev.cyberimplants.network.RigActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The Implantation Rig screen. Left: the implants in your inventory. Centre: a rotating hologram of your skeleton with a
 * node on every body slot. Right: everything about the implant in focus (an animated preview of its effect, its stats
 * against what you have now or the next tier, its features and what the operation needs). Pick an implant and the slots
 * it fits light up; click one to start the operation. Click an installed implant to inspect or extract it.
 *
 * <p>While an operation runs the Rig straps you in and this screen turns into its monitor: progress, stages and the
 * Rig's telemetry, then a banner with the outcome. Every check is repeated by the server; the client only checks first to
 * explain a refusal before sending anything.
 */
public class RigScreen extends Screen {
    private static final int HEADER = 26;
    private static final int FOOTER = 28;
    private static final int ROW = 22;
    private static final int PREVIEW_H = 46;
    private static final float CONFIRM_SECONDS = 3f;
    private static final float BANNER_SECONDS = 2.8f;
    private static final int BONE = 0xFFCFE8FF;

    private record Entry(int slot, ItemStack stack, ResourceLocation type, int tier, boolean fits) {
    }

    /** What the details panel shows. */
    private record Focus(ResourceLocation type, int tier, SlotId slot, boolean installed) {
    }

    /** When the screen last closed: reopening soon after plays a short boot instead of the whole log. */
    private static float lastClosed = -1000;

    private final BlockPos rig;
    private final SkeletonView skeleton = new SkeletonView();
    private final BootSequence boot;
    private final float openedAt = Anim.now();

    private final List<Entry> entries = new ArrayList<>();
    private List<SkeletonView.Node> nodes = List.of();
    private BodyState lastBody;
    private String lastInventory = "";

    private int selectedInv = -1;
    private SlotId selectedSlot;
    private SlotId hoveredSlot;
    private int hoveredRow = -1;
    private float confirmUntil;
    private int listScroll;
    private int detailScroll;
    private int detailHeight;

    private Component status = Component.empty();
    private int statusColor = CyberUi.TEXT_DIM;
    private float statusAt;

    private RigView.Operation lastOp;
    private RigView.Operation pendingOutcome;
    private float pendingSince;
    private Component banner;
    private int bannerColor;
    private float bannerAt = -100;

    private boolean pressedCenter;
    private boolean dragged;
    private double pressX;
    private long lastRowClick;
    private int lastRowClicked = -1;

    private CyberButton primary;

    // layout, recomputed in init()
    private int leftW;
    private int rightW;
    private int centerX0;
    private int centerX1;
    private int bodyTop;
    private int bodyBottom;

    public RigScreen(BlockPos rig) {
        super(Component.translatable("cyberimplants.rig.title"));
        this.rig = rig;
        this.boot = new BootSequence(List.of(
                Component.translatable("cyberimplants.rig.boot.link", rig.getX() + " " + rig.getY() + " " + rig.getZ()).getString(),
                Component.translatable("cyberimplants.rig.boot.handshake").getString(),
                Component.translatable("cyberimplants.rig.boot.scan").getString(),
                Component.translatable("cyberimplants.rig.boot.map").getString(),
                Component.translatable("cyberimplants.rig.boot.ready").getString()), Anim.now() - lastClosed < 60 ? 0.35f : 1.1f);
        UiSounds.boot();
    }

    // ---------------------------------------------------------------- setup

    @Override
    protected void init() {
        leftW = Mth.clamp(width / 4, 112, 150);
        rightW = Mth.clamp((int) (width * 0.3f), 132, 176);
        centerX0 = leftW + 12;
        centerX1 = width - rightW - 12;
        bodyTop = HEADER + 6;
        bodyBottom = height - FOOTER - 6;
        refresh();
        int bx = width - 6 - 64;
        addRenderableWidget(CyberButton.of(bx, height - FOOTER + 5, 64, 18, Component.translatable("gui.done"), b -> onClose(), CyberUi.MAGENTA));
        primary = CyberButton.of(bx - 4 - 116, height - FOOTER + 5, 116, 18, Component.empty(), b -> primaryAction(), CyberUi.GREEN);
        addRenderableWidget(primary);
        updatePrimary();
    }

    /** Re-reads the inventory and the body. */
    private void refresh() {
        lastBody = ClientBodyState.get();
        lastInventory = inventorySignature();
        entries.clear();
        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof ImplantItem item) {
                int tier = ImplantItem.tierOf(stack);
                boolean fits = !BodyOps.candidateSlots(lastBody, item.typeId(), tier).isEmpty();
                entries.add(new Entry(slot, stack, item.typeId(), tier, fits));
            }
        }
        // Fitting implants first, then by category and name order of the catalog.
        entries.sort(Comparator.comparing((Entry e) -> !e.fits()).thenComparingInt(e -> catalogIndex(e.type())).thenComparingInt(Entry::tier));
        if (selectedInv >= 0 && entries.stream().noneMatch(e -> e.slot() == selectedInv)) {
            selectedInv = -1;
        }
        if (selectedSlot != null && selectedSlot.index() >= BodyOps.effectiveSlots(lastBody, selectedSlot.region())) {
            selectedSlot = null;
        }
        listScroll = Mth.clamp(listScroll, 0, maxListScroll());
    }

    private static int catalogIndex(ResourceLocation type) {
        for (int i = 0; i < ImplantCatalog.ALL.size(); i++) {
            if (ImplantCatalog.ALL.get(i).id().equals(type)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
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

    private List<SlotId> usableSlots(BodyState body) {
        List<SlotId> out = new ArrayList<>();
        for (BodyRegion region : BodyRegion.values()) {
            for (int i = 0; i < BodyOps.effectiveSlots(body, region); i++) {
                out.add(new SlotId(region, i));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- state

    private Entry selectedEntry() {
        for (Entry e : entries) {
            if (e.slot() == selectedInv) {
                return e;
            }
        }
        return null;
    }

    private RigView view() {
        return ClientRig.view(rig);
    }

    /** The operation on this Rig if it is ours. */
    private RigView.Operation myOperation() {
        RigView.Operation op = view().operation();
        return op != null && minecraft.player != null && op.patient().equals(minecraft.player.getUUID()) ? op : null;
    }

    private void setStatus(Component text, int color) {
        status = text;
        statusColor = color;
        statusAt = Anim.now();
    }

    /** Why the Rig itself cannot run an operation on an implant of this tier right now, or null. */
    private Component rigProblem(int tier) {
        RigView v = view();
        if (v.operation() != null) {
            return Component.translatable("cyberimplants.rig.busy");
        }
        if (v.locked()) {
            return Component.translatable("cyberimplants.rig.locked");
        }
        if (!v.powered()) {
            return Component.translatable("cyberimplants.rig.no_power");
        }
        if (Layers.atLeast(2)) {
            if (v.rpm() < RigBlockEntity.MIN_RPM[tier - 1]) {
                return Component.translatable("cyberimplants.rig.low_rpm", RigBlockEntity.MIN_RPM[tier - 1]);
            }
            if (v.gel() < RigBlockEntity.GEL_PER_OPERATION || v.neuralGel() != RigBlockEntity.needsNeuralGel(tier)) {
                return Component.translatable("cyberimplants.rig.no_gel", RigBlockEntity.GEL_PER_OPERATION, gelName(tier));
            }
        }
        return null;
    }

    private static Component gelName(int tier) {
        return Component.translatable(RigBlockEntity.needsNeuralGel(tier) ? "fluid_type.cyberimplants.neural_gel" : "fluid_type.cyberimplants.bio_interface_gel");
    }

    @Override
    public void tick() {
        BodyState body = ClientBodyState.get();
        if (body != lastBody || !inventorySignature().equals(lastInventory)) {
            refresh();
            updatePrimary();
        }
        RigView.Operation op = myOperation();
        if (op != null && lastOp == null) {
            pendingOutcome = null;
            banner = null;
        }
        if (op == null && lastOp != null) {
            pendingOutcome = lastOp; // decide once the body sync arrives
            pendingSince = Anim.now();
        }
        lastOp = op;
        RigView.Operation outcome = pendingOutcome;
        if (outcome != null) {
            InstalledImplant now = body.installed().get(outcome.slot());
            boolean done = outcome.install() ? now != null && now.type().equals(outcome.type()) : now == null;
            if (done) {
                showBanner(Component.translatable(outcome.install() ? "cyberimplants.rig.outcome.installed" : "cyberimplants.rig.outcome.removed"), CyberUi.GREEN);
                UiSounds.success();
                pendingOutcome = null;
                selectedInv = -1;
                selectedSlot = outcome.install() ? outcome.slot() : null;
            } else if (Anim.now() - pendingSince > 1.5f) {
                showBanner(Component.translatable("cyberimplants.rig.outcome.aborted"), CyberUi.RED);
                UiSounds.error();
                pendingOutcome = null;
            }
        }
        updatePrimary();
    }

    private void showBanner(Component text, int color) {
        banner = text;
        bannerColor = color;
        bannerAt = Anim.now();
    }

    /** The primary button: Install the selected implant, Extract the selected installed one (with a confirm), or a hint. */
    private void updatePrimary() {
        if (primary == null) {
            return;
        }
        Entry entry = selectedEntry();
        InstalledImplant installed = selectedSlot == null ? null : lastBody.installed().get(selectedSlot);
        if (myOperation() != null) {
            primary.setMessage(Component.translatable("cyberimplants.rig.action.busy"));
            primary.setAccent(CyberUi.MAGENTA);
            primary.active = false;
        } else if (entry != null) {
            primary.setMessage(Component.translatable("cyberimplants.rig.action.install"));
            primary.setAccent(CyberUi.GREEN);
            primary.active = entry.fits();
        } else if (installed != null) {
            boolean confirming = Anim.now() < confirmUntil;
            primary.setMessage(Component.translatable(confirming ? "cyberimplants.rig.action.confirm" : "cyberimplants.rig.action.extract"));
            primary.setAccent(confirming ? CyberUi.AMBER : CyberUi.RED);
            primary.active = true;
        } else {
            primary.setMessage(Component.translatable("cyberimplants.rig.action.select"));
            primary.setAccent(CyberUi.CYAN);
            primary.active = false;
        }
    }

    private void primaryAction() {
        Entry entry = selectedEntry();
        if (entry != null) {
            SlotId target = selectedSlot != null && BodyOps.canInstall(lastBody, entry.type(), entry.tier(), selectedSlot).ok() ? selectedSlot : null;
            if (target == null) {
                List<SlotId> candidates = BodyOps.candidateSlots(lastBody, entry.type(), entry.tier());
                target = candidates.isEmpty() ? null : candidates.get(0);
            }
            if (target != null) {
                install(entry, target);
            }
            return;
        }
        if (selectedSlot != null && lastBody.installed().containsKey(selectedSlot)) {
            if (Anim.now() < confirmUntil) {
                confirmUntil = 0;
                extract(selectedSlot);
            } else {
                confirmUntil = Anim.now() + CONFIRM_SECONDS;
                UiSounds.select();
            }
            updatePrimary();
        }
    }

    private void install(Entry entry, SlotId slot) {
        BodyOps.Result r = BodyOps.canInstall(lastBody, entry.type(), entry.tier(), slot);
        Component problem = r.ok() ? rigProblem(entry.tier()) : Component.translatable(r.translationKey());
        if (problem != null) {
            setStatus(problem, CyberUi.RED);
            UiSounds.error();
            return;
        }
        PacketDistributor.sendToServer(new RigActionPayload(rig, true, slot.region().ordinal(), slot.index(), entry.slot()));
        selectedSlot = slot;
        setStatus(Component.translatable("cyberimplants.rig.status.starting"), CyberUi.CYAN);
        UiSounds.select();
    }

    private void extract(SlotId slot) {
        InstalledImplant installed = lastBody.installed().get(slot);
        BodyOps.Result r = BodyOps.canRemove(lastBody, slot);
        Component problem = !r.ok() ? Component.translatable(r.translationKey()) : installed == null ? null : rigProblem(installed.tier());
        if (problem != null) {
            setStatus(problem, CyberUi.RED);
            UiSounds.error();
            return;
        }
        PacketDistributor.sendToServer(new RigActionPayload(rig, false, slot.region().ordinal(), slot.index(), -1));
        setStatus(Component.translatable("cyberimplants.rig.status.starting"), CyberUi.CYAN);
        UiSounds.select();
    }

    // ---------------------------------------------------------------- input

    private int listTop() {
        return HEADER + 20;
    }

    private int listBottom() {
        return height - FOOTER - 4;
    }

    private int maxListScroll() {
        return Math.max(0, entries.size() * ROW - (listBottom() - listTop()));
    }

    private int rowAt(double mx, double my) {
        if (mx < 8 || mx > 6 + leftW || my < listTop() || my >= listBottom()) {
            return -1;
        }
        int i = (int) ((my - listTop() + listScroll) / ROW);
        return i >= 0 && i < entries.size() ? i : -1;
    }

    private SlotId nodeAt(double mx, double my) {
        SkeletonView.Node best = null;
        double bestDist = 8 * 8;
        for (SkeletonView.Node n : nodes) {
            double d = (n.x() - mx) * (n.x() - mx) + (n.y() - my) * (n.y() - my);
            // Prefer near nodes when two overlap.
            d -= n.depth() * 4;
            if (d < bestDist) {
                bestDist = d;
                best = n;
            }
        }
        return best == null ? null : best.slot();
    }

    private boolean inCenter(double mx, double my) {
        return mx >= centerX0 && mx <= centerX1 && my >= bodyTop && my <= bodyBottom;
    }

    private boolean inDetails(double mx, double my) {
        return mx >= width - rightW - 6 && my >= HEADER + 4 && my <= height - FOOTER - 4;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!boot.done()) {
            boot.skip();
            return true;
        }
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        boolean operating = myOperation() != null;
        int row = rowAt(mx, my);
        if (row >= 0 && !operating) {
            Entry e = entries.get(row);
            long now = net.minecraft.Util.getMillis();
            boolean doubleClick = row == lastRowClicked && now - lastRowClick < 300;
            lastRowClick = now;
            lastRowClicked = row;
            if (doubleClick) {
                selectedInv = e.slot();
                primaryAction();
            } else {
                selectedInv = selectedInv == e.slot() ? -1 : e.slot();
                if (selectedInv >= 0 && selectedSlot != null && lastBody.installed().containsKey(selectedSlot)) {
                    selectedSlot = null;
                }
                detailScroll = 0;
                UiSounds.select();
            }
            updatePrimary();
            return true;
        }
        if (inCenter(mx, my)) {
            SlotId slot = nodeAt(mx, my);
            if (slot != null && !operating) {
                Entry entry = selectedEntry();
                boolean occupied = lastBody.installed().containsKey(slot);
                if (entry != null && !occupied) {
                    install(entry, slot);
                } else {
                    selectedSlot = slot.equals(selectedSlot) ? null : slot;
                    if (occupied) {
                        selectedInv = -1;
                    }
                    confirmUntil = 0;
                    detailScroll = 0;
                    UiSounds.select();
                }
                updatePrimary();
                return true;
            }
            pressedCenter = true;
            dragged = false;
            pressX = mx;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (pressedCenter && button == 0) {
            if (Math.abs(mx - pressX) > 2) {
                dragged = true;
            }
            skeleton.drag((float) dx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (pressedCenter && button == 0) {
            pressedCenter = false;
            if (!dragged) {
                // A plain click on empty space: clear the node selection and let the hologram sway again.
                selectedSlot = null;
                skeleton.resetTurn();
                updatePrimary();
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (mx <= 6 + leftW) {
            listScroll = Mth.clamp(listScroll - (int) (scrollY * ROW), 0, maxListScroll());
            return true;
        }
        if (inDetails(mx, my)) {
            int visible = height - FOOTER - 8 - (HEADER + 8);
            detailScroll = Mth.clamp(detailScroll - (int) (scrollY * 12), 0, Math.max(0, detailHeight - visible));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        lastClosed = Anim.now();
        super.removed();
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float time = Anim.now();
        CyberUi.backdrop(g, width, height, minecraft.player == null ? 0 : minecraft.player.tickCount + partialTick);
        BodyState body = ClientBodyState.get();
        RigView view = view();
        RigView.Operation op = myOperation();
        float intro = Anim.easeOutCubic(Anim.progress(openedAt + 0.6f, 0.5f));

        hoveredRow = boot.done() ? rowAt(mouseX, mouseY) : -1;
        hoveredSlot = boot.done() && inCenter(mouseX, mouseY) && !pressedCenter ? nodeAt(mouseX, mouseY) : null;
        if (hoveredSlot != null && !hoveredSlot.equals(lastHoveredSlot)) {
            UiSounds.hover();
        }
        lastHoveredSlot = hoveredSlot;

        header(g, body, view, op, time);
        g.pose().pushPose();
        g.pose().translate((1 - intro) * -24, 0, 0);
        inventoryPanel(g, op != null);
        g.pose().popPose();
        bodyHologram(g, body, op, time, mouseX, mouseY, partialTick);
        g.pose().pushPose();
        g.pose().translate((1 - intro) * 24, 0, 0);
        detailsPanel(g, body, view, op, partialTick);
        g.pose().popPose();
        footer(g);
    }

    private SlotId lastHoveredSlot;

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (hoveredSlot != null && boot.done()) {
            nodeLabel(g, hoveredSlot);
        }
        bannerOverlay(g);
        if (!boot.done()) {
            boot.render(g, font, 0, 0, width, height, CyberUi.CYAN);
        }
    }

    // -------------------------------- header

    private void header(GuiGraphics g, BodyState body, RigView view, RigView.Operation op, float time) {
        g.fill(0, 0, width, HEADER, 0xC0060912);
        g.fill(0, HEADER, width, HEADER + 1, Anim.fade(CyberUi.CYAN, 0.5f));
        String title = Anim.typed(this.title.getString().toUpperCase(java.util.Locale.ROOT), openedAt + 0.2f, 40);
        g.drawString(font, Component.literal(title).withStyle(ChatFormatting.BOLD), 8, 9, CyberUi.CYAN, true);

        int x = 8 + font.width(this.title.copy().withStyle(ChatFormatting.BOLD).getString().toUpperCase(java.util.Locale.ROOT)) + 14;
        int rpmColor = !view.powered() ? CyberUi.RED : CyberUi.GREEN;
        Component rpm = view.powered() ? Component.translatable("cyberimplants.rig.telemetry.rpm", Math.round(view.rpm()))
                : Component.translatable("cyberimplants.rig.telemetry.offline");
        x += CyberUi.chip(g, font, rpm, x, 8, rpmColor) + 4;
        Component gel = view.gel() <= 0 ? Component.translatable("cyberimplants.rig.telemetry.no_gel")
                : Component.translatable("cyberimplants.rig.telemetry.gel", view.gel(), gelName(view.neuralGel() ? 4 : 1));
        int gelColor = view.gel() <= 0 ? CyberUi.AMBER : view.neuralGel() ? 0xFFB45CFF : 0xFF6FE0A8;
        if (x + font.width(gel) + 8 < width - 190) {
            x += CyberUi.chip(g, font, gel, x, 8, gelColor) + 4;
        }
        if (view.locked()) {
            x += CyberUi.chip(g, font, Component.translatable("cyberimplants.rig.telemetry.locked"), x, 8, CyberUi.RED) + 4;
        }
        if (ClientRig.restrained() && x + 60 < width - 190) {
            int c = Anim.mix(CyberUi.MAGENTA, 0xFFFFFFFF, Anim.pulse(0.8f) * 0.4f);
            CyberUi.chip(g, font, Component.translatable("cyberimplants.rig.telemetry.restrained"), x, 8, c);
        }

        // Neural Load with a ghost of what the selected implant would add.
        double capacity = Body.capacity(body);
        double limit = capacity * (1 + Body.overloadMargin(body));
        int load = Body.totalLoad(body);
        Entry entry = selectedEntry();
        int extra = entry == null ? 0 : Body.loadOf(entry.type(), entry.tier());
        int meterW = 110;
        int mx = width - meterW - 8;
        boolean strained = Body.strained(body);
        Component label = Component.translatable("cyberimplants.rig.load.short", load + (extra > 0 ? "+" + extra : ""),
                Math.round(capacity));
        int labelColor = load + extra > limit ? CyberUi.RED : load + extra > capacity ? CyberUi.AMBER : CyberUi.TEXT;
        g.drawString(font, label, mx - font.width(label) - 6, 9, labelColor, false);
        g.drawString(font, Component.translatable(strained ? "cyberimplants.load.strained" : "cyberimplants.load.stable"),
                mx, 4, strained ? CyberUi.AMBER : CyberUi.GREEN, false);
        double max = Math.max(1, limit);
        CyberUi.meter(g, mx, 14, meterW, 5, load / max, strained ? CyberUi.AMBER : CyberUi.GREEN);
        if (extra > 0) {
            int from = mx + (int) Math.round(meterW * Math.min(1, load / max));
            int to = mx + (int) Math.round(meterW * Math.min(1, (load + extra) / max));
            int ghost = load + extra > limit ? CyberUi.RED : load + extra > capacity ? CyberUi.AMBER : CyberUi.CYAN;
            g.fill(from, 14, Math.max(from + 1, to), 19, Anim.fade(ghost, 0.35f + 0.35f * Anim.pulse(1.2f)));
        }
        // The capacity mark (beyond it the body is Strained).
        if (limit > 0) {
            int cap = mx + (int) Math.round(meterW * capacity / max);
            g.fill(cap, 12, cap + 1, 21, 0xFFFFFFFF);
        }
    }

    // -------------------------------- inventory

    private void inventoryPanel(GuiGraphics g, boolean operating) {
        int x = 6;
        int y = HEADER + 4;
        CyberUi.panel(g, x, y, leftW, height - FOOTER - y, CyberUi.CYAN);
        g.drawString(font, Component.translatable("cyberimplants.rig.header.implants", entries.size()), x + 6, y + 5, CyberUi.CYAN, false);
        g.fill(x + 6, y + 14, x + leftW - 6, y + 15, Anim.fade(CyberUi.CYAN, 0.3f));
        int top = listTop();
        int bottom = listBottom();
        if (entries.isEmpty()) {
            int ty = top + 10;
            for (FormattedCharSequence line : font.split(Component.translatable("cyberimplants.rig.inventory_empty"), leftW - 14)) {
                g.drawString(font, line, x + 7, ty, CyberUi.TEXT_DIM, false);
                ty += 10;
            }
            return;
        }
        g.enableScissor(x + 2, top, x + leftW - 2, bottom);
        for (int i = 0; i < entries.size(); i++) {
            int ry = top + i * ROW - listScroll;
            if (ry + ROW < top || ry > bottom) {
                continue;
            }
            Entry e = entries.get(i);
            ImplantType type = ImplantCatalog.get(e.type());
            int color = type == null ? CyberUi.CYAN : CyberUi.categoryColor(type.category());
            boolean selected = e.slot() == selectedInv;
            boolean hovered = i == hoveredRow && !operating;
            int fill = selected ? CyberUi.withAlpha(CyberUi.dim(CyberUi.MAGENTA, 0.3f), 0xE0)
                    : hovered ? CyberUi.PANEL_LIGHT : 0x60101626;
            g.fill(x + 4, ry + 1, x + leftW - 4, ry + ROW - 1, fill);
            g.fill(x + 4, ry + 1, x + 6, ry + ROW - 1, e.fits() ? color : CyberUi.dim(color, 0.4f));
            if (selected) {
                CyberUi.corners(g, x + 4, ry + 1, leftW - 8, ROW - 2, 4, CyberUi.MAGENTA);
            }
            g.renderItem(e.stack(), x + 9, ry + 3);
            int textX = x + 28;
            int textW = leftW - 34;
            String name = font.plainSubstrByWidth(e.stack().getHoverName().getString(), textW);
            g.drawString(font, name, textX, ry + 3, e.fits() ? CyberUi.TEXT : CyberUi.TEXT_DIM, false);
            CyberUi.tierPips(g, textX, ry + 14, e.tier(), color);
            String meta = "L" + Body.loadOf(e.type(), e.tier()) + (e.stack().getCount() > 1 ? "  x" + e.stack().getCount() : "");
            if (!e.fits()) {
                meta = Component.translatable("cyberimplants.rig.no_fit").getString();
            }
            g.drawString(font, meta, x + leftW - 8 - font.width(meta), ry + 12, e.fits() ? CyberUi.AMBER : CyberUi.dim(CyberUi.RED, 0.8f), false);
        }
        g.disableScissor();
        if (maxListScroll() > 0) {
            int barH = bottom - top;
            int thumb = Math.max(10, barH * barH / (entries.size() * ROW));
            int ty = top + (barH - thumb) * listScroll / maxListScroll();
            g.fill(x + leftW - 3, ty, x + leftW - 2, ty + thumb, CyberUi.CYAN);
        }
        if (operating) {
            g.fill(x + 1, top, x + leftW - 1, bottom, 0x90060912);
        }
    }

    // -------------------------------- hologram

    private void bodyHologram(GuiGraphics g, BodyState body, RigView.Operation op, float time, int mouseX, int mouseY, float partialTick) {
        int cx = (centerX0 + centerX1) / 2;
        int h = bodyBottom - bodyTop - 16;
        float holoH = Math.min(h, (centerX1 - centerX0) * 1.6f);
        skeleton.layout(cx, bodyTop + 6 + h / 2f, holoH);
        skeleton.update(time);

        float reveal = Anim.easeInOutCubic(Anim.progress(openedAt + 0.15f, 1.1f));
        float scan = -1;
        if (op != null) {
            scan = Anim.enabled() ? 0.5f + 0.5f * Mth.sin(time * 2.2f) : 0.5f;
        } else if (reveal < 1) {
            scan = reveal;
        }
        int accent = op != null ? Anim.mix(CyberUi.CYAN, CyberUi.MAGENTA, op.progress(partialTick)) : CyberUi.CYAN;
        // Corner brackets frame the hologram area.
        CyberUi.corners(g, centerX0, bodyTop, centerX1 - centerX0, bodyBottom - bodyTop, 10, Anim.fade(accent, 0.6f));
        skeleton.render(g, accent, BONE, reveal, scan, time);

        Entry entry = selectedEntry();
        List<SlotId> slots = usableSlots(body);
        List<SlotId> candidates = entry == null ? List.of() : BodyOps.candidateSlots(body, entry.type(), entry.tier());
        nodes = new ArrayList<>(skeleton.nodes(slots));
        nodes.sort(Comparator.comparingDouble(SkeletonView.Node::depth));

        Draw.Batch b = Draw.batch(g);
        float pulse = Anim.pulse(1.1f);
        float spin = time * 1.5f;
        float r = Math.max(3f, skeleton.scale() * 0.045f);
        for (SkeletonView.Node n : nodes) {
            if (reveal < 1 && SkeletonView.anchor(n.slot()).y > reveal * 2.05f) {
                continue; // not drawn in yet
            }
            InstalledImplant installed = body.installed().get(n.slot());
            boolean candidate = candidates.contains(n.slot());
            boolean hovered = n.slot().equals(hoveredSlot);
            boolean selected = n.slot().equals(selectedSlot);
            boolean target = op != null && op.slot().equals(n.slot());
            float depthAlpha = 0.55f + 0.45f * (n.depth() + 1) / 2;
            float nr = r * (hovered ? 1.35f : 1f) * (0.85f + 0.15f * (n.depth() + 1) / 2);
            if (target) {
                int c = Anim.mix(CyberUi.MAGENTA, 0xFFFFFFFF, pulse * 0.5f);
                b.glow(n.x(), n.y(), nr * 3.5f, Anim.fade(CyberUi.MAGENTA, 0.8f));
                b.arc(n.x(), n.y(), nr * 2.2f, 1.5f, spin * 2, Mth.PI * 2 * op.progress(partialTick), c);
            }
            if (installed != null) {
                ImplantType type = ImplantCatalog.get(installed.type());
                int c = type == null ? CyberUi.CYAN : CyberUi.categoryColor(type.category());
                b.glow(n.x(), n.y(), nr * 2.2f, Anim.fade(c, 0.45f * depthAlpha));
                b.filledPolygon(n.x(), n.y(), nr, 6, Mth.PI / 6, Anim.fade(c, depthAlpha));
                b.polygon(n.x(), n.y(), nr, 6, Mth.PI / 6, 1f, Anim.fade(0xFFFFFFFF, 0.7f * depthAlpha));
            } else if (candidate) {
                b.disc(n.x(), n.y(), nr * (1.6f + 0.5f * pulse), Anim.fade(CyberUi.GREEN, 0.35f), Anim.fade(CyberUi.GREEN, 0f));
                b.polygon(n.x(), n.y(), nr * 1.1f, 6, Mth.PI / 6 + spin * 0.3f, 1.4f, CyberUi.GREEN);
                b.filledPolygon(n.x(), n.y(), nr * 0.45f, 6, Mth.PI / 6, Anim.fade(CyberUi.GREEN, 0.7f + 0.3f * pulse));
            } else {
                int c = entry != null ? 0xFF3A4660 : CyberUi.CYAN;
                b.filledPolygon(n.x(), n.y(), nr * 0.9f, 6, Mth.PI / 6, Anim.fade(0xFF0A0E18, 0.8f * depthAlpha));
                b.polygon(n.x(), n.y(), nr * 0.9f, 6, Mth.PI / 6, 1f, Anim.fade(c, 0.75f * depthAlpha));
            }
            if (selected) {
                for (int k = 0; k < 4; k++) {
                    float a = spin + k * Mth.PI / 2;
                    b.arc(n.x(), n.y(), nr * 2f, 1.3f, a, Mth.PI / 4, CyberUi.MAGENTA);
                }
            }
            if (hovered) {
                b.ring(n.x(), n.y(), nr * 1.7f, 1f, Anim.fade(0xFFFFFFFF, 0.8f));
            }
        }
        b.end();

        // Legend and hint under the hologram.
        String hint = Component.translatable(op != null ? "cyberimplants.rig.hint.operating"
                : entry != null ? "cyberimplants.rig.hint.place" : "cyberimplants.rig.hint.select").getString();
        g.drawCenteredString(font, font.plainSubstrByWidth(hint, centerX1 - centerX0 - 8), cx, bodyBottom - 10, CyberUi.TEXT_DIM);

        if (op != null) {
            operationMonitor(g, op, cx, partialTick, time);
        }
    }

    private void nodeLabel(GuiGraphics g, SlotId slot) {
        SkeletonView.Node node = null;
        for (SkeletonView.Node n : nodes) {
            if (n.slot().equals(slot)) {
                node = n;
            }
        }
        if (node == null) {
            return;
        }
        BodyState body = ClientBodyState.get();
        InstalledImplant installed = body.installed().get(slot);
        List<Component> lines = new ArrayList<>();
        lines.add(slotName(slot));
        if (installed != null) {
            lines.add(implantName(installed.type()).copy().append(" ").append(Component.translatable("cyberimplants.tooltip.tier",
                    installed.tier(), Component.translatable("cyberimplants.tier." + installed.tier()))));
            lines.add(Component.translatable("cyberimplants.rig.hint.inspect").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lines.add(Component.translatable("cyberimplants.rig.empty"));
            Entry entry = selectedEntry();
            if (entry != null) {
                BodyOps.Result r = BodyOps.canInstall(body, entry.type(), entry.tier(), slot);
                Component problem = r.ok() ? rigProblem(entry.tier()) : Component.translatable(r.translationKey());
                lines.add(problem == null ? Component.translatable("cyberimplants.rig.hint.click_install").withStyle(ChatFormatting.GREEN)
                        : problem.copy().withStyle(ChatFormatting.RED));
            }
        }
        int accent = installed != null && ImplantCatalog.get(installed.type()) != null
                ? CyberUi.categoryColor(ImplantCatalog.get(installed.type()).category()) : CyberUi.CYAN;
        // A leader line from the node out to the tooltip.
        boolean right = node.x() < (centerX0 + centerX1) / 2f;
        int lx = (int) node.x() + (right ? 26 : -26);
        Draw.batch(g).line(node.x(), node.y(), lx, node.y() - 10, 1f, Anim.fade(accent, 0.9f)).end();
        CyberUi.tooltip(g, font, lines, right ? lx - 12 : lx + 8, (int) node.y() - 10, accent, 150, !right);
    }

    private void operationMonitor(GuiGraphics g, RigView.Operation op, int cx, float partialTick, float time) {
        float p = op.progress(partialTick);
        int boxW = Math.min(centerX1 - centerX0 - 8, 200);
        int bx = cx - boxW / 2;
        int by = bodyTop + 4;
        CyberUi.panel(g, bx, by, boxW, 30, CyberUi.MAGENTA, 0xD00A0E18);
        Component what = Component.translatable(op.install() ? "cyberimplants.rig.op.install" : "cyberimplants.rig.op.remove",
                implantName(op.type()));
        g.drawString(font, font.plainSubstrByWidth(what.getString(), boxW - 50), bx + 6, by + 5, CyberUi.TEXT, false);
        String pct = Math.round(p * 100) + "%";
        g.drawString(font, pct, bx + boxW - 6 - font.width(pct), by + 5, CyberUi.MAGENTA, false);
        int stage = Math.min(4, (int) (p * 5));
        String stageKey = stage == 2 ? (op.install() ? "splice" : "detach") : Integer.toString(stage);
        String stageText = Component.translatable("cyberimplants.rig.stage." + stageKey).getString();
        g.drawString(font, (stage + 1) + "/5  " + stageText, bx + 6, by + 16, CyberUi.dim(CyberUi.CYAN, 0.9f), false);
        String left = Component.translatable("cyberimplants.rig.op.left", op.secondsLeft()).getString();
        g.drawString(font, left, bx + boxW - 6 - font.width(left), by + 16, CyberUi.TEXT_DIM, false);
        CyberUi.meter(g, bx + 4, by + 27, boxW - 8, 2, p, CyberUi.MAGENTA);
    }

    private void bannerOverlay(GuiGraphics g) {
        if (banner == null) {
            return;
        }
        float age = Anim.now() - bannerAt;
        if (age > BANNER_SECONDS) {
            banner = null;
            return;
        }
        float in = Anim.easeOutBack(Mth.clamp(age / 0.35f, 0, 1));
        float out = age > BANNER_SECONDS - 0.5f ? (BANNER_SECONDS - age) / 0.5f : 1f;
        int cx = (centerX0 + centerX1) / 2;
        int cy = (bodyTop + bodyBottom) / 2;
        String text = banner.getString().toUpperCase(java.util.Locale.ROOT);
        int w = font.width(text) * 2 + 24;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 300);
        g.pose().scale(in, in, 1);
        CyberUi.panel(g, -w / 2, -14, w, 28, Anim.fade(bannerColor, out), Anim.fade(0xF00A0E18, out));
        int jitter = bannerColor == CyberUi.RED && Anim.enabled() && (int) (age * 20) % 5 == 0 ? 2 : 0;
        g.pose().scale(2, 2, 1);
        g.drawString(font, text, -font.width(text) / 2 + jitter, -4, Anim.fade(bannerColor, out), true);
        g.pose().popPose();
    }

    // -------------------------------- details

    private Focus focus(BodyState body, RigView.Operation op) {
        if (op != null) {
            return new Focus(op.type(), op.tier(), op.slot(), !op.install());
        }
        if (hoveredRow >= 0) {
            Entry e = entries.get(hoveredRow);
            return new Focus(e.type(), e.tier(), null, false);
        }
        SlotId slot = hoveredSlot != null && body.installed().containsKey(hoveredSlot) ? hoveredSlot : null;
        if (slot == null && selectedSlot != null && body.installed().containsKey(selectedSlot) && selectedEntry() == null) {
            slot = selectedSlot;
        }
        if (slot != null) {
            InstalledImplant i = body.installed().get(slot);
            return new Focus(i.type(), i.tier(), slot, true);
        }
        Entry e = selectedEntry();
        return e == null ? null : new Focus(e.type(), e.tier(), null, false);
    }

    private void detailsPanel(GuiGraphics g, BodyState body, RigView view, RigView.Operation op, float partialTick) {
        int x = width - rightW - 6;
        int y = HEADER + 4;
        int h = height - FOOTER - y;
        Focus f = focus(body, op);
        int accent = f == null || ImplantCatalog.get(f.type()) == null ? CyberUi.MAGENTA : CyberUi.categoryColor(ImplantCatalog.get(f.type()).category());
        CyberUi.panel(g, x, y, rightW, h, accent);
        int innerX = x + 6;
        int innerW = rightW - 12;
        g.enableScissor(x + 2, y + 2, x + rightW - 2, y + h - 2);
        int cy = y + 6 - detailScroll;
        if (f == null) {
            cy = overview(g, body, innerX, cy, innerW);
        } else {
            cy = implantDetails(g, body, view, f, innerX, cy, innerW, accent);
        }
        g.disableScissor();
        detailHeight = cy + detailScroll - (y + 6);
        int visible = h - 8;
        if (detailHeight > visible) {
            int thumb = Math.max(10, visible * visible / detailHeight);
            int ty = y + 4 + (visible - thumb) * detailScroll / Math.max(1, detailHeight - visible);
            g.fill(x + rightW - 3, ty, x + rightW - 2, ty + thumb, accent);
        }
    }

    private int overview(GuiGraphics g, BodyState body, int x, int y, int w) {
        g.drawString(font, Component.translatable("cyberimplants.rig.details.overview").withStyle(ChatFormatting.BOLD), x, y, CyberUi.MAGENTA, false);
        y += 14;
        List<SlotId> slots = usableSlots(body);
        y = row(g, x, y, w, Component.translatable("cyberimplants.rig.details.installed"), body.installed().size() + " / " + slots.size(), CyberUi.TEXT);
        y = row(g, x, y, w, Component.translatable("cyberimplants.rig.details.capacity"), StatFormat.number(Body.capacity(body)), CyberUi.TEXT);
        y = row(g, x, y, w, Component.translatable("cyberimplants.rig.details.battery"), StatFormat.number(Body.maxCharge(body)) + " C", CyberUi.TEXT);
        y = row(g, x, y, w, Component.translatable("cyberimplants.rig.details.recharge"), StatFormat.number(Body.metabolicRate(body)) + " C/s", CyberUi.TEXT);
        y += 6;
        // Installed implants grouped by category, as coloured bars.
        Map<ImplantCategory, Integer> counts = new java.util.EnumMap<>(ImplantCategory.class);
        for (InstalledImplant i : body.installed().values()) {
            ImplantType type = ImplantCatalog.get(i.type());
            if (type != null) {
                counts.merge(type.category(), 1, Integer::sum);
            }
        }
        for (ImplantCategory c : ImplantCategory.values()) {
            int n = counts.getOrDefault(c, 0);
            int color = CyberUi.categoryColor(c);
            g.fill(x, y + 2, x + 3, y + 7, n > 0 ? color : CyberUi.dim(color, 0.3f));
            String name = font.plainSubstrByWidth(Component.translatable("cyberimplants.category." + c.getSerializedName()).getString(), w - 30);
            g.drawString(font, name, x + 7, y, n > 0 ? CyberUi.TEXT : CyberUi.TEXT_DIM, false);
            String count = Integer.toString(n);
            g.drawString(font, count, x + w - font.width(count), y, n > 0 ? color : CyberUi.TEXT_DIM, false);
            y += 10;
        }
        y += 6;
        if (!Body.hasCore(body)) {
            for (FormattedCharSequence line : font.split(Component.translatable("cyberimplants.rig.details.core_first"), w)) {
                g.drawString(font, line, x, y, CyberUi.AMBER, false);
                y += 10;
            }
        }
        return y;
    }

    private int implantDetails(GuiGraphics g, BodyState body, RigView view, Focus f, int x, int y, int w, int accent) {
        ImplantType type = ImplantCatalog.get(f.type());
        int tier = Mth.clamp(f.tier(), 1, 5);
        // Heading: icon, name, tier.
        g.fill(x, y, x + 20, y + 20, CyberUi.withAlpha(CyberUi.dim(accent, 0.25f), 0xD0));
        g.renderOutline(x, y, 20, 20, accent);
        g.renderItem(ImplantItem.stackOf(f.type(), tier), x + 2, y + 2);
        String name = font.plainSubstrByWidth(implantName(f.type()).getString(), w - 26);
        g.drawString(font, Component.literal(name).withStyle(ChatFormatting.BOLD), x + 25, y + 1, CyberUi.TEXT, false);
        CyberUi.tierPips(g, x + 25, y + 13, tier, accent);
        g.drawString(font, Component.translatable("cyberimplants.tier." + tier), x + 48, y + 11, CyberUi.TEXT_DIM, false);
        y += 24;
        if (type != null) {
            int cx = x;
            cx += CyberUi.chip(g, font, Component.translatable("cyberimplants.category." + type.category().getSerializedName()), cx, y, accent) + 3;
            Component activation = Component.translatable("cyberimplants.activation." + type.activation().getSerializedName());
            if (cx + font.width(activation) + 8 <= x + w) {
                CyberUi.chip(g, font, activation, cx, y, CyberUi.TEXT_DIM);
            }
            y += 14;
        }
        if (f.slot() != null) {
            g.drawString(font, slotName(f.slot()), x, y, CyberUi.TEXT_DIM, false);
            y += 11;
        }

        EffectPreview.render(g, x, y, w, PREVIEW_H, f.type(), tier, accent);
        y += PREVIEW_H + 6;

        // Stats against a reference: the installed one of the same type, or for an installed implant its next tier.
        int refTier = 0;
        Component refLabel = null;
        if (f.installed() && tier < 5) {
            refTier = tier + 1;
            refLabel = Component.translatable("cyberimplants.rig.details.next");
        } else if (!f.installed()) {
            int have = body.tierOf(f.type());
            if (have > 0) {
                refTier = have;
                refLabel = Component.translatable("cyberimplants.rig.details.now");
            }
        }
        TierStats stats = Stats.of(f.type(), tier);
        TierStats ref = refTier > 0 ? Stats.of(f.type(), refTier) : null;
        g.drawString(font, Component.translatable("cyberimplants.rig.details.stats").withStyle(ChatFormatting.BOLD), x, y, accent, false);
        if (refLabel != null) {
            String rl = refLabel.getString();
            g.drawString(font, rl, x + w - font.width(rl), y, CyberUi.TEXT_DIM, false);
        }
        y += 11;
        y = statRow(g, x, y, w, Component.translatable("cyberimplants.rig.stat.load"), "load", stats.load(), ref == null ? null : (double) ref.load());
        if (stats.chargeCost() > 0) {
            y = statRow(g, x, y, w, Component.translatable("cyberimplants.rig.stat.charge"), "charge_cost", stats.chargeCost(),
                    ref == null ? null : ref.chargeCost());
        }
        if (stats.cooldownTicks() > 0) {
            y = statRow(g, x, y, w, Component.translatable("cyberimplants.rig.stat.cooldown"), "cooldown_ticks", stats.cooldownTicks(),
                    ref == null ? null : (double) ref.cooldownTicks());
        }
        List<String> keys = new ArrayList<>(stats.values().keySet());
        keys.sort(null);
        for (String key : keys) {
            y = statRow(g, x, y, w, StatFormat.label(key), key, stats.value(key, 0), ref == null ? null : ref.value(key, 0));
        }
        y += 4;

        // Features: the ones this tier has, and the next one coming.
        ImplantStats all = ImplantStatsRegistry.get(f.type()).orElse(null);
        if (all != null) {
            List<String> active = all.activeUnlocks(tier, Layers.maxEnabled());
            String upcoming = null;
            int upcomingTier = 0;
            for (int t = tier + 1; t <= 5 && upcoming == null; t++) {
                for (String u : all.activeUnlocks(t, Layers.maxEnabled())) {
                    if (!active.contains(u)) {
                        upcoming = u;
                        upcomingTier = t;
                        break;
                    }
                }
            }
            if (!active.isEmpty() || upcoming != null) {
                g.drawString(font, Component.translatable("cyberimplants.rig.details.unlocks").withStyle(ChatFormatting.BOLD), x, y, accent, false);
                y += 11;
                for (String u : active) {
                    y = wrapped(g, Component.literal("+ ").append(unlockName(f.type(), u)), x, y, w, CyberUi.GREEN);
                }
                if (upcoming != null) {
                    y = wrapped(g, Component.translatable("cyberimplants.rig.details.next_unlock", upcomingTier).append(": ")
                            .append(unlockName(f.type(), upcoming)), x, y, w, CyberUi.TEXT_DIM);
                }
                y += 4;
            }
        }

        // What the operation needs, checked against the Rig.
        g.drawString(font, Component.translatable("cyberimplants.rig.details.operation").withStyle(ChatFormatting.BOLD), x, y, accent, false);
        y += 11;
        if (!Layers.atLeast(2)) {
            y = wrapped(g, Component.translatable("cyberimplants.rig.details.instant"), x, y, w, CyberUi.TEXT_DIM);
        } else {
            int need = RigBlockEntity.MIN_RPM[tier - 1];
            boolean rpmOk = view.powered() && view.rpm() >= need;
            boolean gelOk = view.gel() >= RigBlockEntity.GEL_PER_OPERATION && view.neuralGel() == RigBlockEntity.needsNeuralGel(tier);
            y = check(g, x, y, w, Component.translatable("cyberimplants.rig.details.time"),
                    StatFormat.number(RigBlockEntity.operationTicks(tier) / 20.0) + " s", true);
            y = check(g, x, y, w, Component.translatable("cyberimplants.rig.details.speed"), need + " RPM", rpmOk);
            y = check(g, x, y, w, gelName(tier), RigBlockEntity.GEL_PER_OPERATION + " mB", gelOk);
        }
        y += 4;
        return wrapped(g, Component.translatable("item." + f.type().getNamespace() + "." + f.type().getPath() + ".desc"), x, y, w, CyberUi.TEXT_DIM);
    }

    private int row(GuiGraphics g, int x, int y, int w, Component label, String value, int color) {
        g.drawString(font, font.plainSubstrByWidth(label.getString(), w - font.width(value) - 6), x, y, CyberUi.TEXT_DIM, false);
        g.drawString(font, value, x + w - font.width(value), y, color, false);
        return y + 10;
    }

    /** One stat: label, value, and when there is a reference an arrow coloured by whether it gets better. */
    private int statRow(GuiGraphics g, int x, int y, int w, Component label, String key, double value, Double ref) {
        String v = StatFormat.value(key, value);
        int valueColor = CyberUi.TEXT;
        String refText = "";
        int refColor = CyberUi.TEXT_DIM;
        if (ref != null && Math.abs(ref - value) > 1e-9) {
            boolean better = (ref > value) == StatFormat.higherIsBetter(key);
            refText = (ref > value ? "▲" : "▼") + StatFormat.value(key, ref);
            refColor = better ? CyberUi.GREEN : CyberUi.RED;
        }
        int refW = refText.isEmpty() ? 0 : font.width(refText) + 4;
        int labelW = w - font.width(v) - refW - 6;
        g.drawString(font, font.plainSubstrByWidth(label.getString(), labelW), x, y, CyberUi.TEXT_DIM, false);
        g.drawString(font, v, x + w - refW - font.width(v), y, valueColor, false);
        if (!refText.isEmpty()) {
            g.drawString(font, refText, x + w - refW + 4, y, refColor, false);
        }
        // A faint rule between rows.
        g.fill(x, y + 9, x + w, y + 10, 0x18FFFFFF);
        return y + 10;
    }

    private int check(GuiGraphics g, int x, int y, int w, Component label, String value, boolean ok) {
        String mark = ok ? "✔ " : "✖ ";
        g.drawString(font, mark, x, y, ok ? CyberUi.GREEN : CyberUi.RED, false);
        int mw = font.width(mark);
        g.drawString(font, font.plainSubstrByWidth(label.getString(), w - mw - font.width(value) - 6), x + mw, y, CyberUi.TEXT_DIM, false);
        g.drawString(font, value, x + w - font.width(value), y, ok ? CyberUi.TEXT : CyberUi.RED, false);
        return y + 10;
    }

    private int wrapped(GuiGraphics g, Component text, int x, int y, int w, int color) {
        for (FormattedCharSequence line : font.split(text, w)) {
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    // -------------------------------- footer

    private void footer(GuiGraphics g) {
        int y = height - FOOTER;
        g.fill(0, y, width, height, 0xC0060912);
        g.fill(0, y, width, y + 1, Anim.fade(CyberUi.CYAN, 0.4f));
        Component text = status;
        int color = statusColor;
        if (text.getString().isEmpty() || Anim.now() - statusAt > 6) {
            Entry entry = selectedEntry();
            text = entry != null && !entry.fits() ? Component.translatable("cyberimplants.rig.hint.no_slot")
                    : Component.translatable("cyberimplants.rig.hint.drag");
            color = CyberUi.TEXT_DIM;
        }
        int maxW = primary == null ? width - 16 : primary.getX() - 16;
        g.drawString(font, font.plainSubstrByWidth(text.getString(), maxW), 8, y + 10, color, false);
    }

    // ---------------------------------------------------------------- names

    private static Component implantName(ResourceLocation type) {
        return Component.translatable("item." + type.getNamespace() + "." + type.getPath());
    }

    private static Component unlockName(ResourceLocation type, String unlock) {
        return Component.translatable("cyberimplants.unlock." + type.getPath() + "." + unlock);
    }

    private static Component slotName(SlotId slot) {
        return Component.translatable("cyberimplants.rig.slot_name",
                Component.translatable("cyberimplants.region." + slot.region().getSerializedName()), slot.index() + 1);
    }
}
