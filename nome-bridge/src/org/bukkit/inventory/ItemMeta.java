package org.bukkit.inventory;

import java.util.ArrayList;
import java.util.List;

public class ItemMeta implements Cloneable {
    private String displayName;
    private List<String> lore = new ArrayList<>();

    public boolean hasDisplayName() {
        return displayName != null && !displayName.isEmpty();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public boolean hasLore() {
        return lore != null && !lore.isEmpty();
    }

    public List<String> getLore() {
        return lore != null ? new ArrayList<>(lore) : null;
    }

    public void setLore(List<String> lore) {
        this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
    }

    @Override
    public ItemMeta clone() {
        ItemMeta copy = new ItemMeta();
        copy.displayName = this.displayName;
        copy.lore = new ArrayList<>(this.lore);
        return copy;
    }
}
