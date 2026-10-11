package io.nome.bridge;

import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;

public class NomeUnsafeValues implements UnsafeValues {
    @Override
    public int getProtocolVersion() {
        return 777; // Protocol 777 (Minecraft 26.3 Native)
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
