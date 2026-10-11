package io.papermc.paper.antixray;

import io.nome.bridge.NomeNativeBridge;
import java.lang.foreign.MemorySegment;

public class ChunkPacketBlockController {

    public static final ChunkPacketBlockController NO_OPERATION_INSTANCE = new ChunkPacketBlockController();

    public boolean shouldObfuscate() {
        return false;
    }

    public void updateNearbyBlocks(int x, int y, int z) {}
}
