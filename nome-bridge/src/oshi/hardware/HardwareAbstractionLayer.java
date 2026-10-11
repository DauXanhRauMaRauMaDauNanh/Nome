package oshi.hardware;

public interface HardwareAbstractionLayer {
    CentralProcessor getProcessor();
    GlobalMemory getMemory();
}
