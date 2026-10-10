package org.bukkit.inventory;

import org.bukkit.Material;

public interface Inventory {
    int getSize();
    ItemStack getItem(int index);
    void setItem(int index, ItemStack item);
    ItemStack[] getContents();
    void setContents(ItemStack[] items);
    boolean contains(Material material);
    void clear();
}
