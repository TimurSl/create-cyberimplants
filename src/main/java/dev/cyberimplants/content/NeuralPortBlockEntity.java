package dev.cyberimplants.content;

import dev.cyberimplants.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Remembers who placed a Neural Port and the name it is bound under. */
public class NeuralPortBlockEntity extends BlockEntity {
    private UUID owner;
    private String portName = "";

    public NeuralPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEURAL_PORT.get(), pos, state);
    }

    public UUID owner() {
        return owner;
    }

    public String portName() {
        return portName;
    }

    public void setOwner(UUID owner, String name) {
        this.owner = owner;
        this.portName = name;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("PortName", portName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        portName = tag.getString("PortName");
    }
}
