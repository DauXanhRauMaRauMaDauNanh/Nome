package net.milkbowl.vault.economy;

import org.bukkit.OfflinePlayer;
import java.util.List;

public interface Economy {

    boolean isEnabled();

    String getName();

    boolean hasBankSupport();

    int fractionalDigits();

    String format(double amount);

    String currencyNamePlural();

    String currencyNameSingular();

    boolean hasAccount(String playerName);
    boolean hasAccount(OfflinePlayer player);
    boolean hasAccount(String playerName, String worldName);
    boolean hasAccount(OfflinePlayer player, String worldName);

    double getBalance(String playerName);
    double getBalance(OfflinePlayer player);
    double getBalance(String playerName, String world);
    double getBalance(OfflinePlayer player, String world);

    boolean has(String playerName, double amount);
    boolean has(OfflinePlayer player, double amount);
    boolean has(String playerName, String worldName, double amount);
    boolean has(OfflinePlayer player, String worldName, double amount);

    EconomyResponse withdrawPlayer(String playerName, double amount);
    EconomyResponse withdrawPlayer(OfflinePlayer player, double amount);
    EconomyResponse withdrawPlayer(String playerName, String worldName, double amount);
    EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount);

    EconomyResponse depositPlayer(String playerName, double amount);
    EconomyResponse depositPlayer(OfflinePlayer player, double amount);
    EconomyResponse depositPlayer(String playerName, String worldName, double amount);
    EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount);

    boolean createPlayerAccount(String playerName);
    boolean createPlayerAccount(OfflinePlayer player);
    boolean createPlayerAccount(String playerName, String worldName);
    boolean createPlayerAccount(OfflinePlayer player, String worldName);
}
