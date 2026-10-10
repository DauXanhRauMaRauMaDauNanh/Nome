package org.bukkit.inventory;

public interface PlayerInventory extends Inventory {
    ItemStack getItemInHand();
    void setItemInHand(ItemStack stack);
    ItemStack getItemInMainHand();
    void setItemInMainHand(ItemStack stack);
    ItemStack getItemInOffHand();
    void setItemInOffHand(ItemStack stack);
    ItemStack getHelmet();
    void setHelmet(ItemStack helmet);
    ItemStack getChestplate();
    void setChestplate(ItemStack chestplate);
    ItemStack getLeggings();
    void setLeggings(ItemStack leggings);
    ItemStack getBoots();
    void setBoots(ItemStack boots);
    int getHeldItemSlot();
    void setHeldItemSlot(int slot);
}
