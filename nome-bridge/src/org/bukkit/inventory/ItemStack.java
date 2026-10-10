package org.bukkit.inventory;

import org.bukkit.Material;

public class ItemStack implements Cloneable {
    private Material type;
    private int amount;
    private ItemMeta meta;

    public ItemStack(Material type) {
        this(type, 1);
    }

    public ItemStack(Material type, int amount) {
        this.type = type != null ? type : Material.AIR;
        this.amount = amount;
        this.meta = new ItemMeta();
    }

    public Material getType() {
        return type;
    }

    public void setType(Material type) {
        this.type = type != null ? type : Material.AIR;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean hasItemMeta() {
        return meta != null && (meta.hasDisplayName() || meta.hasLore());
    }

    public ItemMeta getItemMeta() {
        return meta != null ? meta.clone() : new ItemMeta();
    }

    public boolean setItemMeta(ItemMeta meta) {
        this.meta = meta != null ? meta.clone() : new ItemMeta();
        return true;
    }

    @Override
    public ItemStack clone() {
        ItemStack copy = new ItemStack(this.type, this.amount);
        if (this.meta != null) copy.meta = this.meta.clone();
        return copy;
    }

    @Override
    public String toString() {
        return "ItemStack{" + type.name() + " x " + amount + "}";
    }
}
