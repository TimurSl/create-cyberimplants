package dev.cyberimplants.implant.limbs;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.implant.Arms;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Cybernetic Arm frame: the main-hand frame's empty-hand harvest level (Layer 2 row: none, stone, iron, diamond,
 * netherite) and the Tier V Haptic precision (Layer 5), a toggle on the Implant Wheel that makes the empty hand mine
 * with silk touch.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class CyberArm {
    /** The haptic precision toggle; the wheel and decks reach it once the frame's tier has the unlock. */
    public static final ImplantAction HAPTIC = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            boolean on = !Toggles.isOn(player, ImplantIds.CYBERNETIC_ARM);
            Toggles.set(player, ImplantIds.CYBERNETIC_ARM, on);
            ImplantFx.sound(player, on ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.AMETHYST_BLOCK_HIT, 0.8f, on ? 1.6f : 0.9f);
            player.displayClientMessage(Component.translatable(on ? "cyberimplants.arm.haptic_on" : "cyberimplants.arm.haptic_off"), true);
            return UseResult.OK;
        }

        @Override
        public boolean isActive(ServerPlayer player) {
            return Toggles.isOn(player, ImplantIds.CYBERNETIC_ARM);
        }

        @Override
        public boolean stop(ServerPlayer player) {
            if (!isActive(player)) {
                return false;
            }
            Toggles.set(player, ImplantIds.CYBERNETIC_ARM, false);
            return true;
        }
    };

    /** Tier of the frame in the main-hand arm, 0 without one. */
    public static int mainFrameTier(Player player) {
        InstalledImplant frame = BodyState.get(player).installed().get(new SlotId(Arms.frameRegion(Arms.mainSide(player)), 0));
        return frame == null || !frame.type().equals(ImplantIds.CYBERNETIC_ARM) ? 0 : frame.tier();
    }

    /** Whether an empty hand with this harvest level (1 stone ... 4 netherite) is the right tool for the block. */
    public static boolean harvests(int level, BlockState state) {
        if (level <= 0) {
            return false;
        }
        if (state.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) {
            return false;
        }
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return level >= 3;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return level >= 2;
        }
        return true;
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        Player player = event.getEntity();
        if (event.canHarvest() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        int tier = mainFrameTier(player);
        if (tier > 0 && harvests((int) Stats.of(ImplantIds.CYBERNETIC_ARM, tier).value("harvest_level", 0), event.getTargetBlock())) {
            event.setCanHarvest(true);
        }
    }

    /** Haptic precision: the drops of an empty-hand break are rolled again with a silk-touch tool. */
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || !event.getTool().isEmpty()
                || !Toggles.isOn(player, ImplantIds.CYBERNETIC_ARM)
                || !Stats.unlocked(player, ImplantIds.CYBERNETIC_ARM, "haptic_precision")) {
            return;
        }
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack silk = new ItemStack(Items.NETHERITE_PICKAXE);
        silk.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        event.getDrops().clear();
        for (ItemStack stack : Block.getDrops(event.getState(), level, pos, event.getBlockEntity(), player, silk)) {
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
        }
        event.setDroppedExperience(0);
    }

    private CyberArm() {
    }
}
