package dev.cyberimplants.implant;

import dev.cyberimplants.CyberImplants;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

/** Idempotent transient attribute modifiers with stable ids (docs/design/08-technical-architecture.md, section 8.5). */
public final class Attrs {
    public static ResourceLocation modId(String name) {
        return CyberImplants.id("mod/" + name);
    }

    /** Adds, updates or removes the modifier so the attribute reflects {@code present}/{@code amount}. */
    public static void set(Player player, Holder<Attribute> attribute, String name, double amount,
                           AttributeModifier.Operation operation, boolean present) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        ResourceLocation id = modId(name);
        AttributeModifier existing = instance.getModifier(id);
        if (!present || amount == 0.0) {
            if (existing != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (existing != null && existing.amount() == amount && existing.operation() == operation) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    public static void multiplied(Player player, Holder<Attribute> attribute, String name, double amount, boolean present) {
        set(player, attribute, name, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, present);
    }

    public static void added(Player player, Holder<Attribute> attribute, String name, double amount, boolean present) {
        set(player, attribute, name, amount, AttributeModifier.Operation.ADD_VALUE, present);
    }

    private Attrs() {
    }
}
