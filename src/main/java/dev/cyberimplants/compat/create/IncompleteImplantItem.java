package dev.cyberimplants.compat.create;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.world.item.Item;

/** The transitional item of the implant upgrade assemblies (sequenced assembly keeps its progress on this item). */
public class IncompleteImplantItem extends SequencedAssemblyItem {
    public IncompleteImplantItem(Item.Properties properties) {
        super(properties);
    }
}
