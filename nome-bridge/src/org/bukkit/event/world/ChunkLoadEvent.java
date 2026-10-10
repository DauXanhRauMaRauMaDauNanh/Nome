package org.bukkit.event.world;

import org.bukkit.Chunk;

public class ChunkLoadEvent extends ChunkEvent {
    private final boolean isNewChunk;

    public ChunkLoadEvent(Chunk chunk, boolean isNewChunk) {
        super(chunk);
        this.isNewChunk = isNewChunk;
    }

    public boolean isNewChunk() {
        return isNewChunk;
    }
}
