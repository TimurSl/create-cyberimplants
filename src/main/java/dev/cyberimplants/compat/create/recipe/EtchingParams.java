package dev.cyberimplants.compat.create.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Function;

/** Create's processing parameters plus {@code min_rpm}: the Precision Etcher idles below that speed. */
public class EtchingParams extends ProcessingRecipeParams {
    public static final MapCodec<EtchingParams> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            codec(EtchingParams::new).forGetter(Function.identity()),
            Codec.INT.optionalFieldOf("min_rpm", 0).forGetter(EtchingParams::minRpm)
    ).apply(i, (params, minRpm) -> {
        params.minRpm = minRpm;
        return params;
    }));
    public static final StreamCodec<RegistryFriendlyByteBuf, EtchingParams> STREAM_CODEC = streamCodec(EtchingParams::new);

    protected int minRpm;

    public EtchingParams() {
    }

    public int minRpm() {
        return minRpm;
    }

    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        ByteBufCodecs.VAR_INT.encode(buffer, minRpm);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        super.decode(buffer);
        minRpm = ByteBufCodecs.VAR_INT.decode(buffer);
    }
}
