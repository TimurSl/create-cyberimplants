package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CyberImplants.MOD_ID);

    /** Persistent per-player cybernetic state. Kept on death; synced to the owning client explicitly. */
    public static final Supplier<AttachmentType<BodyState>> BODY_STATE = ATTACHMENT_TYPES.register("body_state",
            () -> AttachmentType.builder(() -> BodyState.EMPTY)
                    .serialize(BodyState.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }
}
