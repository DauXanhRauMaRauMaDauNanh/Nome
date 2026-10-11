package oshi;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class SystemInfo {
    private final HardwareAbstractionLayer hardware = new HardwareAbstractionLayer() {
        private final CentralProcessor processor = new CentralProcessor() {
            public int getLogicalProcessorCount() {
                return Runtime.getRuntime().availableProcessors();
            }
            public int getPhysicalProcessorCount() {
                return Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
            }
            public double getSystemCpuLoad() {
                OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
                return os.getSystemLoadAverage();
            }
        };

        private final GlobalMemory memory = new GlobalMemory() {
            public long getTotal() {
                return Runtime.getRuntime().totalMemory();
            }
            public long getAvailable() {
                return Runtime.getRuntime().freeMemory();
            }
        };

        @Override
        public CentralProcessor getProcessor() {
            return processor;
        }

        @Override
        public GlobalMemory getMemory() {
            return memory;
        }
    };

    public HardwareAbstractionLayer getHardware() {
        return hardware;
    }
}
