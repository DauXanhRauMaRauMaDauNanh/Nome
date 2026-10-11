package io.papermc.paper.antixray;

import io.nome.bridge.NomeNativeBridge;
import java.lang.foreign.MemorySegment;

public class ChunkPacketBlockControllerAntiXray extends ChunkPacketBlockController {

    private final int engineMode; // 1 = Obfuscate, 2 = Fake ores

    public ChunkPacketBlockControllerAntiXray(int engineMode) {
        this.engineMode = engineMode;
    }

    @Override
    public boolean shouldObfuscate() {
        return engineMode > 0;
    }

    public void processSection(MemorySegment sectionBlocks, int sectionY, long seed) {
        if (engineMode > 0 && sectionBlocks != null) {
            NomeNativeBridge.processSectionAntiXray(sectionBlocks, sectionY, (byte) engineMode, seed);
        }
    }
}
