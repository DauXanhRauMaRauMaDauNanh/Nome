package org.bukkit.event.world;

import org.bukkit.Chunk;

public class ChunkUnloadEvent extends ChunkEvent {
    private boolean isSaveChunk = true;

    public ChunkUnloadEvent(Chunk chunk) {
        super(chunk);
    }

    public boolean isSaveChunk() {
        return isSaveChunk;
    }

    public void setSaveChunk(boolean saveChunk) {
        this.isSaveChunk = saveChunk;
    }
}
