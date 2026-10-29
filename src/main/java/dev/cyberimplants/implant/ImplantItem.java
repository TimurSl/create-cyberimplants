package dev.cyberimplants.implant;

import dev.cyberimplants.data.Stats;
import dev.cyberimplants.registry.ModDataComponents;
import dev.cyberimplants.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The item form of an implant type. The tier lives in the {@code cyberimplants:tier} component (absent = Tier I), so
 * the item needs no registration-order tricks and every tier shares one item.
 */
public class ImplantItem extends Item {
    private final ResourceLocation typeId;

    public ImplantItem(ResourceLocation typeId, Properties properties) {
        super(properties);
        this.typeId = typeId;
    }

    public ResourceLocation typeId() {
        return typeId;
    }

    public ImplantType type() {
        return ImplantCatalog.get(typeId);
    }

    public static int tierOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TIER.get(), 1);
    }

    public static ItemStack stackOf(ResourceLocation typeId, int tier) {
        Item item = ModItems.itemFor(typeId);
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.TIER.get(), tier);
        return stack;
    }

    /** Tier I carries its component explicitly so upgrade recipes can match "this implant at tier N". */
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.set(ModDataComponents.TIER.get(), 1);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int tier = tierOf(stack);
        ImplantType type = type();
        tooltip.add(Component.translatable("cyberimplants.tooltip.tier", tier,
                Component.translatable("cyberimplants.tier." + tier)).withStyle(ChatFormatting.AQUA));
        if (type != null) {
            tooltip.add(Component.translatable("cyberimplants.tooltip.slot",
                    Component.translatable("cyberimplants.category." + type.category().getSerializedName())).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("cyberimplants.tooltip.activation",
                    Component.translatable("cyberimplants.activation." + type.activation().getSerializedName())).withStyle(ChatFormatting.GRAY));
        }
        int load = Stats.of(typeId, tier).load();
        if (load > 0) {
            tooltip.add(Component.translatable("cyberimplants.tooltip.load", load).withStyle(ChatFormatting.YELLOW));
        }
        String descKey = "item." + typeId.getNamespace() + "." + typeId.getPath() + ".desc";
        tooltip.add(Component.translatable(descKey).withStyle(ChatFormatting.DARK_GRAY));
    }
}
