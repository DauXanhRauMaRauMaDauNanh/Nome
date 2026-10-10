package io.nome.bridge;

import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;

public class NomeUnsafeValues implements UnsafeValues {
    @Override
    public int getProtocolVersion() {
        return 769; // Protocol 769 (1.21.4) for high compatibility with standard plugins like ViaVersion
    }

    @Override
    public ItemStack modifyItemStack(ItemStack stack, String arguments) {
        return stack;
    }

    @Override
    public void checkSupported(PluginDescriptionFile pdf) {}

    @Override
    public byte[] processClass(PluginDescriptionFile pdf, String path, byte[] clazz) {
        return clazz;
    }
}
