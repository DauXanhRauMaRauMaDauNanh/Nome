package oshi.hardware;

public interface CentralProcessor {
    int getLogicalProcessorCount();
    int getPhysicalProcessorCount();
    double getSystemCpuLoad();
}
