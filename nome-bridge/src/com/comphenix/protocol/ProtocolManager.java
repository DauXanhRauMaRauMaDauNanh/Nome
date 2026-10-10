package com.comphenix.protocol;

import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import io.nome.bridge.NomeNativeBridge;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProtocolManager {

    private final List<PacketAdapter> listeners = new CopyOnWriteArrayList<>();

    public void addPacketListener(PacketAdapter listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removePacketListener(PacketAdapter listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public boolean hasReceivingListeners(int packetId) {
        if (listeners.isEmpty()) return false;
        for (PacketAdapter adapter : listeners) {
            if (adapter.listensTo(packetId)) return true;
        }
        return false;
    }

    public boolean hasSendingListeners(int packetId) {
        if (listeners.isEmpty()) return false;
        for (PacketAdapter adapter : listeners) {
            if (adapter.listensTo(packetId)) return true;
        }
        return false;
    }

    public void sendServerPacket(Player player, PacketContainer packet) {
        if (player == null || packet == null) return;
        NomeNativeBridge.sendRawPacket((long) player.getEntityId(), packet.getType(), packet.getData());
    }

    public void broadcastServerPacket(PacketContainer packet) {
        if (packet == null) return;
        org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> sendServerPacket(p, packet));
    }

    public boolean onPacketReceive(Player player, int packetId, byte[] payload) {
        if (listeners.isEmpty()) return true;
        PacketContainer container = new PacketContainer(packetId, payload);
        PacketEvent event = new PacketEvent(player, container);

        for (PacketAdapter adapter : listeners) {
            if (adapter.listensTo(packetId)) {
                try {
                    adapter.onPacketReceiving(event);
                    if (event.isCancelled()) return false;
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        return !event.isCancelled();
    }

    public boolean onPacketSend(Player player, int packetId, byte[] payload) {
        if (listeners.isEmpty()) return true;
        PacketContainer container = new PacketContainer(packetId, payload);
        PacketEvent event = new PacketEvent(player, container);

        for (PacketAdapter adapter : listeners) {
            if (adapter.listensTo(packetId)) {
                try {
                    adapter.onPacketSending(event);
                    if (event.isCancelled()) return false;
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        return !event.isCancelled();
    }
}
