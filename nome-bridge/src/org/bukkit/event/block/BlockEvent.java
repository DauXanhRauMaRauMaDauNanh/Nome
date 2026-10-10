package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.event.Event;

public abstract class BlockEvent extends Event {
    protected final Block block;

    public BlockEvent(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
