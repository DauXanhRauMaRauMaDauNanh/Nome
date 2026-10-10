package org.bukkit.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Standard implementation of PlayerInventory backed by slot array.
 */
public class SimplePlayerInventory implements PlayerInventory {

    private final Player player;
    private final ItemStack[] slots = new ItemStack[46];
    private int heldSlot = 0;

    public SimplePlayerInventory(Player player) {
        this.player = player;
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new ItemStack(Material.AIR, 0);
        }
    }

    @Override
    public int getSize() {
        return slots.length;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index >= 0 && index < slots.length) {
            return slots[index] != null ? slots[index] : new ItemStack(Material.AIR, 0);
        }
        return new ItemStack(Material.AIR, 0);
    }

    @Override
    public void setItem(int index, ItemStack item) {
        if (index >= 0 && index < slots.length) {
            slots[index] = item != null ? item : new ItemStack(Material.AIR, 0);
        }
    }

    @Override
    public ItemStack[] getContents() {
        return slots.clone();
    }

    @Override
    public void setContents(ItemStack[] items) {
        if (items != null) {
            for (int i = 0; i < Math.min(slots.length, items.length); i++) {
                slots[i] = items[i];
            }
        }
    }

    @Override
    public boolean contains(Material material) {
        for (ItemStack s : slots) {
            if (s != null && s.getType() == material && s.getAmount() > 0) return true;
        }
        return false;
    }

    @Override
    public void clear() {
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new ItemStack(Material.AIR, 0);
        }
    }

    @Override
    public ItemStack getItemInHand() {
        return getItemInMainHand();
    }

    @Override
    public void setItemInHand(ItemStack stack) {
        setItemInMainHand(stack);
    }

    @Override
    public ItemStack getItemInMainHand() {
        return getItem(36 + heldSlot);
    }

    @Override
    public void setItemInMainHand(ItemStack stack) {
        setItem(36 + heldSlot, stack);
    }

    @Override
    public ItemStack getItemInOffHand() {
        return getItem(45);
    }

    @Override
    public void setItemInOffHand(ItemStack stack) {
        setItem(45, stack);
    }

    @Override
    public ItemStack getHelmet() {
        return getItem(5);
    }

    @Override
    public void setHelmet(ItemStack helmet) {
        setItem(5, helmet);
    }

    @Override
    public ItemStack getChestplate() {
        return getItem(6);
    }

    @Override
    public void setChestplate(ItemStack chestplate) {
        setItem(6, chestplate);
    }

    @Override
    public ItemStack getLeggings() {
        return getItem(7);
    }

    @Override
    public void setLeggings(ItemStack leggings) {
        setItem(7, leggings);
    }

    @Override
    public ItemStack getBoots() {
        return getItem(8);
    }

    @Override
    public void setBoots(ItemStack boots) {
        setItem(8, boots);
    }

    @Override
    public int getHeldItemSlot() {
        return heldSlot;
    }

    @Override
    public void setHeldItemSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            this.heldSlot = slot;
        }
    }
}
