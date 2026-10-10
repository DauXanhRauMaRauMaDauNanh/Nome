package io.nome.bridge.vault;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in ultra-fast off-heap/memory-backed Economy Provider for Nome.
 */
public class NomeEconomy implements Economy {

    private final Map<String, Double> balances = new ConcurrentHashMap<>();

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "Nome-Native-Economy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return String.format("$%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return "Coins";
    }

    @Override
    public String currencyNameSingular() {
        return "Coin";
    }

    private String getKey(String player) {
        return player == null ? "" : player.toLowerCase();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return balances.containsKey(getKey(playerName));
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null && hasAccount(player.getName());
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        return balances.getOrDefault(getKey(playerName), 0.0);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return player != null ? getBalance(player.getName()) : 0.0;
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(playerName), EconomyResponse.ResponseType.FAILURE, "Amount cannot be negative");
        }
        String key = getKey(playerName);
        double current = balances.getOrDefault(key, 0.0);
        if (current < amount) {
            return new EconomyResponse(0, current, EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
        }
        double newBal = current - amount;
        balances.put(key, newBal);
        return new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return player != null ? withdrawPlayer(player.getName(), amount) :
                new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player null");
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(playerName), EconomyResponse.ResponseType.FAILURE, "Amount cannot be negative");
        }
        String key = getKey(playerName);
        double current = balances.getOrDefault(key, 0.0);
        double newBal = current + amount;
        balances.put(key, newBal);
        return new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return player != null ? depositPlayer(player.getName(), amount) :
                new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player null");
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        String key = getKey(playerName);
        balances.putIfAbsent(key, 0.0);
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return player != null && createPlayerAccount(player.getName());
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
}
