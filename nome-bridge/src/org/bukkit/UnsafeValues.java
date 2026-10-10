package org.bukkit;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;

public interface UnsafeValues {
    int getProtocolVersion();
    ItemStack modifyItemStack(ItemStack stack, String arguments);
    void checkSupported(PluginDescriptionFile pdf);
    byte[] processClass(PluginDescriptionFile pdf, String path, byte[] clazz);
}
